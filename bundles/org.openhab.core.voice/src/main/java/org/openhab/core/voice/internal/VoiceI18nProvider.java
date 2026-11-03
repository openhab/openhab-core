/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.voice.internal;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Service providing localized strings for the voice bundle without requiring consumers to reference OSGi
 * {@link Bundle}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = VoiceI18nProvider.class)
@NonNullByDefault
public class VoiceI18nProvider {

    private final TranslationProvider i18nProvider;
    private final @Nullable Bundle bundle;

    @Activate
    public VoiceI18nProvider(final @Reference TranslationProvider i18nProvider, BundleContext bundleContext) {
        this.i18nProvider = i18nProvider;
        this.bundle = bundleContext.getBundle();
    }

    /**
     * Resolves a localized text string for the given key and locale.
     *
     * @param key translation key
     * @param locale target locale
     * @return localized text or null if not found
     */
    public @Nullable String getText(String key, Locale locale) {
        return getText(key, null, locale);
    }

    /**
     * Resolves a localized text string for the given key and locale with a default fallback.
     *
     * @param key translation key
     * @param defaultText fallback text if key is not found
     * @param locale target locale
     * @return localized text or defaultText if not found
     */
    public @Nullable String getText(String key, @Nullable String defaultText, Locale locale) {
        Bundle b = bundle;
        return b != null ? i18nProvider.getText(b, key, defaultText, locale) : defaultText;
    }

    /**
     * Resolves a localized text string and formats it by replacing positional placeholders {0}, {1}, etc.
     * If the translation key is not found and the first argument is provided, the argument string is returned as
     * fallback.
     *
     * @param key translation key
     * @param locale target locale
     * @param args arguments to replace {0}, {1}, etc.
     * @return formatted localized text or fallback
     */
    public @Nullable String getFormattedText(String key, Locale locale, @Nullable Object... args) {
        String template = getText(key, locale);
        if (template != null) {
            String result = template;
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                result = result.replace("{" + i + "}", arg != null ? String.valueOf(arg) : "");
            }
            return result;
        }
        if (args.length > 0 && args[0] != null) {
            return String.valueOf(args[0]);
        }
        return null;
    }
}
