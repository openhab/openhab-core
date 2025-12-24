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
package org.openhab.core.addon.internal;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;

/**
 * The {@link AddonI18nUtil} uses the {@link TranslationProvider} to resolve the
 * localized texts. It automatically infers the key if the default text is not a
 * constant.
 *
 * @author Dennis Nobel - Initial contribution
 */
@NonNullByDefault
public class AddonI18nUtil {

    private final TranslationProvider i18nProvider;

    /**
     * Creates a new instance with the given translation provider.
     *
     * @param i18nProvider the translation provider to use for localization
     */
    public AddonI18nUtil(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    /**
     * Gets the localized description for an add-on.
     *
     * @param bundle the bundle containing the translations
     * @param addonId the identifier of the add-on
     * @param defaultDescription the default description text
     * @param locale the locale to use for translation, or null to use the default locale
     * @return the localized description, or the default description if no translation is available
     */
    public String getDescription(Bundle bundle, String addonId, String defaultDescription, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultDescription, () -> inferKey(addonId, "description"));
        String localizedText = i18nProvider.getText(bundle, key, defaultDescription, locale);
        return localizedText != null ? localizedText : defaultDescription;
    }

    /**
     * Gets the localized name for an add-on.
     *
     * @param bundle the bundle containing the translations
     * @param addonId the identifier of the add-on
     * @param defaultLabel the default label text
     * @param locale the locale to use for translation, or null to use the default locale
     * @return the localized name, or the default label if no translation is available
     */
    public String getName(Bundle bundle, String addonId, String defaultLabel, @Nullable Locale locale) {
        String key = I18nUtil.stripConstantOr(defaultLabel, () -> inferKey(addonId, "name"));
        String localizedText = i18nProvider.getText(bundle, key, defaultLabel, locale);
        return localizedText != null ? localizedText : defaultLabel;
    }

    private String inferKey(String addonId, String lastSegment) {
        return "addon." + addonId + "." + lastSegment;
    }
}
