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
package org.eclipse.smarthome.core.library.types;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Thomas.Eichstaedt-Engelen
 * @author Gaël L'hopital - Added Timezone and Milliseconds
 * @author Erdoan Hadzhiyusein - Added ZonedDateTime tests
 */
@RunWith(Parameterized.class)
public class DateTimeTypeTest {

    /**
     * parameter test set class.
     * each instance of this class represents a test which executes the test once.
     */
    public static class ParameterSet {
        /**
         * the java default time zone to set.
         * this should not change a result, except for wrong time zone informations,
         * then this default time zone is used.
         */
        public final TimeZone defaultTimeZone;
        /**
         * input time.
         * used to call the {@link Calendar#set(int, int, int, int, int, int)} method to set the time.
         */
        public final Map<String, Integer> inputTimeMap;
        /**
         * input time zone.
         * used to call the {@link Calendar#setTimeZone(TimeZone)} to set the time zone.
         * the time zone offset has direct impact on the result.
         */
        public final TimeZone inputTimeZone;
        /**
         * direct input of a time string (with or without time zone).
         *
         * @see {@link DateTimeType#valueOf(String)}
         *      if this is set, the {@link ParameterSet#inputTimeMap} and {@link ParameterSet#inputTimeZone} are ignored
         */
        public final String inputTimeString;
        /**
         * the expected result of the test.
         * golden rule:
         * should always return the input time minus or plus the offset of the given time zone.
         * if no time zone is specified or the time zone in the {@link ParameterSet#inputTimeString} is wrong, then the
         * {@link ParameterSet#defaultTimeZone} is used.
         */
        public final String expectedResult;

        /**
         * create a parameter set with {@link ParameterSet#inputTimeMap} and {@link ParameterSet#inputTimeZone}
         * parameters.
         *
         * @param defaultTimeZone
         * @param inputTimeMap
         * @param inputTimeZone
         * @param expectedResult
         */
        public ParameterSet(TimeZone defaultTimeZone, Map<String, Integer> inputTimeMap, TimeZone inputTimeZone,
                String expectedResult) {
            this.defaultTimeZone = defaultTimeZone;
            this.inputTimeMap = inputTimeMap;
            this.inputTimeZone = inputTimeZone;
            this.inputTimeString = null;
            this.expectedResult = expectedResult;
        }

        /**
         * create a parameter set with {@link ParameterSet#inputTimeString} parameter.
         *
         * @param defaultTimeZone
         * @param inputTimeString
         * @param expectedResult
         */
        public ParameterSet(TimeZone defaultTimeZone, String inputTimeString, String expectedResult) {
            this.defaultTimeZone = defaultTimeZone;
            this.inputTimeMap = null;
            this.inputTimeZone = null;
            this.inputTimeString = inputTimeString;
            this.expectedResult = expectedResult;
        }

    }

    /**
     * Test parameter maps collection.
     *
     * @return collection
     */
    @Parameters
    public static Collection<Object[]> parameters() {
        // for simplicity we use always the same input time.
        return Arrays.asList(new Object[][] {
                { new ParameterSet(TimeZone.getTimeZone("UTC"), initTimeMap(), TimeZone.getTimeZone("UTC"),
                        "2014-03-30T10:58:47.033+0000") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), initTimeMap(), TimeZone.getTimeZone("CET"),
                        "2014-03-30T08:58:47.033+0000") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), "2014-03-30T10:58:47UTC",
                        "2014-03-30T10:58:47.000+0000") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), initTimeMap(), TimeZone.getTimeZone("UTC"),
                        "2014-03-30T12:58:47.033+0200") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), initTimeMap(), TimeZone.getTimeZone("CET"),
                        "2014-03-30T10:58:47.033+0200") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), "2014-03-30T10:58:47CET",
                        "2014-03-30T10:58:47.000+0200") },
                { new ParameterSet(TimeZone.getTimeZone("GMT+5"), "2014-03-30T10:58:47.000Z",
                        "2014-03-30T15:58:47.000+0500") },
                { new ParameterSet(TimeZone.getTimeZone("GMT"), initTimeMap(), TimeZone.getTimeZone("GMT"),
                        "2014-03-30T10:58:47.033+0000") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), initTimeMap(), TimeZone.getTimeZone("+02:00"),
                        "2014-03-30T12:58:47.033+0200") },
                { new ParameterSet(TimeZone.getTimeZone("GMT+2"), initTimeMap(), TimeZone.getTimeZone("GML"),
                        "2014-03-30T12:58:47.033+0200") },
                { new ParameterSet(TimeZone.getTimeZone("GMT-2"), initTimeMap(), TimeZone.getTimeZone("GMT+3"),
                        "2014-03-30T05:58:47.033-0200") },
                { new ParameterSet(TimeZone.getTimeZone("GMT-2"), initTimeMap(), TimeZone.getTimeZone("GMT-4"),
                        "2014-03-30T12:58:47.033-0200") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), "10:58:47", "1970-01-01T10:58:47.000+0000") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), "10:58", "1970-01-01T10:58:00.000+0000") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), "10:58:47CET", "1970-01-01T10:58:47.000+0100") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), "10:58CET", "1970-01-01T10:58:00.000+0100") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), "2014-03-30", "2014-03-30T00:00:00.000+0000") }, });
    }

    private static Map<String, Integer> initTimeMap() {
        Map<String, Integer> inputTimeMap = new HashMap<String, Integer>();
        inputTimeMap.put("year", 2014);
        inputTimeMap.put("month", 2);
        inputTimeMap.put("date", 30);
        inputTimeMap.put("hourOfDay", 10);
        inputTimeMap.put("minute", 58);
        inputTimeMap.put("second", 47);
        inputTimeMap.put("milliseconds", 33);
        return inputTimeMap;
    }

    private final ParameterSet parameterSet;

    /**
     * setup Test class with current parameter map.
     *
     * @param parameterMap
     *            parameter map
     */
    public DateTimeTypeTest(ParameterSet parameterSet) {
        this.parameterSet = parameterSet;
    }

    @Test
    public void serializationTest() {
        DateTimeType dt = new DateTimeType(Calendar.getInstance());
        assertTrue(dt.equals(new DateTimeType(dt.toString())));
    }

    @Test
    public void serializationTestZoned() {
        ZonedDateTime zoned = ZonedDateTime.now();
        DateTimeType dt = new DateTimeType(zoned);
        DateTimeType sdt = new DateTimeType(dt.toFullString());
        assertEquals(dt.getZonedDateTime(), sdt.getZonedDateTime());
    }

    @Test
    public void equalityTest() {
        DateTimeType dt1 = new DateTimeType(Calendar.getInstance());
        DateTimeType dt2 = DateTimeType.valueOf(dt1.toFullString());

        assertTrue(dt1.toString().equals(dt2.toString()));
        assertTrue(dt1.equals(dt2));
        assertTrue(dt1.getCalendar().equals(dt2.getCalendar()));

        assertTrue(dt1.equals(dt2));
    }

    @Test
    public void equalityTestZoned() {
        ZonedDateTime zoned = ZonedDateTime.now();
        DateTimeType dt1 = new DateTimeType(zoned);
        DateTimeType dt2 = DateTimeType.valueOf(dt1.toFullString());

        assertTrue(dt1.toString().equals(dt2.toFullString()));
        assertTrue(dt1.getZonedDateTime().equals(dt2.getZonedDateTime()));
        assertTrue(dt1.equals(dt2));
    }

    @Test
    public void createDate() {
        String inputTimeString;

        // set default time zone
        TimeZone.setDefault(parameterSet.defaultTimeZone);

        // get formatted time string
        if (parameterSet.inputTimeString == null) {
            final Calendar calendar = Calendar.getInstance(parameterSet.inputTimeZone);
            calendar.set(parameterSet.inputTimeMap.get("year"), parameterSet.inputTimeMap.get("month"),
                    parameterSet.inputTimeMap.get("date"), parameterSet.inputTimeMap.get("hourOfDay"),
                    parameterSet.inputTimeMap.get("minute"), parameterSet.inputTimeMap.get("second"));
            calendar.set(Calendar.MILLISECOND, parameterSet.inputTimeMap.get("milliseconds"));
            inputTimeString = new SimpleDateFormat(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS).format(calendar.getTime());
        } else {
            inputTimeString = parameterSet.inputTimeString;
        }
        DateTimeType dt = DateTimeType.valueOf(inputTimeString);
        if (parameterSet.inputTimeZone == null) {
            dt = new DateTimeType(dt.getZonedDateTime().withZoneSameInstant(TimeZone.getDefault().toZoneId()));
        }
        // Test
        assertEquals(parameterSet.expectedResult, dt.toString());
    }

    @Test
    public void createZonedDate() {
        String inputTimeString;

        // set default time zone
        TimeZone.setDefault(parameterSet.defaultTimeZone);

        // get formatted time string
        if (parameterSet.inputTimeString == null) {
            int durationInNano = (int) TimeUnit.NANOSECONDS.convert(parameterSet.inputTimeMap.get("milliseconds"),
                    TimeUnit.MILLISECONDS);

            LocalDateTime dateTime = LocalDateTime.of(parameterSet.inputTimeMap.get("year"),
                    parameterSet.inputTimeMap.get("month") + 1, parameterSet.inputTimeMap.get("date"),
                    parameterSet.inputTimeMap.get("hourOfDay"), parameterSet.inputTimeMap.get("minute"),
                    parameterSet.inputTimeMap.get("second"), durationInNano);
            ZonedDateTime zonedDate = ZonedDateTime.of(dateTime, parameterSet.inputTimeZone.toZoneId()).toInstant()
                    .atZone(parameterSet.defaultTimeZone.toZoneId());
            inputTimeString = zonedDate.format((DateTimeFormatter.ofPattern(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS)));
        } else {
            inputTimeString = parameterSet.inputTimeString;
        }
        DateTimeType dt = new DateTimeType(inputTimeString);
        if (parameterSet.inputTimeZone == null) {
            dt = new DateTimeType(dt.getZonedDateTime().withZoneSameInstant(TimeZone.getDefault().toZoneId()));
        }
        // Test
        assertEquals(parameterSet.expectedResult, dt.toString());
    }
}
