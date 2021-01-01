/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.config.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ConfigDescriptionRegistry} provides access to {@link ConfigDescription}s.
 * It tracks {@link ConfigDescriptionProvider} OSGi services to collect all {@link ConfigDescription}s.
 *
 * @see ConfigDescriptionProvider
 *
 * @author Dennis Nobel - Initial contribution
 * @author Michael Grammling - Initial contribution
 * @author Chris Jackson - Added compatibility with multiple ConfigDescriptionProviders. Added Config OptionProvider.
 * @author Thomas Höfer - Added unit
 */
@Component(immediate = true, service = { ConfigDescriptionRegistry.class })
@NonNullByDefault
public class ConfigDescriptionRegistry {

    private final Logger logger = LoggerFactory.getLogger(ConfigDescriptionRegistry.class);

    private final List<ConfigOptionProvider> configOptionProviders = new CopyOnWriteArrayList<>();
    private final List<ConfigDescriptionProvider> configDescriptionProviders = new CopyOnWriteArrayList<>();
    private final List<ConfigDescriptionAliasProvider> configDescriptionAliasProviders = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addConfigOptionProvider(ConfigOptionProvider configOptionProvider) {
        configOptionProviders.add(configOptionProvider);
    }

    protected void removeConfigOptionProvider(ConfigOptionProvider configOptionProvider) {
        configOptionProviders.remove(configOptionProvider);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addConfigDescriptionProvider(ConfigDescriptionProvider configDescriptionProvider) {
        configDescriptionProviders.add(configDescriptionProvider);
    }

    protected void removeConfigDescriptionProvider(ConfigDescriptionProvider configDescriptionProvider) {
        configDescriptionProviders.remove(configDescriptionProvider);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addConfigDescriptionAliasProvider(ConfigDescriptionAliasProvider configDescriptionAliasProvider) {
        configDescriptionAliasProviders.add(configDescriptionAliasProvider);
    }

    protected void removeConfigDescriptionAliasProvider(ConfigDescriptionAliasProvider configDescriptionAliasProvider) {
        configDescriptionAliasProviders.remove(configDescriptionAliasProvider);
    }

    /**
     * Returns all config descriptions.
     * <p>
     * If more than one {@link ConfigDescriptionProvider} is registered for a specific URI, then the returned
     * {@link ConfigDescription} collection will contain the data from all providers.
     * <p>
     * No checking is performed to ensure that multiple providers don't provide the same configuration data. It is up to
     * the binding to ensure that multiple sources (eg static XML and dynamic binding data) do not contain overlapping
     * information.
     *
     * @param locale locale
     * @return all config descriptions or an empty collection if no config
     *         description exists
     */
    public Collection<ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
        Map<URI, ConfigDescription> configMap = new HashMap<>();

        // Loop over all providers
        for (ConfigDescriptionProvider configDescriptionProvider : configDescriptionProviders) {
            // And for each provider, loop over all their config descriptions
            for (ConfigDescription configDescription : configDescriptionProvider.getConfigDescriptions(locale)) {
                // See if there already exists a configuration for this URI in the map
                ConfigDescription configFromMap = configMap.get(configDescription.getUID());
                if (configFromMap != null) {
                    // Yes - Merge the groups and parameters
                    List<ConfigDescriptionParameter> parameters = new ArrayList<>();
                    parameters.addAll(configFromMap.getParameters());
                    parameters.addAll(configDescription.getParameters());

                    List<ConfigDescriptionParameterGroup> parameterGroups = new ArrayList<>();
                    parameterGroups.addAll(configFromMap.getParameterGroups());
                    parameterGroups.addAll(configDescription.getParameterGroups());

                    // And add the combined configuration to the map
                    configMap.put(configDescription.getUID(),
                            ConfigDescriptionBuilder.create(configDescription.getUID()).withParameters(parameters)
                                    .withParameterGroups(parameterGroups).build());
                } else {
                    // No - Just add the new configuration to the map
                    configMap.put(configDescription.getUID(), configDescription);
                }
            }
        }

        // Now convert the map into the collection
        Collection<ConfigDescription> configDescriptions = new ArrayList<>(configMap.size());
        for (ConfigDescription configDescription : configMap.values()) {
            configDescriptions.add(configDescription);
        }

        return Collections.unmodifiableCollection(configDescriptions);
    }

    /**
     * Returns all config descriptions.
     *
     * @return all config descriptions or an empty collection if no config
     *         description exists
     */
    public Collection<ConfigDescription> getConfigDescriptions() {
        return getConfigDescriptions(null);
    }

    /**
     * Returns a config description for a given URI.
     * <p>
     * If more than one {@link ConfigDescriptionProvider} is registered for the requested URI, then the returned
     * {@link ConfigDescription} will contain the data from all providers.
     * <p>
     * No checking is performed to ensure that multiple providers don't provide the same configuration data. It is up to
     * the binding to ensure that multiple sources (eg static XML and dynamic binding data) do not contain overlapping
     * information.
     *
     * @param uri the URI to which the config description to be returned (must
     *            not be null)
     * @param locale locale
     * @return config description or null if no config description exists for
     *         the given name
     */
    public @Nullable ConfigDescription getConfigDescription(URI uri, @Nullable Locale locale) {
        List<ConfigDescriptionParameter> parameters = new ArrayList<>();
        List<ConfigDescriptionParameterGroup> parameterGroups = new ArrayList<>();

        boolean found = false;
        Set<URI> aliases = getAliases(uri);
        for (URI alias : aliases) {
            logger.debug("No config description found for '{}', using alias '{}' instead", uri, alias);
            found |= fillFromProviders(alias, locale, parameters, parameterGroups);
        }

        found |= fillFromProviders(uri, locale, parameters, parameterGroups);

        if (found) {
            List<ConfigDescriptionParameter> parametersWithOptions = new ArrayList<>(parameters.size());
            for (ConfigDescriptionParameter parameter : parameters) {
                parametersWithOptions.add(getConfigOptions(uri, aliases, parameter, locale));
            }

            // Return the new configuration description
            return ConfigDescriptionBuilder.create(uri).withParameters(parametersWithOptions)
                    .withParameterGroups(parameterGroups).build();
        } else {
            // Otherwise null
            return null;
        }
    }

    private Set<URI> getAliases(URI original) {
        Set<URI> ret = new LinkedHashSet<>();
        for (ConfigDescriptionAliasProvider aliasProvider : configDescriptionAliasProviders) {
            URI alias = aliasProvider.getAlias(original);
            if (alias != null) {
                ret.add(alias);
            }
        }
        return ret;
    }

    private boolean fillFromProviders(URI uri, @Nullable Locale locale, List<ConfigDescriptionParameter> parameters,
            List<ConfigDescriptionParameterGroup> parameterGroups) {
        boolean found = false;
        for (ConfigDescriptionProvider configDescriptionProvider : configDescriptionProviders) {
            ConfigDescription config = configDescriptionProvider.getConfigDescription(uri, locale);

            if (config != null) {
                found = true;

                // Simply merge the groups and parameters
                parameters.addAll(config.getParameters());
                parameterGroups.addAll(config.getParameterGroups());
            }
        }
        return found;
    }

    /**
     * Returns a config description for a given URI.
     *
     * @param uri the URI to which the config description to be returned (must
     *            not be null)
     * @return config description or null if no config description exists for
     *         the given name
     */
    public @Nullable ConfigDescription getConfigDescription(URI uri) {
        return getConfigDescription(uri, null);
    }

    /**
     * Updates the config parameter options for a given URI and parameter
     * <p>
     * If more than one {@link ConfigOptionProvider} is registered for the requested URI, then the returned
     * {@link ConfigDescriptionParameter} will contain the data from all providers.
     * <p>
     * No checking is performed to ensure that multiple providers don't provide the same options. It is up to
     * the binding to ensure that multiple sources (eg static XML and dynamic binding data) do not contain overlapping
     * information.
     *
     * @param uri the URI to which the options to be returned
     * @param parameter the parameter requiring options to be updated
     * @param locale locale
     * @return config description
     */
    private ConfigDescriptionParameter getConfigOptions(URI uri, Set<URI> aliases, ConfigDescriptionParameter parameter,
            @Nullable Locale locale) {
        List<ParameterOption> options = new ArrayList<>();

        // Add all the existing options that may be provided by the initial config description provider
        options.addAll(parameter.getOptions());

        boolean found = fillFromProviders(uri, parameter, locale, options);

        if (!found) {
            for (URI alias : aliases) {
                found = fillFromProviders(alias, parameter, locale, options);
                if (found) {
                    break;
                }
            }
        }

        if (found) {
            // Return the new parameter
            return ConfigDescriptionParameterBuilder.create(parameter.getName(), parameter.getType()) //
                    .withMinimum(parameter.getMinimum()) //
                    .withMaximum(parameter.getMaximum()) //
                    .withStepSize(parameter.getStepSize()) //
                    .withPattern(parameter.getPattern()) //
                    .withRequired(parameter.isRequired()) //
                    .withReadOnly(parameter.isReadOnly()) //
                    .withMultiple(parameter.isMultiple()) //
                    .withContext(parameter.getContext()) //
                    .withDefault(parameter.getDefault()) //
                    .withLabel(parameter.getLabel()) //
                    .withDescription(parameter.getDescription()) //
                    .withOptions(options) //
                    .withFilterCriteria(parameter.getFilterCriteria()) //
                    .withGroupName(parameter.getGroupName()) //
                    .withAdvanced(parameter.isAdvanced()) //
                    .withLimitToOptions(parameter.getLimitToOptions()) //
                    .withMultipleLimit(parameter.getMultipleLimit()) //
                    .withUnit(parameter.getUnit()) //
                    .withUnitLabel(parameter.getUnitLabel()) //
                    .withVerify(parameter.isVerifyable()) //
                    .build();
        } else {
            // Otherwise return the original parameter
            return parameter;
        }
    }

    private boolean fillFromProviders(URI alias, ConfigDescriptionParameter parameter, @Nullable Locale locale,
            List<ParameterOption> options) {
        boolean found = false;
        for (ConfigOptionProvider configOptionProvider : configOptionProviders) {
            Collection<ParameterOption> newOptions = configOptionProvider.getParameterOptions(alias,
                    parameter.getName(), parameter.getContext(), locale);

            if (newOptions != null) {
                found = true;

                // Simply merge the options
                options.addAll(newOptions);
            }
        }
        return found;
    }
}
