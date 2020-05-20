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
package org.openhab.core.internal.i18n;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.openhab.core.internal.i18n.I18nProviderImpl.*;

import java.time.ZoneId;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.library.types.PointType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * The {@link I18nProviderImplTest} tests the basic functionality of the {@link I18nProviderImpl} OSGi service.
 *
 * @author Stefan Triller - Initial contribution
 */
public class I18nProviderImplTest {

    private static final String LOCATION_ZERO = "0,0";
    private static final String LOCATION_DARMSTADT = "49.876733,8.666809,1";
    private static final String LOCATION_HAMBURG = "53.588231,9.920082,5";

    private static final String LANGUAGE_DE = "de";
    private static final String LANGUAGE_RU = "ru";

    private static final String SCRIPT_DE = "Latn";
    private static final String SCRIPT_RU = "Cyrl";

    private static final String REGION_DE = "DE";
    private static final String REGION_RU = "RU";

    private static final String VARIANT_DE = "1996";
    private static final String VARIANT_RU = "";

    private static final String TIMEZONE_GMT9 = "Etc/GMT-9";

    private I18nProviderImpl i18nProviderImpl;
    private Dictionary<String, Object> initialConfig = new Hashtable<>();

    @Mock
    private ComponentContext componentContext;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private Bundle bundle;

    @Before
    public void setup() {
        initMocks(this);
        initialConfig = buildInitialConfig();
        when(componentContext.getProperties()).thenReturn(initialConfig);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getBundles()).thenReturn(new Bundle[] { bundle });

        i18nProviderImpl = new I18nProviderImpl(componentContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void assertThatConfigurationWasSet() {
        i18nProviderImpl.modified((Map<String, Object>) initialConfig);

        PointType location = i18nProviderImpl.getLocation();
        Locale setLocale = i18nProviderImpl.getLocale();

        assertThat(location.toString(), is(LOCATION_ZERO));

        assertThat(setLocale.getLanguage(), is(initialConfig.get(LANGUAGE)));
        assertThat(setLocale.getScript(), is(initialConfig.get(SCRIPT)));
        assertThat(setLocale.getCountry(), is(initialConfig.get(REGION)));
        assertThat(setLocale.getVariant(), is(initialConfig.get(VARIANT)));
        assertThat(i18nProviderImpl.getTimeZone(), is(ZoneId.of(TIMEZONE_GMT9)));
    }

    @Test
    public void assertThatDefaultLocaleWillBeUsed() {
        i18nProviderImpl.modified(new Hashtable<>());

        PointType location = i18nProviderImpl.getLocation();
        Locale setLocale = i18nProviderImpl.getLocale();

        assertNull(location);
        assertThat(i18nProviderImpl.getTimeZone(), is(TimeZone.getDefault().toZoneId()));
        assertThat(setLocale, is(Locale.getDefault()));
    }

    @Test
    public void assertThatDefaultLocaleWillBeUsedAndLocationIsSet() {
        Hashtable<String, Object> conf = new Hashtable<>();
        conf.put(LOCATION, LOCATION_DARMSTADT);
        i18nProviderImpl.modified(conf);

        PointType location = i18nProviderImpl.getLocation();
        Locale setLocale = i18nProviderImpl.getLocale();

        assertThat(location.toString(), is(LOCATION_DARMSTADT));
        assertThat(setLocale, is(Locale.getDefault()));
    }

    @Test
    public void assertThatActivateSetsLocaleAndLocation() {
        PointType location = i18nProviderImpl.getLocation();
        Locale setLocale = i18nProviderImpl.getLocale();

        assertThat(location.toString(), is(LOCATION_ZERO));

        assertThat(setLocale.getLanguage(), is(initialConfig.get(LANGUAGE)));
        assertThat(setLocale.getScript(), is(initialConfig.get(SCRIPT)));
        assertThat(setLocale.getCountry(), is(initialConfig.get(REGION)));
        assertThat(setLocale.getVariant(), is(initialConfig.get(VARIANT)));
    }

    @Test
    public void assertThatInvalidTimeZoneFallsbackToDefaultTimeZone() {
        Hashtable<String, Object> conf = new Hashtable<>();
        conf.put(TIMEZONE, "invalid");
        i18nProviderImpl.modified(conf);

        assertThat(i18nProviderImpl.getTimeZone(), is(TimeZone.getDefault().toZoneId()));
    }

    @Test
    public void assertThatConfigurationChangeWorks() {
        i18nProviderImpl.modified(buildRUConfig());

        PointType location = i18nProviderImpl.getLocation();
        Locale setLocale = i18nProviderImpl.getLocale();

        assertThat(location.toString(), is(LOCATION_HAMBURG));

        assertThat(setLocale.getLanguage(), is(LANGUAGE_RU));
        assertThat(setLocale.getScript(), is(SCRIPT_RU));
        assertThat(setLocale.getCountry(), is(REGION_RU));
        assertThat(setLocale.getVariant(), is(VARIANT_RU));
    }

    private Dictionary<String, Object> buildInitialConfig() {
        Dictionary<String, Object> conf = new Hashtable<>();
        conf.put(LOCATION, LOCATION_ZERO);
        conf.put(LANGUAGE, LANGUAGE_DE);
        conf.put(SCRIPT, SCRIPT_DE);
        conf.put(REGION, REGION_DE);
        conf.put(VARIANT, VARIANT_DE);
        conf.put(TIMEZONE, TIMEZONE_GMT9);

        return conf;
    }

    private Hashtable<String, Object> buildRUConfig() {
        Hashtable<String, Object> conf = new Hashtable<>();
        conf.put(LOCATION, LOCATION_HAMBURG);
        conf.put(LANGUAGE, LANGUAGE_RU);
        conf.put(SCRIPT, SCRIPT_RU);
        conf.put(REGION, REGION_RU);
        conf.put(VARIANT, VARIANT_RU);
        return conf;
    }
}
