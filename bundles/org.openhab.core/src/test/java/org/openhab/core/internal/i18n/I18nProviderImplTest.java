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
package org.openhab.core.internal.i18n;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.openhab.core.internal.i18n.I18nProviderImpl.*;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.spi.SystemOfUnits;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.util.UnitUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * The {@link I18nProviderImplTest} tests the basic functionality of the {@link I18nProviderImpl} OSGi service.
 *
 * @author Stefan Triller - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
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

    private @NonNullByDefault({}) I18nProviderImpl i18nProviderImpl;
    private Dictionary<String, Object> initialConfig = new Hashtable<>();

    private @Mock @NonNullByDefault({}) ComponentContext componentContextMock;
    private @Mock @NonNullByDefault({}) BundleContext bundleContextMock;
    private @Mock @NonNullByDefault({}) Bundle bundleMock;

    @BeforeEach
    public void setup() {
        initialConfig = buildInitialConfig();
        when(componentContextMock.getProperties()).thenReturn(initialConfig);
        when(componentContextMock.getBundleContext()).thenReturn(bundleContextMock);
        when(bundleContextMock.getBundles()).thenReturn(new Bundle[] { bundleMock });

        i18nProviderImpl = new I18nProviderImpl(componentContextMock);
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
        assertThat(i18nProviderImpl.getTimeZone(), is(ZoneId.systemDefault()));
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

        assertThat(i18nProviderImpl.getTimeZone(), is(ZoneId.systemDefault()));
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

    @ParameterizedTest
    @MethodSource("getAllDimensions")
    @SuppressWarnings("unchecked")
    public <T extends Quantity<T>> void assertThatUnitProviderIsComplete(String dimensionName) {
        Class<? extends Quantity<?>> dimension = UnitUtils.parseDimension(dimensionName);
        assertThat(dimension, is(notNullValue()));

        Unit<?> defaultUnit = i18nProviderImpl.getUnit((Class<T>) dimension);
        assertThat(dimensionName + " has no default unit", defaultUnit, notNullValue());
    }

    private static Stream<String> getAllDimensions() {
        return Stream.of(SIUnits.getInstance(), Units.getInstance(), ImperialUnits.getInstance())
                .map(SystemOfUnits::getUnits).flatMap(Collection::stream) //
                .map(UnitUtils::getDimensionName).filter(Objects::nonNull).map(Objects::requireNonNull).distinct();
    }

    @Test
    public void testFormattingTexts() {
        String defaultText = "formatString {0} {1,number}";
        String successResult = i18nProviderImpl.getText(bundleMock, "testKey", defaultText, null, false, 1);
        assertThat(successResult, is("formatString false 1"));

        // make sure bugs are properly handled
        String failedResult = i18nProviderImpl.getText(bundleMock, "testKey", defaultText, null, false, "foo");
        assertThat(failedResult, is(defaultText));
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
