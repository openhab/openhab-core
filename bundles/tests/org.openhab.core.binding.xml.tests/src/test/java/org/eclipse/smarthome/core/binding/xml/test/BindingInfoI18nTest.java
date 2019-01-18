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
package org.eclipse.smarthome.core.binding.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;

import org.eclipse.smarthome.core.binding.BindingInfo;
import org.eclipse.smarthome.core.binding.BindingInfoRegistry;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.test.SyntheticBundleInstaller;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class BindingInfoI18nTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "yahooweather.bundle";

    private BindingInfoRegistry bindingInfoRegistry;

    @Before
    public void setUp() {
        bindingInfoRegistry = getService(BindingInfoRegistry.class);
        assertThat(bindingInfoRegistry, is(notNullValue()));
    }

    @After
    public void tearDown() throws BundleException {
        SyntheticBundleInstaller.uninstall(bundleContext, TEST_BUNDLE_NAME);
    }

    @Test
    public void assertBindingInfosWereLocalizedInGerman() throws Exception {
        int initialNumberOfBindingInfos = bindingInfoRegistry.getBindingInfos().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(Locale.GERMAN);
        assertThat(bindingInfos.size(), is(initialNumberOfBindingInfos + 1));
        BindingInfo bindingInfo = bindingInfos.iterator().next();

        assertThat(bindingInfo, is(notNullValue()));
        assertThat(bindingInfo.getName(), is("Yahoo Wetter Binding"));
        assertThat(bindingInfo.getDescription(), is(
                "Das Yahoo Wetter Binding stellt verschiedene Wetterdaten wie die Temperatur, die Luftfeuchtigkeit und den Luftdruck für konfigurierbare Orte vom yahoo Wetterdienst bereit"));
    }

    @Test
    public void assertBindingInfosWereLocalizedInDutch() throws Exception {
        int initialNumberOfBindingInfos = bindingInfoRegistry.getBindingInfos().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(new Locale("nl"));
        assertThat(bindingInfos.size(), is(initialNumberOfBindingInfos + 1));
        BindingInfo bindingInfo = bindingInfos.iterator().next();

        assertThat(bindingInfo, is(notNullValue()));
        assertThat(bindingInfo.getName(), is("Yahoo Weer Binding"));
        assertThat(bindingInfo.getDescription(), is(
                "De Yahoo Weer Binding biedt verschillende meteorologische gegevens zoals temperatuur, vochtigheid en luchtdruk voor configureerbare locaties uit yahoo weerdienst klaar"));
    }

    @Test
    public void assertUsingOriginalBindingInfosIfProvidedLocaleIsNotSupported() throws Exception {
        int initialNumberOfBindingInfos = bindingInfoRegistry.getBindingInfos().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(Locale.FRENCH);
        assertThat(bindingInfos.size(), is(initialNumberOfBindingInfos + 1));
        BindingInfo bindingInfo = bindingInfos.iterator().next();

        assertThat(bindingInfo, is(notNullValue()));
        assertThat(bindingInfo.getName(), is("YahooWeather Binding"));
        assertThat(bindingInfo.getDescription(), is(
                "The Yahoo Weather Binding requests the Yahoo Weather Service to show the current temperature, humidity and pressure."));
    }

    @Test
    public void assertUsingDefaultLocale() throws Exception {
        // Set german locale
        ConfigurationAdmin configAdmin = getService(ConfigurationAdmin.class);
        Configuration config = configAdmin.getConfiguration("org.eclipse.smarthome.core.i18nprovider", null);
        Dictionary<String, String> localeCfg = new Hashtable<>();
        localeCfg.put("language", "de");
        localeCfg.put("country", "DE");
        config.update(localeCfg);

        // before running the test with a default locale make sure the locale has been set
        LocaleProvider localeProvider = getService(LocaleProvider.class);
        waitForAssert(() -> assertThat(localeProvider.getLocale().toString(), is("de")));

        int initialNumberOfBindingInfos = bindingInfoRegistry.getBindingInfos().size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Set<BindingInfo> bindingInfos = bindingInfoRegistry.getBindingInfos(/* use default locale */ null);
        assertThat(bindingInfos.size(), is(initialNumberOfBindingInfos + 1));
        BindingInfo bindingInfo = bindingInfos.iterator().next();

        assertThat(bindingInfo, is(notNullValue()));
        assertThat(bindingInfo.getName(), is("Yahoo Wetter Binding"));
        assertThat(bindingInfo.getDescription(), is(
                "Das Yahoo Wetter Binding stellt verschiedene Wetterdaten wie die Temperatur, die Luftfeuchtigkeit und den Luftdruck für konfigurierbare Orte vom yahoo Wetterdienst bereit"));
    }

}
