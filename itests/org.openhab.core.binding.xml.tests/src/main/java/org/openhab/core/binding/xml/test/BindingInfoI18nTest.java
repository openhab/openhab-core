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
package org.openhab.core.binding.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.binding.BindingInfo;
import org.openhab.core.binding.BindingInfoRegistry;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.internal.i18n.I18nProviderImpl;
import org.openhab.core.test.java.JavaOSGiTest;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author Dennis Nobel - Initial contribution
 */
public class BindingInfoI18nTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "yahooweather.bundle";

    private BindingInfoRegistry bindingInfoRegistry;
    private BindingInstaller bindingInstaller;

    @Before
    public void setUp() {
        bindingInfoRegistry = getService(BindingInfoRegistry.class);
        assertThat(bindingInfoRegistry, is(notNullValue()));
        bindingInstaller = new BindingInstaller(this::waitForAssert, bindingInfoRegistry, bundleContext);
    }

    @Test
    public void assertBindingInfosWereLocalizedInGerman() throws Exception {
        bindingInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(Locale.GERMAN);
            BindingInfo bindingInfo = bindingInfos.iterator().next();

            assertThat(bindingInfo, is(notNullValue()));
            assertThat(bindingInfo.getName(), is("Yahoo Wetter Binding"));
            assertThat(bindingInfo.getDescription(), is(
                    "Das Yahoo Wetter Binding stellt verschiedene Wetterdaten wie die Temperatur, die Luftfeuchtigkeit und den Luftdruck für konfigurierbare Orte vom yahoo Wetterdienst bereit"));
        });
    }

    @Test
    public void assertBindingInfosWereLocalizedInDutch() throws Exception {
        bindingInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(new Locale("nl"));
            BindingInfo bindingInfo = bindingInfos.iterator().next();

            assertThat(bindingInfo, is(notNullValue()));
            assertThat(bindingInfo.getName(), is("Yahoo Weer Binding"));
            assertThat(bindingInfo.getDescription(), is(
                    "De Yahoo Weer Binding biedt verschillende meteorologische gegevens zoals temperatuur, vochtigheid en luchtdruk voor configureerbare locaties uit yahoo weerdienst klaar"));
        });
    }

    @Test
    public void assertUsingOriginalBindingInfosIfProvidedLocaleIsNotSupported() throws Exception {
        bindingInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(Locale.FRENCH);
            BindingInfo bindingInfo = bindingInfos.iterator().next();

            assertThat(bindingInfo, is(notNullValue()));
            assertThat(bindingInfo.getName(), is("YahooWeather Binding"));
            assertThat(bindingInfo.getDescription(), is(
                    "The Yahoo Weather Binding requests the Yahoo Weather Service to show the current temperature, humidity and pressure."));
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

        bindingInstaller.exec(TEST_BUNDLE_NAME, () -> {
            // use default locale
            Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(null);
            BindingInfo bindingInfo = bindingInfos.iterator().next();
            assertThat(bindingInfo, is(notNullValue()));
            assertThat(bindingInfo.getName(), is("Yahoo Wetter Binding"));
            assertThat(bindingInfo.getDescription(), is(
                    "Das Yahoo Wetter Binding stellt verschiedene Wetterdaten wie die Temperatur, die Luftfeuchtigkeit und den Luftdruck für konfigurierbare Orte vom yahoo Wetterdienst bereit"));
        });
    }

}
