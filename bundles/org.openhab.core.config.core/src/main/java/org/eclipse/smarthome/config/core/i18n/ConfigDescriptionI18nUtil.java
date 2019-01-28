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
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.smarthome.core.i18n.I18nUtil;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
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
 */
public class ConfigDescriptionI18nUtil {

    private final TranslationProvider i18nProvider;

    private static final Pattern DELIMITER = Pattern.compile("[:=\\s]");

    public ConfigDescriptionI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public String getParameterPattern(Bundle bundle, URI configDescriptionURI, String parameterName,
            String defaultPattern, Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultPattern,
                () -> inferKey(configDescriptionURI, parameterName, "pattern"));
        return i18nProvider.getText(bundle, key, defaultPattern, locale);
    }

    public String getParameterDescription(Bundle bundle, URI configDescriptionURI, String parameterName,
            String defaultDescription, Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription,
                () -> inferKey(configDescriptionURI, parameterName, "description"));
        return i18nProvider.getText(bundle, key, defaultDescription, locale);
    }

    public String getParameterLabel(Bundle bundle, URI configDescriptionURI, String parameterName, String defaultLabel,
            Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel,
                () -> inferKey(configDescriptionURI, parameterName, "label"));
        return i18nProvider.getText(bundle, key, defaultLabel, locale);
    }

    public String getParameterOptionLabel(Bundle bundle, URI configDescriptionURI, String parameterName,
            String optionValue, String defaultOptionLabel, Locale locale) {
        if (!isValidPropertyKey(optionValue)) {
            return defaultOptionLabel;
        }

        String key = I18nUtil.stripConstantOr(defaultOptionLabel,
                () -> inferKey(configDescriptionURI, parameterName, "option." + optionValue));

        return i18nProvider.getText(bundle, key, defaultOptionLabel, locale);
    }

    public String getParameterUnitLabel(Bundle bundle, URI configDescriptionURI, String parameterName, String unit,
            String defaultUnitLabel, Locale locale) {
        if (unit != null && defaultUnitLabel == null) {
            String label = i18nProvider.getText(FrameworkUtil.getBundle(this.getClass()), "unit." + unit, null, locale);
            if (label != null) {
                return label;
            }
        }
        String key = I18nUtil.stripConstantOr(defaultUnitLabel,
                () -> inferKey(configDescriptionURI, parameterName, "unitLabel"));
        return i18nProvider.getText(bundle, key, defaultUnitLabel, locale);
    }

    private String inferKey(URI configDescriptionURI, String parameterName, String lastSegment) {
        String uri = configDescriptionURI.getSchemeSpecificPart().replace(":", ".");
        return configDescriptionURI.getScheme() + ".config." + uri + "." + parameterName + "." + lastSegment;
    }

    private boolean isValidPropertyKey(String key) {
        if (key != null) {
            return !DELIMITER.matcher(key).find();
        }
        return false;
    }

}
