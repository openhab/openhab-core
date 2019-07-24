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

import static org.eclipse.smarthome.core.ephemeris.internal.EphemerisManagerImpl.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.smarthome.config.core.ParameterOption;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link EphemerisManagerImpl}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class EphemerisManagerImplTest {

    private static final String COUNTRY_AUSTRALIA_KEY = "au";
    private static final String COUNTRY_AUSTRALIA_NAME = "Australia";
    private static final String REGION_TASMANIA_KEY = "tas";
    private static final String REGION_TASMANIA_NAME = "Tasmania";
    private static final String CITY_HOBARD_AREA_KEY = "ho";
    private static final String CITY_HOBARD_AREA_NAME = "Hobard Area";

    @Before
    public void setUp() {
        COUNTRIES.clear();
        REGIONS.clear();
        CITIES.clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParsePropertyFailed() {
        parseProperty("", "");
    }

    @Test
    public void testParsePropertyCountryCorrectly() {
        parseProperty("country.description.au", COUNTRY_AUSTRALIA_NAME);
        assertThat(COUNTRIES.size(), is(1));
        ParameterOption option = COUNTRIES.get(0);
        assertThat(option.getValue(), is(COUNTRY_AUSTRALIA_KEY));
        assertThat(option.getLabel(), is(COUNTRY_AUSTRALIA_NAME));

        assertTrue(REGIONS.isEmpty());
        assertTrue(CITIES.isEmpty());
    }

    @Test
    public void testParsePropertyRegionCorrectly() {
        parseProperty("country.description.au.tas", REGION_TASMANIA_NAME);
        assertTrue(COUNTRIES.isEmpty());

        assertThat(REGIONS.size(), is(1));
        assertTrue(REGIONS.containsKey(COUNTRY_AUSTRALIA_KEY));
        List<ParameterOption> options = REGIONS.get(COUNTRY_AUSTRALIA_KEY);
        assertThat(options.size(), is(1));
        ParameterOption option = options.get(0);
        assertThat(option.getValue(), is(REGION_TASMANIA_KEY));
        assertThat(option.getLabel(), is(REGION_TASMANIA_NAME));

        assertTrue(EphemerisManagerImpl.CITIES.isEmpty());
    }

    @Test
    public void testParsePropertyCityCorrectly() {
        parseProperty("country.description.au.tas.ho", CITY_HOBARD_AREA_NAME);
        assertTrue(COUNTRIES.isEmpty());
        assertTrue(REGIONS.isEmpty());

        assertThat(CITIES.size(), is(1));
        assertTrue(CITIES.containsKey(REGION_TASMANIA_KEY));
        List<ParameterOption> options = CITIES.get(REGION_TASMANIA_KEY);
        assertThat(options.size(), is(1));
        ParameterOption option = options.get(0);
        assertThat(option.getValue(), is(CITY_HOBARD_AREA_KEY));
        assertThat(option.getLabel(), is(CITY_HOBARD_AREA_NAME));
    }
}
