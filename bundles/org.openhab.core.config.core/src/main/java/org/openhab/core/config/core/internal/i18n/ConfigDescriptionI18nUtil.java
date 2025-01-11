/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.config.core.internal.i18n;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The {@link ConfigDescriptionI18nUtil} uses the {@link TranslationProvider} to
 * resolve the localized texts. It automatically infers the key if the default
 * text is not a constant.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Alex Tugarev - Extended for pattern and option label
 * @author Thomas Höfer - Extended for unit label
 * @author Laurent Garnier - Changed inferred key for add-ons + alternative key
 */
@NonNullByDefault
public class ConfigDescriptionI18nUtil {

    private final TranslationProvider i18nProvider;

    private static final Pattern DELIMITER = Pattern.compile("[:=\\s]");
    private static final Set<String> ADDON_TYPES = Set.of("automation", "binding", "io", "misc", "persistence", "voice",
            "ui");

    public ConfigDescriptionI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public @Nullable String getParameterPattern(Bundle bundle, URI configDescriptionURI, String parameterName,
            @Nullable String defaultPattern, @Nullable Locale locale) {
        return getParameterValue(bundle, configDescriptionURI, parameterName, "pattern", defaultPattern, locale);
    }

    public @Nullable String getParameterDescription(Bundle bundle, URI configDescriptionURI, String parameterName,
            @Nullable String defaultDescription, @Nullable Locale locale) {
        return getParameterValue(bundle, configDescriptionURI, parameterName, "description", defaultDescription,
                locale);
    }

    public @Nullable String getParameterLabel(Bundle bundle, URI configDescriptionURI, String parameterName,
            @Nullable String defaultLabel, @Nullable Locale locale) {
        return getParameterValue(bundle, configDescriptionURI, parameterName, "label", defaultLabel, locale);
    }

    public @Nullable String getParameterOptionLabel(Bundle bundle, URI configDescriptionURI, String parameterName,
            @Nullable String optionValue, @Nullable String defaultOptionLabel, @Nullable Locale locale) {
        if (!isValidPropertyKey(optionValue)) {
            return defaultOptionLabel;
        }

        return getParameterValue(bundle, configDescriptionURI, parameterName, "option." + optionValue,
                defaultOptionLabel, locale);
    }

    public @Nullable String getParameterUnitLabel(Bundle bundle, URI configDescriptionURI, String parameterName,
            @Nullable String unit, @Nullable String defaultUnitLabel, @Nullable Locale locale) {
        if (unit != null && defaultUnitLabel == null) {
            String label = i18nProvider.getText(FrameworkUtil.getBundle(this.getClass()), "unit." + unit, null, locale);
            if (label != null) {
                return label;
            }
        }
        return getParameterValue(bundle, configDescriptionURI, parameterName, "unitLabel", defaultUnitLabel, locale);
    }

    private @Nullable String getParameterValue(Bundle bundle, URI configDescriptionURI, String parameterName,
            String parameterAttribute, @Nullable String defaultValue, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultValue,
                () -> inferKey(true, configDescriptionURI, parameterName, parameterAttribute));
        String value = i18nProvider.getText(bundle, key, null, locale);
        if (value == null && ADDON_TYPES.contains(configDescriptionURI.getScheme())) {
            key = I18nUtil.stripConstantOr(defaultValue,
                    () -> inferKey(false, configDescriptionURI, parameterName, parameterAttribute));
            value = i18nProvider.getText(bundle, key, null, locale);
        }
        return value != null ? value : defaultValue;
    }

    private String inferKey(boolean checkAddonType, URI configDescriptionURI, String parameterName,
            String lastSegment) {
        String prefix = checkAddonType && ADDON_TYPES.contains(configDescriptionURI.getScheme()) ? "addon"
                : configDescriptionURI.getScheme();
        String uri = configDescriptionURI.getSchemeSpecificPart().replace(":", ".");
        return prefix + ".config." + uri + "." + parameterName + "." + lastSegment;
    }

    private boolean isValidPropertyKey(@Nullable String key) {
        return (key != null) && !DELIMITER.matcher(key).find();
    }
}
