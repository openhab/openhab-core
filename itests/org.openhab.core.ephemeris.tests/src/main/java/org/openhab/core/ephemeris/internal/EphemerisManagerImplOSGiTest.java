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
package org.openhab.core.ephemeris.internal;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.ephemeris.internal.EphemerisManagerImpl.*;

import java.net.URI;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.ephemeris.EphemerisManager;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * Test class for {@link EphemerisManagerImpl}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class EphemerisManagerImplOSGiTest extends JavaOSGiTest {
    private static final String INTERNAL_DAYSET = "internal";
    private static final URI CONFIG_URI = URI.create("system:ephemeris");
    private static final String COUNTRY_AUSTRALIA_KEY = "au";
    private static final String COUNTRY_AUSTRALIA_NAME = "Australia";
    private static final String REGION_BAVARIA_KEY = "by";
    private static final String REGION_NORTHRHINEWESTPHALIA_KEY = "nw";
    private static final String REGION_TASMANIA_KEY = "tas";
    private static final String REGION_TASMANIA_NAME = "Tasmania";
    private static final String CITY_HOBARD_AREA_KEY = "ho";
    private static final String CITY_HOBARD_AREA_NAME = "Hobard Area";

    private @NonNullByDefault({}) EphemerisManagerImpl ephemerisManager;

    @BeforeEach
    public void setUp() {
        ephemerisManager = getService(EphemerisManager.class, EphemerisManagerImpl.class);
        assertNotNull(ephemerisManager);

        ephemerisManager.modified(Map.ofEntries(entry(CONFIG_DAYSET_PREFIX + CONFIG_DAYSET_WEEKEND, "Saturday,Sunday"),
                entry(CONFIG_COUNTRY, Locale.GERMANY.getCountry())));
    }

    @Test
    public void testEphemerisManagerFixesIllegalCharacters() {
        ephemerisManager.modified(Map.ofEntries(entry(CONFIG_DAYSET_PREFIX + CONFIG_DAYSET_WEEKEND, "\"( \"MONDAY\"")));

        assertTrue(ephemerisManager.daysets.containsKey(CONFIG_DAYSET_WEEKEND));
    }

    @Test
    public void testEphemerisManagerDoesNotCrashOnIllegalName() {
        ephemerisManager.modified(Map.ofEntries(entry(CONFIG_DAYSET_PREFIX + CONFIG_DAYSET_WEEKEND, "Mondax")));

        // assertion only to check if no exception occurs
        assertFalse(ephemerisManager.daysets.isEmpty());
    }

    @Test
    public void testEphemerisManagerLoadedProperly() {
        assertTrue(ephemerisManager.daysets.containsKey(CONFIG_DAYSET_WEEKEND));
        assertEquals(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), ephemerisManager.daysets.get(CONFIG_DAYSET_WEEKEND));
        assertFalse(ephemerisManager.countries.isEmpty());
        assertFalse(ephemerisManager.regions.isEmpty());
        assertFalse(ephemerisManager.cities.isEmpty());
    }

    @Test
    public void testConfigurationDaysetWeekendIterable() {
        ephemerisManager.modified(Map.of(CONFIG_DAYSET_PREFIX + CONFIG_DAYSET_WEEKEND, List.of("Saturday", "Sunday")));
        assertTrue(ephemerisManager.daysets.containsKey(CONFIG_DAYSET_WEEKEND));
        assertEquals(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), ephemerisManager.daysets.get(CONFIG_DAYSET_WEEKEND));
    }

    @Test
    public void testConfigurationDaysetWeekendListAsString() {
        ephemerisManager.modified(Map.of(CONFIG_DAYSET_PREFIX + CONFIG_DAYSET_WEEKEND, List.of("Saturday", "Sunday")));
        assertTrue(ephemerisManager.daysets.containsKey(CONFIG_DAYSET_WEEKEND));
        assertEquals(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), ephemerisManager.daysets.get(CONFIG_DAYSET_WEEKEND));
    }

    @Test
    public void testParsePropertyFailed() {
        assertThrows(IllegalArgumentException.class, () -> ephemerisManager.parseProperty("", ""));
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

    @Test
    public void testConfigOptionProviderDaysetDefault() {
        final Collection<ParameterOption> options = ephemerisManager.getParameterOptions(CONFIG_URI, "dayset-weekend",
                null, null);
        assertNotNull(options);
        assertEquals(7, options.size());
    }

    @Test
    public void testConfigOptionProviderDaysetUS() {
        final Collection<ParameterOption> options = ephemerisManager.getParameterOptions(CONFIG_URI, "dayset-weekend",
                null, Locale.US);
        assertNotNull(options);
        assertEquals(7, options.size());
        assertEquals(List.of(new ParameterOption("MONDAY", "Monday"), new ParameterOption("TUESDAY", "Tuesday"),
                new ParameterOption("WEDNESDAY", "Wednesday"), new ParameterOption("THURSDAY", "Thursday"),
                new ParameterOption("FRIDAY", "Friday"), new ParameterOption("SATURDAY", "Saturday"),
                new ParameterOption("SUNDAY", "Sunday")), options);
    }

    @Test
    public void testConfigOptionProviderDaysetGerman() {
        final Collection<ParameterOption> options = ephemerisManager.getParameterOptions(CONFIG_URI, "dayset-weekend",
                null, Locale.GERMAN);
        assertNotNull(options);
        assertEquals(7, options.size());
        assertEquals(List.of(new ParameterOption("MONDAY", "Montag"), new ParameterOption("TUESDAY", "Dienstag"),
                new ParameterOption("WEDNESDAY", "Mittwoch"), new ParameterOption("THURSDAY", "Donnerstag"),
                new ParameterOption("FRIDAY", "Freitag"), new ParameterOption("SATURDAY", "Samstag"),
                new ParameterOption("SUNDAY", "Sonntag")), options);
    }

    @Test
    public void testConfigOptionProviderCountries() {
        final Collection<ParameterOption> options = ephemerisManager.getParameterOptions(CONFIG_URI, CONFIG_COUNTRY,
                null, null);
        assertNotNull(options);
        assertFalse(options.isEmpty());
        assertEquals(ephemerisManager.countries, options);
    }

    @Test
    public void testConfigOptionProviderRegionsAustria() {
        ephemerisManager.modified(Map.of(CONFIG_COUNTRY, COUNTRY_AUSTRALIA_KEY));

        final Collection<ParameterOption> options = ephemerisManager.getParameterOptions(CONFIG_URI, CONFIG_REGION,
                null, null);
        assertNotNull(options);
        assertEquals(8, options.size());
        assertEquals(ephemerisManager.regions.get(COUNTRY_AUSTRALIA_KEY), options);
    }

    @Test
    public void testConfigOptionProviderRegionsGermany() {
        final Collection<ParameterOption> options = ephemerisManager.getParameterOptions(CONFIG_URI, CONFIG_REGION,
                null, null);
        assertNotNull(options);
        assertEquals(16, options.size());
        assertEquals(ephemerisManager.regions.get(Locale.GERMANY.getCountry().toLowerCase()), options);
    }

    @Test
    public void testConfigOptionProviderCitiesNorthRhineWestphalia() {
        ephemerisManager.modified(Stream
                .of(entry(CONFIG_COUNTRY, Locale.GERMANY.getCountry()),
                        entry(CONFIG_REGION, REGION_NORTHRHINEWESTPHALIA_KEY))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));

        final Collection<ParameterOption> options = ephemerisManager.getParameterOptions(CONFIG_URI, CONFIG_CITY, null,
                null);
        assertNull(options);
    }

    @Test
    public void testConfigOptionProviderCitiesBavaria() {
        ephemerisManager.modified(Map.ofEntries(entry(CONFIG_COUNTRY, Locale.GERMANY.getCountry()),
                entry(CONFIG_REGION, REGION_BAVARIA_KEY)));

        final Collection<ParameterOption> options = ephemerisManager.getParameterOptions(CONFIG_URI, CONFIG_CITY, null,
                null);
        assertNotNull(options);
        assertFalse(options.isEmpty());
        assertEquals(ephemerisManager.cities.get(REGION_BAVARIA_KEY), options);
    }

    @Test
    public void testConfigOptionProviderCitiesTasmania() {
        ephemerisManager.modified(Map.ofEntries(entry(CONFIG_COUNTRY, Locale.GERMANY.getCountry()),
                entry(CONFIG_REGION, REGION_TASMANIA_KEY)));

        final Collection<ParameterOption> options = ephemerisManager.getParameterOptions(CONFIG_URI, CONFIG_CITY, null,
                null);
        assertNotNull(options);
        assertFalse(options.isEmpty());
        assertEquals(ephemerisManager.cities.get(REGION_TASMANIA_KEY), options);
    }

    @Test
    public void testWeekends() {
        ZonedDateTime monday = ZonedDateTime.of(2019, 10, 28, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        ZonedDateTime sunday = ZonedDateTime.of(2019, 10, 27, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        ephemerisManager.modified(Map.of(CONFIG_DAYSET_PREFIX + INTERNAL_DAYSET, List.of("Monday", "Tuesday")));
        assertTrue(ephemerisManager.isWeekend(sunday));
        assertFalse(ephemerisManager.isWeekend(monday));
        assertTrue(ephemerisManager.isInDayset(INTERNAL_DAYSET, monday));
        assertFalse(ephemerisManager.isInDayset(INTERNAL_DAYSET, sunday));
    }

    @Test
    public void testIsBankHoliday() {
        ZonedDateTime newyearsday = ZonedDateTime.of(2019, 01, 01, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        ZonedDateTime secondday = ZonedDateTime.of(2019, 01, 02, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));

        boolean vacation = ephemerisManager.isBankHoliday(newyearsday);
        assertTrue(vacation);

        vacation = ephemerisManager.isBankHoliday(secondday);
        assertFalse(vacation);
    }

    @Test
    public void testGetBankHoliday() {
        ZonedDateTime theDay = ZonedDateTime.of(2019, 01, 01, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));

        boolean vacation = ephemerisManager.isBankHoliday(theDay);
        assertTrue(vacation);

        String code = ephemerisManager.getBankHolidayName(theDay);
        assertEquals("NEW_YEAR", code);

        String name = ephemerisManager.getHolidayDescription(code);
        assertNotNull(name);
    }

    @Test
    public void testNextBankHoliday() {
        ZonedDateTime today = ZonedDateTime.of(2019, 12, 28, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        long delay = ephemerisManager.getDaysUntil(today, "NONEXISTING");
        assertEquals(-1, delay);

        String next = ephemerisManager.getNextBankHoliday(today);
        assertEquals("NEW_YEAR", next);
        if (next != null) {
            String description = ephemerisManager.getHolidayDescription(next);
            assertNotNull(description);

            delay = ephemerisManager.getDaysUntil(today, next);
            assertEquals(4, delay);
        }
    }

    @Test
    public void testUserFile() {
        URL url = bundleContext.getBundle().getResource("events.xml");

        ZonedDateTime today = ZonedDateTime.of(2019, 10, 28, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        ZonedDateTime theDay = ZonedDateTime.of(2019, 10, 31, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));

        boolean vacation = ephemerisManager.isBankHoliday(theDay, url);
        assertTrue(vacation);

        long delay = ephemerisManager.getDaysUntil(today, "Halloween", url);
        assertEquals(3, delay);

        String next = ephemerisManager.getNextBankHoliday(today, url);
        assertEquals("Halloween", next);

        String result = ephemerisManager.getBankHolidayName(theDay, url);
        assertEquals("Halloween", result);
    }
}
