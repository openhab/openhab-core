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
package org.eclipse.smarthome.core.internal.i18n;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import org.eclipse.smarthome.core.common.osgi.ResourceBundleClassLoader;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.osgi.framework.Bundle;

/**
 * The {@link LanguageResourceBundleManager} class manages all available i18n resources for one
 * specific <i>OSGi</i> bundle. Any i18n resource is searched within the {@link RESOURCE_DIRECTORY} of the bundle and
 * <i>not</i> within the general bundle classpath. For the translation, the
 * i18n mechanism of Java ({@link ResourceBundle}) is used.
 * <p>
 * This implementation uses the user defined {@link ResourceBundleClassLoader} to map the bundle resource files to usual
 * URLs which the Java {@link ResourceBundle} can handle.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Markus Rathgeb - Add locale provider support
 */
public class LanguageResourceBundleManager {

    /** The directory within the bundle where the resource files are searched. */
    protected static final String RESOURCE_DIRECTORY = "/ESH-INF/i18n";

    /** The file pattern to filter out resource files. */
    private static final String RESOURCE_FILE_PATTERN = "*.properties";

    private LocaleProvider localeProvider;
    private Bundle bundle;
    private ClassLoader resourceClassLoader;
    private List<String> resourceNames;

    public LanguageResourceBundleManager(LocaleProvider localeProvider, Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("The Bundle must not be null!");
        }

        this.localeProvider = localeProvider;
        this.bundle = bundle;
        this.resourceClassLoader = new ResourceBundleClassLoader(bundle, RESOURCE_DIRECTORY, RESOURCE_FILE_PATTERN);
        this.resourceNames = determineResourceNames();
    }

    public Bundle getBundle() {
        return this.bundle;
    }

    /**
     * Releases any cached resources which were managed by this class from the {@link ResourceBundle}.
     */
    public void clearCache() {
        ResourceBundle.clearCache(this.resourceClassLoader);
    }

    /**
     * Returns {@code true} if the specified resource is managed by this instance
     * and therefore the according module is responsible for translations,
     * otherwise {@code false}.
     *
     * @param resource the resource to check (could be null or empty)
     * @return true if the specified resource is managed by this instance, otherwise false
     */
    public boolean containsResource(String resource) {
        if (resource != null) {
            return this.resourceNames.contains(resource);
        }

        return false;
    }

    /**
     * Returns {@code true} if this instance and therefore the according module provides
     * resource information, otherwise {@code false}.
     *
     * @return true if the according bundle provides resource information, otherwise false
     */
    public boolean containsResources() {
        return (this.resourceNames.size() > 0);
    }

    private List<String> determineResourceNames() {
        List<String> resourceNames = new ArrayList<String>();

        Enumeration<URL> resourceFiles = this.bundle.findEntries(RESOURCE_DIRECTORY, RESOURCE_FILE_PATTERN, true);

        if (resourceFiles != null) {
            while (resourceFiles.hasMoreElements()) {
                URL resourceURL = resourceFiles.nextElement();
                String resourcePath = resourceURL.getFile();
                File resourceFile = new File(resourcePath);
                String resourceFileName = resourceFile.getName();
                String baseName = resourceFileName.replaceFirst("[._]+.*", "");

                if (!resourceNames.contains(baseName)) {
                    resourceNames.add(baseName);
                }
            }
        }

        return resourceNames;
    }

    /**
     * Returns a translation for the specified key in the specified locale (language) by only
     * considering the specified resource section. The resource is equal to a base name and
     * therefore it is mapped to one translation package (all files which belong to the base
     * name).
     * <p>
     * If no translation could be found, {@code null} is returned. If the location is not specified, the default
     * location is used.
     *
     * @param resource the resource to be used for look-up (could be null or empty)
     * @param key the key to be translated (could be null or empty)
     * @param locale the locale (language) to be used (could be null)
     *
     * @return the translated text, or null if the key could not be translated
     */
    public String getText(String resource, String key, Locale locale) {
        if ((key != null) && (!key.isEmpty())) {
            Locale effectiveLocale = locale != null ? locale : localeProvider.getLocale();

            if (resource != null) {
                return getTranslatedText(resource, key, effectiveLocale);
            } else {
                for (String resourceName : this.resourceNames) {
                    String text = getTranslatedText(resourceName, key, effectiveLocale);

                    if (text != null) {
                        return text;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns a translation for the specified key in the specified locale (language)
     * by considering all resources in the according bundle.
     * <p>
     * If no translation could be found, {@code null} is returned. If the location is not specified, the default
     * location is used.
     *
     * @param key the key to be translated (could be null or empty)
     * @param locale the locale (language) to be used (could be null)
     *
     * @return the translated text, or null if the key could not be translated
     */
    public String getText(String key, Locale locale) {
        return getText(null, key, locale);
    }

    private String getTranslatedText(String resourceName, String key, Locale locale) {
        try {
            // Modify the search order so that the following applies:
            // 1.) baseName + "_" + language + "_" + country
            // 2.) baseName + "_" + language
            // 3.) baseName
            // 4.) null -> leads to a default text
            // Not using the default fallback strategy helps that not the default locale
            // search order is applied between 2.) and 3.).
            ResourceBundle resourceBundle = ResourceBundle.getBundle(resourceName, locale, this.resourceClassLoader,
                    Control.getNoFallbackControl(Control.FORMAT_PROPERTIES));

            if (resourceBundle != null) {
                return resourceBundle.getString(key);
            }
        } catch (Exception ex) {
            // nothing to do
        }

        return null;
    }

}
