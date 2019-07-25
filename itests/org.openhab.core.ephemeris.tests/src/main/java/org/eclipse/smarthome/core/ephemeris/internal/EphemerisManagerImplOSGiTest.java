/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.ephemeris.internal;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.core.ephemeris.EphemerisManager;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link EphemerisManagerImpl}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class EphemerisManagerImplOSGiTest extends JavaOSGiTest {

    private static final String COUNTRY_AUSTRALIA_KEY = "au";
    private static final String COUNTRY_AUSTRALIA_NAME = "Australia";
    private static final String REGION_TASMANIA_KEY = "tas";
    private static final String REGION_TASMANIA_NAME = "Tasmania";
    private static final String CITY_HOBARD_AREA_KEY = "ho";
    private static final String CITY_HOBARD_AREA_NAME = "Hobard Area";

    private @NonNullByDefault({}) EphemerisManagerImpl ephemerisManager;

    @Before
    public void setUp() {
        ephemerisManager = getService(EphemerisManager.class, EphemerisManagerImpl.class);
        assertNotNull(ephemerisManager);
    }

    @Test
    public void testEphemerisManagerLoadedProperly() {
        assertFalse(ephemerisManager.countries.isEmpty());
        assertFalse(ephemerisManager.regions.isEmpty());
        assertFalse(ephemerisManager.cities.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsePropertyFailed() {
        ephemerisManager.parseProperty("", "");
    }

    @Test
    public void testParsePropertyCountryCorrectly() {
        final Optional<ParameterOption> option = ephemerisManager.countries.stream()
                .filter(o -> COUNTRY_AUSTRALIA_KEY.equals(o.getValue())).findFirst();
        assertTrue(option.isPresent());
        assertEquals(COUNTRY_AUSTRALIA_KEY, option.get().getValue());
        assertEquals(COUNTRY_AUSTRALIA_NAME, option.get().getLabel());
    }

    @Test
    public void testParsePropertyRegionCorrectly() {
        assertTrue(ephemerisManager.regions.containsKey(COUNTRY_AUSTRALIA_KEY));
        final List<ParameterOption> regions = ephemerisManager.regions.get(COUNTRY_AUSTRALIA_KEY);
        assertFalse(regions.isEmpty());
        final Optional<ParameterOption> option = regions.stream().filter(o -> REGION_TASMANIA_KEY.equals(o.getValue()))
                .findFirst();
        assertTrue(option.isPresent());
        assertEquals(REGION_TASMANIA_KEY, option.get().getValue());
        assertEquals(REGION_TASMANIA_NAME, option.get().getLabel());
    }

    @Test
    public void testParsePropertyCityCorrectly() {
        assertTrue(ephemerisManager.cities.containsKey(REGION_TASMANIA_KEY));
        final List<ParameterOption> cities = ephemerisManager.cities.get(REGION_TASMANIA_KEY);
        assertFalse(cities.isEmpty());
        final Optional<ParameterOption> option = cities.stream().filter(o -> CITY_HOBARD_AREA_KEY.equals(o.getValue()))
                .findFirst();
        assertTrue(option.isPresent());
        assertEquals(CITY_HOBARD_AREA_KEY, option.get().getValue());
        assertEquals(CITY_HOBARD_AREA_NAME, option.get().getLabel());
    }
}
