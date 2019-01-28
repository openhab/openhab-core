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
package org.eclipse.smarthome.config.core.i18n;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterGroup;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This OSGi service could be used to localize a config description using the I18N mechanism of the Eclipse SmartHome
 * framework.
 *
 * @author Markus Rathgeb - Move code from XML config description provider to separate service
 *
 */
@NonNullByDefault
@Component(immediate = true, service = { ConfigI18nLocalizationService.class })
public class ConfigI18nLocalizationService {

    private @NonNullByDefault({}) ConfigDescriptionI18nUtil configDescriptionParamI18nUtil;
    private @NonNullByDefault({}) ConfigDescriptionGroupI18nUtil configDescriptionGroupI18nUtil;

    @Reference
    protected void setTranslationProvider(final TranslationProvider i18nProvider) {
        this.configDescriptionParamI18nUtil = new ConfigDescriptionI18nUtil(i18nProvider);
        this.configDescriptionGroupI18nUtil = new ConfigDescriptionGroupI18nUtil(i18nProvider);
    }

    protected void unsetTranslationProvider(final TranslationProvider i18nProvider) {
        this.configDescriptionParamI18nUtil = null;
        this.configDescriptionGroupI18nUtil = null;
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
        final String parameterName = parameter.getName();

        final String label = this.configDescriptionParamI18nUtil.getParameterLabel(bundle, configDescriptionURI,
                parameterName, parameter.getLabel(), locale);

        final String description = this.configDescriptionParamI18nUtil.getParameterDescription(bundle,
                configDescriptionURI, parameterName, parameter.getDescription(), locale);

        final String pattern = this.configDescriptionParamI18nUtil.getParameterPattern(bundle, configDescriptionURI,
                parameterName, parameter.getPattern(), locale);

        final String unitLabel = this.configDescriptionParamI18nUtil.getParameterUnitLabel(bundle, configDescriptionURI,
                parameterName, parameter.getUnit(), parameter.getUnitLabel(), locale);

        final List<ParameterOption> options = getLocalizedOptions(parameter.getOptions(), bundle, configDescriptionURI,
                parameterName, locale);

        final ConfigDescriptionParameter localizedParameter = ConfigDescriptionParameterBuilder
                .create(parameterName, parameter.getType()).withMinimum(parameter.getMinimum())
                .withMaximum(parameter.getMaximum()).withStepSize(parameter.getStepSize()).withPattern(pattern)
                .withRequired(parameter.isRequired()).withReadOnly(parameter.isReadOnly())
                .withMultiple(parameter.isMultiple()).withContext(parameter.getContext())
                .withDefault(parameter.getDefault()).withLabel(label).withDescription(description).withOptions(options)
                .withFilterCriteria(parameter.getFilterCriteria()).withGroupName(parameter.getGroupName())
                .withAdvanced(parameter.isAdvanced()).withVerify(parameter.isVerifyable())
                .withLimitToOptions(parameter.getLimitToOptions()).withMultipleLimit(parameter.getMultipleLimit())
                .withUnit(parameter.getUnit()).withUnitLabel(unitLabel).build();

        return localizedParameter;
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
        final String name = group.getName();

        final String label = this.configDescriptionGroupI18nUtil.getGroupLabel(bundle, configDescriptionURI, name,
                group.getLabel(), locale);

        final String description = this.configDescriptionGroupI18nUtil.getGroupDescription(bundle, configDescriptionURI,
                name, group.getDescription(), locale);

        final ConfigDescriptionParameterGroup localizedGroup = new ConfigDescriptionParameterGroup(name,
                group.getContext(), group.isAdvanced(), label, description);

        return localizedGroup;
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

            final String localizedLabel = this.configDescriptionParamI18nUtil.getParameterOptionLabel(bundle,
                    configDescriptionURI, parameterName, /* key */option.getValue(), /* fallback */option.getLabel(),
                    locale);
            final ParameterOption localizedOption = new ParameterOption(option.getValue(), localizedLabel);
            localizedOptions.add(localizedOption);
        }
        return localizedOptions;
    }
}
