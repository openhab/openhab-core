/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.config.core.i18n;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.config.core.internal.i18n.ConfigDescriptionGroupI18nUtil;
import org.openhab.core.config.core.internal.i18n.ConfigDescriptionI18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize a config description using the I18N mechanism of the openHAB
 * framework.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@Component(immediate = true, service = { ConfigI18nLocalizationService.class })
@NonNullByDefault
public class ConfigI18nLocalizationService {

    private final ConfigDescriptionI18nUtil configDescriptionI18nUtil;
    private final ConfigDescriptionGroupI18nUtil configDescriptionGroupI18nUtil;

    @Activate
    public ConfigI18nLocalizationService(final @Reference TranslationProvider i18nProvider) {
        this.configDescriptionI18nUtil = new ConfigDescriptionI18nUtil(i18nProvider);
        this.configDescriptionGroupI18nUtil = new ConfigDescriptionGroupI18nUtil(i18nProvider);
    }

    /**
     * Localize a config description.
     *
     * @param bundle the bundle the i18n resources are located
     * @param configDescription the config description that should be localized
     * @param locale the locale it should be localized to
     * @return a localized configuration description on success, a non-localized one on error (e.g. no translation is
     *         found).
     */
    public ConfigDescription getLocalizedConfigDescription(final Bundle bundle,
            final ConfigDescription configDescription, final @Nullable Locale locale) {
        final List<ConfigDescriptionParameter> localizedConfigDescriptionParameters = new ArrayList<>(
                configDescription.getParameters().size());

        // Loop through all the configuration parameters and localize them
        for (final ConfigDescriptionParameter configDescriptionParameter : configDescription.getParameters()) {
            final ConfigDescriptionParameter localizedConfigDescriptionParameter = getLocalizedConfigDescriptionParameter(
                    bundle, configDescription, configDescriptionParameter, locale);
            localizedConfigDescriptionParameters.add(localizedConfigDescriptionParameter);
        }

        final List<ConfigDescriptionParameterGroup> localizedConfigDescriptionGroups = new ArrayList<>(
                configDescription.getParameterGroups().size());

        // Loop through all the configuration groups and localize them
        for (final ConfigDescriptionParameterGroup configDescriptionParameterGroup : configDescription
                .getParameterGroups()) {
            final ConfigDescriptionParameterGroup localizedConfigDescriptionGroup = getLocalizedConfigDescriptionGroup(
                    bundle, configDescription, configDescriptionParameterGroup, locale);
            localizedConfigDescriptionGroups.add(localizedConfigDescriptionGroup);
        }
        return new ConfigDescription(configDescription.getUID(), localizedConfigDescriptionParameters,
                localizedConfigDescriptionGroups);
    }

    /**
     * Localize a config description parameter.
     *
     * @param bundle the bundle the i18n resources are located
     * @param configDescription the config description the parameter is part of
     * @param parameter the parameter that should be localized
     * @param locale the locale it should be localized to
     * @return a localized parameter on success, a non-localized one on error (e.g. no translation is found).
     */
    public ConfigDescriptionParameter getLocalizedConfigDescriptionParameter(final Bundle bundle,
            final ConfigDescription configDescription, final ConfigDescriptionParameter parameter,
            final @Nullable Locale locale) {
        final URI configDescriptionURI = configDescription.getUID();
        return getLocalizedConfigDescriptionParameter(bundle, configDescriptionURI, parameter, locale);
    }

    /**
     * Localize a config description parameter.
     *
     * @param bundle the bundle the i18n resources are located
     * @param configDescriptionURI the config description URI
     * @param parameter the parameter that should be localized
     * @param locale the locale it should be localized to
     * @return a localized parameter on success, a non-localized one on error (e.g. no translation is found).
     */
    public ConfigDescriptionParameter getLocalizedConfigDescriptionParameter(final Bundle bundle,
            final URI configDescriptionURI, final ConfigDescriptionParameter parameter, final @Nullable Locale locale) {
        final String parameterName = parameter.getName();

        final String label = configDescriptionI18nUtil.getParameterLabel(bundle, configDescriptionURI, parameterName,
                parameter.getLabel(), locale);

        final String description = configDescriptionI18nUtil.getParameterDescription(bundle, configDescriptionURI,
                parameterName, parameter.getDescription(), locale);

        final String pattern = configDescriptionI18nUtil.getParameterPattern(bundle, configDescriptionURI,
                parameterName, parameter.getPattern(), locale);

        final String unitLabel = configDescriptionI18nUtil.getParameterUnitLabel(bundle, configDescriptionURI,
                parameterName, parameter.getUnit(), parameter.getUnitLabel(), locale);

        final List<ParameterOption> options = getLocalizedOptions(parameter.getOptions(), bundle, configDescriptionURI,
                parameterName, locale);

        return ConfigDescriptionParameterBuilder.create(parameterName, parameter.getType())
                .withMinimum(parameter.getMinimum()).withMaximum(parameter.getMaximum())
                .withStepSize(parameter.getStepSize()).withPattern(pattern == null ? parameter.getPattern() : pattern)
                .withRequired(parameter.isRequired()).withReadOnly(parameter.isReadOnly())
                .withMultiple(parameter.isMultiple()).withContext(parameter.getContext())
                .withDefault(parameter.getDefault()).withLabel(label == null ? parameter.getLabel() : label)
                .withDescription(description == null ? parameter.getDescription() : description)
                .withOptions(options == null || options.isEmpty() ? parameter.getOptions() : options)
                .withFilterCriteria(parameter.getFilterCriteria()).withGroupName(parameter.getGroupName())
                .withAdvanced(parameter.isAdvanced()).withVerify(parameter.isVerifyable())
                .withLimitToOptions(parameter.getLimitToOptions()).withMultipleLimit(parameter.getMultipleLimit())
                .withUnit(parameter.getUnit()).withUnitLabel(unitLabel == null ? parameter.getUnitLabel() : unitLabel)
                .build();
    }

    /**
     * Localize a config description parameter group.
     *
     * @param bundle the bundle the i18n resources are located
     * @param configDescription the config description the parameter group is part of
     * @param group the parameter group that should be localized
     * @param locale the locale it should be localized to
     * @return a localized parameter group on success, a non-localized one on error (e.g. no translation is found).
     */
    public ConfigDescriptionParameterGroup getLocalizedConfigDescriptionGroup(final Bundle bundle,
            final ConfigDescription configDescription, final ConfigDescriptionParameterGroup group,
            final @Nullable Locale locale) {
        final URI configDescriptionURI = configDescription.getUID();
        return getLocalizedConfigDescriptionGroup(bundle, configDescriptionURI, group, locale);
    }

    /**
     * Localize a config description parameter group.
     *
     * @param bundle the bundle the i18n resources are located
     * @param configDescriptionURI the config description URI
     * @param group the parameter group that should be localized
     * @param locale the locale it should be localized to
     * @return a localized parameter group on success, a non-localized one on error (e.g. no translation is found).
     */
    public ConfigDescriptionParameterGroup getLocalizedConfigDescriptionGroup(final Bundle bundle,
            final URI configDescriptionURI, final ConfigDescriptionParameterGroup group,
            final @Nullable Locale locale) {
        final String name = group.getName();

        final String label = configDescriptionGroupI18nUtil.getGroupLabel(bundle, configDescriptionURI, name,
                group.getLabel(), locale);

        final String description = configDescriptionGroupI18nUtil.getGroupDescription(bundle, configDescriptionURI,
                name, group.getDescription(), locale);

        return new ConfigDescriptionParameterGroup(name, group.getContext(), group.isAdvanced(),
                label == null ? group.getLabel() : label, description == null ? group.getDescription() : description);
    }

    /**
     * Localize parameter options.
     *
     * @param originalOptions the parameter options that should be localized
     * @param bundle the bundle the i18n resources are located
     * @param configDescriptionURI the config description URI
     * @param parameterName the name of the parameter
     * @param locale the locale it should be localized to
     * @return a list with parameter option. If an option could not be localized (e.g. no translation is found), the
     *         non-localized one is added to the list.
     */
    public List<ParameterOption> getLocalizedOptions(final List<ParameterOption> originalOptions, final Bundle bundle,
            final URI configDescriptionURI, final String parameterName, final @Nullable Locale locale) {
        if (originalOptions == null || originalOptions.isEmpty()) {
            return originalOptions;
        }

        final List<ParameterOption> localizedOptions = new ArrayList<>();
        for (final ParameterOption option : originalOptions) {
            final String label = configDescriptionI18nUtil.getParameterOptionLabel(bundle, configDescriptionURI,
                    parameterName, option.getValue(), option.getLabel(), locale);

            localizedOptions.add(new ParameterOption(option.getValue(), label == null ? option.getLabel() : label));
        }
        return localizedOptions;
    }
}
