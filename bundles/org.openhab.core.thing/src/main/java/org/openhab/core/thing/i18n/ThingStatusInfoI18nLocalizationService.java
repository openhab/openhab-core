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
package org.openhab.core.thing.i18n;

import java.util.Arrays;
import java.util.Locale;

import org.openhab.core.i18n.I18nUtil;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * <p>
 * The {@link ThingStatusInfoI18nLocalizationService} can be used to localize the {@link ThingStatusInfo} of a thing
 * using the I18N mechanism of the openHAB framework. Currently the description of the {@link ThingStatusInfo}
 * is the single attribute which can be localized.
 *
 * <p>
 * In order to provide a localized description the corresponding {@link ThingHandler} of the thing does not provide a
 * localized string in the <i>ThingStatus.description</i> attribute, but instead provides the reference of the
 * localization string, e.g &#64;text/rate_limit. The handler is able to provide placeholder values as a JSON-serialized
 * array of strings:
 *
 * <pre>
 * &#64;text/rate_limit ["60", "10", "@text/hour"]
 * </pre>
 *
 * <pre>
 * rate_limit=Device is blocked by remote service for {0} minutes. Maximum limit of {1} configuration
 * changes per {2} has been exceeded. For further info please refer to device vendor.
 * </pre>
 *
 * @author Thomas Höfer - Initial contribution
 * @author Henning Sudbrock - Permit translations from thing handler parent bundles
 */
@Component(service = ThingStatusInfoI18nLocalizationService.class)
public final class ThingStatusInfoI18nLocalizationService {

    private TranslationProvider i18nProvider;
    private BundleResolver bundleResolver;

    /**
     * Localizes the {@link ThingStatusInfo} for the given thing.
     *
     * @param thing the thing whose thing status info is to be localized (must not be null)
     * @param locale the locale to be used (can be null)
     * @return the localized thing status or the original thing status if
     *         <ul>
     *         <li>there is nothing to be localized</li>
     *         <li>the thing does not have a handler</li>
     *         </ul>
     * @throws IllegalArgumentException if given thing is null
     */
    public ThingStatusInfo getLocalizedThingStatusInfo(Thing thing, Locale locale) {
        if (thing == null) {
            throw new IllegalArgumentException("Thing must not be null.");
        }

        ThingHandler thingHandler = thing.getHandler();

        if (thingHandler == null) {
            return thing.getStatusInfo();
        }

        String description = thing.getStatusInfo().getDescription();
        if (description == null || !I18nUtil.isConstant(description)) {
            return thing.getStatusInfo();
        }

        String translatedDescription = translateDescription(description, locale, thingHandler);

        return new ThingStatusInfo(thing.getStatus(), thing.getStatusInfo().getStatusDetail(), translatedDescription);
    }

    @Reference
    protected void setTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    protected void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = null;
    }

    @Reference
    public void setBundleResolver(BundleResolver bundleResolver) {
        this.bundleResolver = bundleResolver;
    }

    protected void unsetBundleResolver(BundleResolver bundleResolver) {
        this.bundleResolver = bundleResolver;
    }

    /**
     * Returns the translation of the description for the specified locale, using the translations from the bundles of
     * the given thingHandler and its parent classes. The description may contain arguments that may also need
     * translation (see class JavaDoc for an example); those arguments are translated in the same way.
     */
    private String translateDescription(String description, Locale locale, ThingHandler thingHandler) {
        ParsedDescription parsedDescription = new ParsedDescription(description);

        Object[] translatedArgs = null;
        if (parsedDescription.args != null) {
            translatedArgs = Arrays.stream(parsedDescription.args).map(arg -> {
                if (I18nUtil.isConstant(arg)) {
                    return getTranslationForClass(arg, locale, thingHandler.getClass());
                } else {
                    return arg;
                }
            }).toArray(String[]::new);
        }

        return getTranslationForClass(parsedDescription.key, locale, thingHandler.getClass(), translatedArgs);
    }

    /**
     * Returns the translation for the given i18n constant and locale using the translations from the bundle of the
     * given class; if there is no translation look up the translation in the bundle in the parent class, and so
     * forth. If no translation is found for the bundle of any parent class, return the i18n constant.
     */
    private String getTranslationForClass(String i18nConstant, Locale locale, Class<?> clazz, Object... args) {
        if (clazz == null) {
            return i18nConstant;
        }

        Bundle bundle = bundleResolver.resolveBundle(clazz);

        if (bundle == null) {
            return getTranslationForClass(i18nConstant, locale, clazz.getSuperclass(), args);
        }

        String translatedDescription = i18nProvider.getText(bundle, I18nUtil.stripConstant(i18nConstant), null, locale,
                args);

        if (translatedDescription != null) {
            return translatedDescription;
        } else {
            return getTranslationForClass(i18nConstant, locale, clazz.getSuperclass(), args);
        }
    }

    /**
     * Utility class to parse the thing status description into the text reference and optional arguments.
     */
    private final class ParsedDescription {

        private static final int LIMIT = 2;

        private final String key;
        private final String[] args;

        private ParsedDescription(String description) {
            String[] parts = description.split("\\s+", LIMIT);
            this.key = parts[0];

            if (parts.length == 1) {
                this.args = null;
            } else {
                this.args = Arrays.stream(parts[1].replaceAll("\\[|\\]|\"", "").split(","))
                        .filter(s -> s != null && !s.trim().isEmpty()).map(s -> s.trim()).toArray(String[]::new);
            }
        }
    }
}
