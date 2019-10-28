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
import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.ephemeris.EphemerisManager;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link EphemerisManagerImpl}.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class EphemerisManagerImplTest extends JavaOSGiTest {
    private @NonNullByDefault({}) EphemerisManagerImpl ephemerisManager;
    private String INTERNAL_DAYSET = "internal";

    @Before
    public void setUp() {
        ephemerisManager = getService(EphemerisManager.class, EphemerisManagerImpl.class);
        assertNotNull(ephemerisManager);

        ephemerisManager.modified(Stream
                .of(new SimpleEntry<>(CONFIG_DAYSET_PREFIX + CONFIG_DAYSET_WEEKEND, "Saturday,Sunday"),
                        new SimpleEntry<>(CONFIG_DAYSET_PREFIX + INTERNAL_DAYSET, "Monday"),
                        new SimpleEntry<>(CONFIG_COUNTRY, Locale.FRANCE.getCountry()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    }

    @Test
    public void testWeekends() {
        ZonedDateTime monday = ZonedDateTime.of(2019, 10, 28, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        ZonedDateTime sunday = ZonedDateTime.of(2019, 10, 27, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));

        assertEquals(true, ephemerisManager.isWeekend(sunday));
        assertEquals(false, ephemerisManager.isWeekend(monday));
        assertEquals(true, ephemerisManager.isInDayset(INTERNAL_DAYSET, monday));
        assertEquals(false, ephemerisManager.isInDayset(INTERNAL_DAYSET, sunday));
    }

    @Test
    public void testIsBankHoliday() {
        ZonedDateTime newyearsday = ZonedDateTime.of(2019, 01, 01, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        ZonedDateTime secondday = ZonedDateTime.of(2019, 01, 02, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));

        boolean vacation = ephemerisManager.isBankHoliday(newyearsday);
        assertEquals(true, vacation);

        vacation = ephemerisManager.isBankHoliday(secondday);
        assertEquals(false, vacation);
    }

    @Test
    public void testGetBankHoliday() {
        ZonedDateTime theDay = ZonedDateTime.of(2019, 01, 01, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));

        boolean vacation = ephemerisManager.isBankHoliday(theDay);
        assertEquals(true, vacation);

        String code = ephemerisManager.getBankHolidayName(theDay);
        assertEquals("NEW_YEAR", code);

        String name = ephemerisManager.getHolidayDescription(code);
        assertEquals("Jour de l'AN", name);
    }

    @Test
    public void testNextBankHoliday() {
        ZonedDateTime today = ZonedDateTime.of(2019, 10, 28, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));

        long delay = ephemerisManager.getDaysUntil(today, "NONEXISTING");
        assertEquals(-1, delay);

        String next = ephemerisManager.getNextBankHoliday(today);
        assertEquals("ALL_SAINTS", next);
        if (next != null) {
            String description = ephemerisManager.getHolidayDescription(next);
            assertEquals("Toussaint", description);

            delay = ephemerisManager.getDaysUntil(today, next);
            assertEquals(4, delay);
        }

    }

    @Test
    public void testUserFile() {
        URL url = bundleContext.getBundle().getResource("events.xml");

        ZonedDateTime today = ZonedDateTime.of(2019, 10, 28, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));
        ZonedDateTime theDay = ZonedDateTime.of(2019, 10, 31, 0, 0, 0, 0, ZoneId.of("Europe/Paris"));

        try {
            boolean vacation = ephemerisManager.isBankHoliday(theDay, url);
            assertEquals(true, vacation);

            long delay = ephemerisManager.getDaysUntil(today, "Halloween", url);
            assertEquals(3, delay);

            String next = ephemerisManager.getNextBankHoliday(today, url);
            assertEquals("Halloween", next);

            String result = ephemerisManager.getBankHolidayName(theDay, url);
            assertEquals("Halloween", result);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
