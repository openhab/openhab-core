/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.addon.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.internal.i18n.I18nProviderImpl;
import org.openhab.core.test.java.JavaOSGiTest;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author Dennis Nobel - Initial contribution
 */
@NonNullByDefault
public class AddonInfoI18nTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "acmeweather.bundle";

    private @NonNullByDefault({}) AddonInfoRegistry addonInfoRegistry;
    private @NonNullByDefault({}) AddonInstaller addonInstaller;

    @BeforeEach
    public void setUp() {
        addonInfoRegistry = getService(AddonInfoRegistry.class);
        assertThat(addonInfoRegistry, is(notNullValue()));
        addonInstaller = new AddonInstaller(this::waitForAssert, addonInfoRegistry, bundleContext);
    }

    @Test
    public void assertAddonInfosWereLocalizedInGerman() throws Exception {
        addonInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<AddonInfo> addonInfos = addonInfoRegistry.getAddonInfos(Locale.GERMAN);
            AddonInfo addonInfo = addonInfos.iterator().next();

            assertThat(addonInfo, is(notNullValue()));
            assertThat(addonInfo.getName(), is("ACME Wetter Binding"));
            assertThat(addonInfo.getDescription(), is(
                    "Das ACME Wetter Binding stellt verschiedene Wetterdaten wie die Temperatur, die Luftfeuchtigkeit und den Luftdruck für konfigurierbare Orte vom ACME Wetterdienst bereit"));
        });
    }

    @Test
    public void assertAddonInfosWereLocalizedInDutch() throws Exception {
        addonInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<AddonInfo> bindingInfos = addonInfoRegistry.getAddonInfos(new Locale("nl"));
            AddonInfo bindingInfo = bindingInfos.iterator().next();

            assertThat(bindingInfo, is(notNullValue()));
            assertThat(bindingInfo.getName(), is("ACME Weer Binding"));
            assertThat(bindingInfo.getDescription(), is(
                    "De ACME Weer Binding biedt verschillende meteorologische gegevens zoals temperatuur, vochtigheid en luchtdruk voor configureerbare locaties uit ACME weerdienst klaar"));
        });
    }

    @Test
    public void assertUsingOriginalAddonInfosIfProvidedLocaleIsNotSupported() throws Exception {
        addonInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<AddonInfo> bindingInfos = addonInfoRegistry.getAddonInfos(Locale.FRENCH);
            AddonInfo bindingInfo = bindingInfos.iterator().next();

            assertThat(bindingInfo, is(notNullValue()));
            assertThat(bindingInfo.getName(), is("ACME Weather Binding"));
            assertThat(bindingInfo.getDescription(), is(
                    "The ACME Weather Binding requests the ACME Weather Service to show the current temperature, humidity and pressure."));
        });
    }

    @Test
    public void assertUsingDefaultLocale() throws Exception {
        // Set german locale
        ConfigurationAdmin configAdmin = getService(ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));

        Configuration config = configAdmin.getConfiguration(I18nProviderImpl.CONFIGURATION_PID, null);
        assertThat(config, is(notNullValue()));

        Dictionary<String, Object> properties = config.getProperties();
        if (properties == null) {
            properties = new Hashtable<>();
        }

        properties.put("language", "de");
        properties.put("region", "DE");

        config.update(properties);

        // before running the test with a default locale make sure the locale has been set
        LocaleProvider localeProvider = getService(LocaleProvider.class);
        assertThat(localeProvider, is(notNullValue()));

        waitForAssert(() -> assertThat(localeProvider.getLocale().toString(), is("de_DE")));

        addonInstaller.exec(TEST_BUNDLE_NAME, () -> {
            // use default locale
            Set<AddonInfo> bindingInfos = addonInfoRegistry.getAddonInfos(null);
            AddonInfo bindingInfo = bindingInfos.iterator().next();
            assertThat(bindingInfo, is(notNullValue()));
            assertThat(bindingInfo.getName(), is("ACME Wetter Binding"));
            assertThat(bindingInfo.getDescription(), is(
                    "Das ACME Wetter Binding stellt verschiedene Wetterdaten wie die Temperatur, die Luftfeuchtigkeit und den Luftdruck für konfigurierbare Orte vom ACME Wetterdienst bereit"));
        });
    }
}
