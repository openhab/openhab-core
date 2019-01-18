/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.config.core;

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
 * @author Dennis Nobel - Initial contribution, added locale support
 * @author Michael Grammling - Initial contribution
 * @author Chris Jackson - Added compatibility with multiple ConfigDescriptionProviders. Added Config OptionProvider.
 * @author Thomas Höfer - Added unit
 */
@Component(immediate = true, service = { ConfigDescriptionRegistry.class })
public class ConfigDescriptionRegistry {

    private final Logger logger = LoggerFactory.getLogger(ConfigDescriptionRegistry.class);

    private final List<ConfigOptionProvider> configOptionProviders = new CopyOnWriteArrayList<>();
    private final List<ConfigDescriptionProvider> configDescriptionProviders = new CopyOnWriteArrayList<>();
    private final List<ConfigDescriptionAliasProvider> configDescriptionAliasProviders = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addConfigOptionProvider(ConfigOptionProvider configOptionProvider) {
        if (configOptionProvider != null) {
            configOptionProviders.add(configOptionProvider);
        }
    }

    protected void removeConfigOptionProvider(ConfigOptionProvider configOptionProvider) {
        if (configOptionProvider != null) {
            configOptionProviders.remove(configOptionProvider);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addConfigDescriptionProvider(ConfigDescriptionProvider configDescriptionProvider) {
        if (configDescriptionProvider != null) {
            configDescriptionProviders.add(configDescriptionProvider);
        }
    }

    protected void removeConfigDescriptionProvider(ConfigDescriptionProvider configDescriptionProvider) {
        if (configDescriptionProvider != null) {
            configDescriptionProviders.remove(configDescriptionProvider);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addConfigDescriptionAliasProvider(ConfigDescriptionAliasProvider configDescriptionAliasProvider) {
        if (configDescriptionAliasProvider != null) {
            configDescriptionAliasProviders.add(configDescriptionAliasProvider);
        }
    }

    protected void removeConfigDescriptionAliasProvider(ConfigDescriptionAliasProvider configDescriptionAliasProvider) {
        if (configDescriptionAliasProvider != null) {
            configDescriptionAliasProviders.remove(configDescriptionAliasProvider);
        }
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
    public Collection<ConfigDescription> getConfigDescriptions(Locale locale) {
        Map<URI, ConfigDescription> configMap = new HashMap<URI, ConfigDescription>();

        // Loop over all providers
        for (ConfigDescriptionProvider configDescriptionProvider : this.configDescriptionProviders) {
            // And for each provider, loop over all their config descriptions
            for (ConfigDescription configDescription : configDescriptionProvider.getConfigDescriptions(locale)) {
                // See if there already exists a configuration for this URI in the map
                ConfigDescription configFromMap = configMap.get(configDescription.getUID());
                if (configFromMap != null) {
                    // Yes - Merge the groups and parameters
                    List<ConfigDescriptionParameter> parameters = new ArrayList<ConfigDescriptionParameter>();
                    parameters.addAll(configFromMap.getParameters());
                    parameters.addAll(configDescription.getParameters());

                    List<ConfigDescriptionParameterGroup> parameterGroups = new ArrayList<ConfigDescriptionParameterGroup>();
                    parameterGroups.addAll(configFromMap.getParameterGroups());
                    parameterGroups.addAll(configDescription.getParameterGroups());

                    // And add the combined configuration to the map
                    configMap.put(configDescription.getUID(),
                            new ConfigDescription(configDescription.getUID(), parameters, parameterGroups));
                } else {
                    // No - Just add the new configuration to the map
                    configMap.put(configDescription.getUID(), configDescription);
                }
            }
        }

        // Now convert the map into the collection
        Collection<ConfigDescription> configDescriptions = new ArrayList<ConfigDescription>(configMap.size());
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
    public @Nullable ConfigDescription getConfigDescription(URI uri, Locale locale) {
        List<ConfigDescriptionParameter> parameters = new ArrayList<ConfigDescriptionParameter>();
        List<ConfigDescriptionParameterGroup> parameterGroups = new ArrayList<ConfigDescriptionParameterGroup>();

        boolean found = false;
        Set<URI> aliases = getAliases(uri);
        for (URI alias : aliases) {
            logger.debug("No config description found for '{}', using alias '{}' instead", uri, alias);
            found |= fillFromProviders(alias, locale, parameters, parameterGroups);
        }

        found |= fillFromProviders(uri, locale, parameters, parameterGroups);

        if (found) {
            List<ConfigDescriptionParameter> parametersWithOptions = new ArrayList<ConfigDescriptionParameter>(
                    parameters.size());
            for (ConfigDescriptionParameter parameter : parameters) {
                parametersWithOptions.add(getConfigOptions(uri, aliases, parameter, locale));
            }

            // Return the new configuration description
            return new ConfigDescription(uri, parametersWithOptions, parameterGroups);
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

    private boolean fillFromProviders(URI uri, Locale locale, List<ConfigDescriptionParameter> parameters,
            List<ConfigDescriptionParameterGroup> parameterGroups) {
        boolean found = false;
        for (ConfigDescriptionProvider configDescriptionProvider : this.configDescriptionProviders) {
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
     * @param uri the URI to which the options to be returned (must not be null)
     * @param parameter the parameter requiring options to be updated
     * @param locale locale
     * @return config description
     */
    private ConfigDescriptionParameter getConfigOptions(URI uri, Set<URI> aliases, ConfigDescriptionParameter parameter,
            Locale locale) {
        List<ParameterOption> options = new ArrayList<ParameterOption>();

        // Add all the existing options that may be provided by the initial config description provider
        options.addAll(parameter.getOptions());

        boolean found = fillFromProviders(uri, parameter, locale, options);

        if (!found && aliases != null) {
            for (URI alias : aliases) {
                found = fillFromProviders(alias, parameter, locale, options);
                if (found) {
                    break;
                }
            }
        }

        if (found) {
            // Return the new parameter
            return new ConfigDescriptionParameter(parameter.getName(), parameter.getType(), parameter.getMinimum(),
                    parameter.getMaximum(), parameter.getStepSize(), parameter.getPattern(), parameter.isRequired(),
                    parameter.isReadOnly(), parameter.isMultiple(), parameter.getContext(), parameter.getDefault(),
                    parameter.getLabel(), parameter.getDescription(), options, parameter.getFilterCriteria(),
                    parameter.getGroupName(), parameter.isAdvanced(), parameter.getLimitToOptions(),
                    parameter.getMultipleLimit(), parameter.getUnit(), parameter.getUnitLabel(),
                    parameter.isVerifyable());
        } else {
            // Otherwise return the original parameter
            return parameter;
        }
    }

    private boolean fillFromProviders(URI alias, ConfigDescriptionParameter parameter, Locale locale,
            List<ParameterOption> options) {
        boolean found = false;
        for (ConfigOptionProvider configOptionProvider : this.configOptionProviders) {
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
