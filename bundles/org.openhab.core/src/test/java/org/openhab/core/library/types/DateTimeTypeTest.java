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
package org.openhab.core.library.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Gaël L'hopital - Added Timezone and Milliseconds
 * @author Erdoan Hadzhiyusein - Added ZonedDateTime tests
 * @author Laurent Garnier - Enhanced tests
 * @author Gaël L'hopital - added ability to use second and milliseconds unix time
 */
@NonNullByDefault
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
         * used to call the {@link LocalDateTime#of(int, int, int, int, int, int, int)}method to set the time.
         */
        public final @Nullable Map<String, Integer> inputTimeMap;
        /**
         * input time zone.
         * used to call the {@link ZonedDateTime#of(LocalDateTime, ZoneId)} to set the time zone.
         * the time zone offset has direct impact on the result.
         */
        public final @Nullable TimeZone inputTimeZone;
        /**
         * direct input of a time string (with or without time zone).
         *
         * @see {@link DateTimeType#valueOf(String)}
         *      if this is set, the {@link ParameterSet#inputTimeMap} and {@link ParameterSet#inputTimeZone} are ignored
         */
        public final @Nullable String inputTimeString;
        /**
         * the expected result of the test without any additional translation to the time zone specified in the
         * {@link ParameterSet#defaultTimeZone}.
         * golden rule:
         * should always return the input time minus or plus the offset of the given time zone.
         * if no time zone is specified or the time zone in the {@link ParameterSet#inputTimeString} is wrong, then the
         * {@link ParameterSet#defaultTimeZone} is used.
         */
        public final String expectedResult;
        /**
         * the expected result of the test with an additional translation to the time zone specified in the
         * {@link ParameterSet#defaultTimeZone}.
         * golden rule:
         * should always return the input time minus or plus the offset of the time zone specified in the
         * {@link ParameterSet#defaultTimeZone}.
         * if no time zone is specified or the time zone in the {@link ParameterSet#inputTimeString} is wrong, then the
         * {@link ParameterSet#defaultTimeZone} is used to first build the date.
         */
        public final String expectedResultLocalTZ;
        /**
         * the locale parameter to be used to test the method {@link DateTimeType#format(Locale, String)}.
         */
        public final @Nullable Locale locale;
        /**
         * the pattern parameter to be used to test the method {@link DateTimeType#format(Locale, String)}
         * or {@link DateTimeType#format(String)}.
         */
        public final @Nullable String pattern;
        /**
         * the expected result when testing the method {@link DateTimeType#format(Locale, String)}
         * or {@link DateTimeType#format(String)}.
         */
        public final String expectedFormattedResult;

        /**
         * create a parameter set with {@link ParameterSet#inputTimeMap} and {@link ParameterSet#inputTimeZone}
         * parameters.
         *
         * @param defaultTimeZone
         * @param inputTimeMap
         * @param inputTimeZone
         * @param expectedResult
         * @param expectedResultLocalTZ
         */
        public ParameterSet(TimeZone defaultTimeZone, Map<String, Integer> inputTimeMap, TimeZone inputTimeZone,
                String expectedResult, String expectedResultLocalTZ) {
            this(defaultTimeZone, inputTimeMap, inputTimeZone, null, expectedResult, expectedResultLocalTZ, null, null,
                    expectedResult.substring(0, 19));
        }

        /**
         * create a parameter set with {@link ParameterSet#inputTimeString} parameter.
         *
         * @param defaultTimeZone
         * @param inputTimeString
         * @param expectedResult
         * @param expectedResultLocalTZ
         */
        public ParameterSet(TimeZone defaultTimeZone, String inputTimeString, String expectedResult,
                String expectedResultLocalTZ) {
            this(defaultTimeZone, null, null, inputTimeString, expectedResult, expectedResultLocalTZ, null, null,
                    expectedResult.substring(0, 19));
        }

        /**
         * create a parameter set with either {@link ParameterSet#inputTimeMap} and {@link ParameterSet#inputTimeZone}
         * parameters or {@link ParameterSet#inputTimeString} parameter. Non default values can be set for testing
         * the format method.
         *
         * @param defaultTimeZone
         * @param inputTimeMap
         * @param inputTimeZone
         * @param inputTimeString
         * @param expectedResult
         * @param expectedResultLocalTZ
         * @param locale
         * @param pattern
         * @param expectedFormattedResult
         */
        public ParameterSet(TimeZone defaultTimeZone, @Nullable Map<String, Integer> inputTimeMap,
                @Nullable TimeZone inputTimeZone, @Nullable String inputTimeString, String expectedResult,
                String expectedResultLocalTZ, @Nullable Locale locale, @Nullable String pattern,
                String expectedFormattedResult) {
            this.defaultTimeZone = defaultTimeZone;
            this.inputTimeMap = inputTimeMap;
            this.inputTimeZone = inputTimeZone;
            this.inputTimeString = inputTimeString;
            this.expectedResult = expectedResult;
            this.expectedResultLocalTZ = expectedResultLocalTZ;
            this.locale = locale;
            this.pattern = pattern;
            this.expectedFormattedResult = expectedFormattedResult;
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
                        "2014-03-30T10:58:47.033+0000", "2014-03-30T10:58:47.033+0000") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), initTimeMap(), TimeZone.getTimeZone("CET"),
                        "2014-03-30T10:58:47.033+0200", "2014-03-30T08:58:47.033+0000") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), "2014-03-30T10:58:47.23",
                        "2014-03-30T10:58:47.230+0000", "2014-03-30T10:58:47.230+0000") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), "2014-03-30T10:58:47UTC",
                        "2014-03-30T10:58:47.000+0000", "2014-03-30T10:58:47.000+0000") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), initTimeMap(), TimeZone.getTimeZone("UTC"),
                        "2014-03-30T10:58:47.033+0000", "2014-03-30T12:58:47.033+0200") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), initTimeMap(), TimeZone.getTimeZone("CET"),
                        "2014-03-30T10:58:47.033+0200", "2014-03-30T10:58:47.033+0200") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), "2014-03-30T10:58:47CET",
                        "2014-03-30T10:58:47.000+0200", "2014-03-30T10:58:47.000+0200") },
                { new ParameterSet(TimeZone.getTimeZone("GMT+5"), "2014-03-30T10:58:47.000Z",
                        "2014-03-30T10:58:47.000+0000", "2014-03-30T15:58:47.000+0500") },
                { new ParameterSet(TimeZone.getTimeZone("GMT+2"), null, null, "2014-03-30T10:58:47",
                        "2014-03-30T10:58:47.000+0200", "2014-03-30T10:58:47.000+0200", null,
                        "%1$td.%1$tm.%1$tY %1$tH:%1$tM", "30.03.2014 10:58") },
                { new ParameterSet(TimeZone.getTimeZone("GMT"), initTimeMap(), TimeZone.getTimeZone("GMT"),
                        "2014-03-30T10:58:47.033+0000", "2014-03-30T10:58:47.033+0000") },
                // Parameter set with an invalid time zone id as input, leading to GMT being considered
                { new ParameterSet(TimeZone.getTimeZone("CET"), initTimeMap(), TimeZone.getTimeZone("+02:00"),
                        "2014-03-30T10:58:47.033+0000", "2014-03-30T12:58:47.033+0200") },
                // Parameter set with an invalid time zone id as input, leading to GMT being considered
                { new ParameterSet(TimeZone.getTimeZone("GMT+2"), initTimeMap(), TimeZone.getTimeZone("GML"),
                        "2014-03-30T10:58:47.033+0000", "2014-03-30T12:58:47.033+0200") },
                { new ParameterSet(TimeZone.getTimeZone("GMT-2"), initTimeMap(), TimeZone.getTimeZone("GMT+3"), null,
                        "2014-03-30T10:58:47.033+0300", "2014-03-30T05:58:47.033-0200", Locale.GERMAN,
                        "%1$tA %1$td.%1$tm.%1$tY %1$tH:%1$tM", "Sonntag 30.03.2014 10:58") },
                { new ParameterSet(TimeZone.getTimeZone("GMT-2"), initTimeMap(), TimeZone.getTimeZone("GMT-4"),
                        "2014-03-30T10:58:47.033-0400", "2014-03-30T12:58:47.033-0200") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), "10:58:47", "1970-01-01T10:58:47.000+0000",
                        "1970-01-01T10:58:47.000+0000") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), "10:58", "1970-01-01T10:58:00.000+0000",
                        "1970-01-01T10:58:00.000+0000") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), "10:58:47CET", "1970-01-01T10:58:47.000+0100",
                        "1970-01-01T10:58:47.000+0100") },
                { new ParameterSet(TimeZone.getTimeZone("CET"), "10:58CET", "1970-01-01T10:58:00.000+0100",
                        "1970-01-01T10:58:00.000+0100") },
                { new ParameterSet(TimeZone.getTimeZone("UTC"), "2014-03-30", "2014-03-30T00:00:00.000+0000",
                        "2014-03-30T00:00:00.000+0000") }, });
    }

    private static Map<String, Integer> initTimeMap() {
        Map<String, Integer> inputTimeMap = new HashMap<>();
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
     * @param parameterMap parameter map
     */
    public DateTimeTypeTest(ParameterSet parameterSet) {
        this.parameterSet = parameterSet;
    }

    @Before
    public void setUp() {
        // set default time zone
        TimeZone.setDefault(parameterSet.defaultTimeZone);
    }

    @Test
    public void serializationTest() {
        ZonedDateTime zoned = ZonedDateTime.now();
        DateTimeType dt = new DateTimeType(zoned);
        DateTimeType sdt = new DateTimeType(dt.toFullString());
        assertEquals(dt.getZonedDateTime(), sdt.getZonedDateTime());
    }

    @Test
    public void equalityTest() {
        ZonedDateTime zoned = ZonedDateTime.now();
        DateTimeType dt1 = new DateTimeType(zoned);
        DateTimeType dt2 = DateTimeType.valueOf(dt1.toFullString());

        assertTrue(dt1.toString().equals(dt2.toFullString()));
        assertTrue(dt1.getZonedDateTime().equals(dt2.getZonedDateTime()));
        assertTrue(dt1.equals(dt2));
    }

    @Test
    public void parsingTest() {
        DateTimeType dt1 = new DateTimeType("2019-06-12T17:30:00Z");
        DateTimeType dt2 = new DateTimeType("2019-06-12T17:30:00+0000");
        DateTimeType dt3 = new DateTimeType("2019-06-12T19:30:00+0200");
        assertThat(dt1, is(dt2));

        ZonedDateTime zdt1 = dt1.getZonedDateTime();
        ZonedDateTime zdt2 = dt2.getZonedDateTime();
        ZonedDateTime zdt3 = dt3.getZonedDateTime();
        assertThat(zdt1.getZone(), is(zdt2.getZone()));
        assertThat(zdt1, is(zdt2));
        assertThat(zdt1, is(zdt3.withZoneSameInstant(zdt1.getZone())));
        assertThat(zdt2, is(zdt3.withZoneSameInstant(zdt2.getZone())));
    }

    @Test
    public void epochTest() {
        DateTimeType zdtEpoch = new DateTimeType("1970-01-01T00:00:00+0000");
        DateTimeType zdtStandard = new DateTimeType("2014-03-30T10:58:47+0000");
        DateTimeType epochSecond = new DateTimeType("0");
        DateTimeType epochStandard = new DateTimeType("1396177127");
        DateTimeType epochMilliseconds = new DateTimeType("000000000000");
        assertThat(epochSecond, is(zdtEpoch));
        assertThat(epochMilliseconds, is(zdtEpoch));
        assertThat(epochStandard, is(zdtStandard));
    }

    @Test
    public void createDate() {
        // get DateTimeType from the current parameter
        DateTimeType dt1;
        DateTimeType dt2;
        DateTimeType dt3;
        Map<String, Integer> inputTimeMap = parameterSet.inputTimeMap;
        TimeZone inputTimeZone = parameterSet.inputTimeZone;
        String inputTimeString = parameterSet.inputTimeString;
        if (inputTimeMap != null && inputTimeZone != null) {
            int durationInNano = (int) TimeUnit.NANOSECONDS.convert(inputTimeMap.get("milliseconds"),
                    TimeUnit.MILLISECONDS);

            LocalDateTime dateTime = LocalDateTime.of(inputTimeMap.get("year"), inputTimeMap.get("month") + 1,
                    inputTimeMap.get("date"), inputTimeMap.get("hourOfDay"), inputTimeMap.get("minute"),
                    inputTimeMap.get("second"), durationInNano);
            ZonedDateTime zonedDate = ZonedDateTime.of(dateTime, inputTimeZone.toZoneId());
            dt1 = new DateTimeType(zonedDate);
            dt3 = new DateTimeType(
                    zonedDate.format((DateTimeFormatter.ofPattern(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS))));
            zonedDate = ZonedDateTime.of(dateTime, inputTimeZone.toZoneId()).toInstant()
                    .atZone(parameterSet.defaultTimeZone.toZoneId());
            dt2 = new DateTimeType(zonedDate);
        } else if (inputTimeString != null) {
            dt1 = new DateTimeType(inputTimeString);
            dt2 = new DateTimeType(dt1.getZonedDateTime().withZoneSameInstant(ZoneId.systemDefault()));
            dt3 = new DateTimeType(dt1.getZonedDateTime());
        } else {
            throw new DateTimeException("Invalid inputs in parameter set");
        }
        // Test
        assertEquals(dt1.toFullString(), dt1.toString());
        assertEquals(dt2.toFullString(), dt2.toString());
        assertEquals(parameterSet.expectedResult, dt1.toString());
        assertEquals(parameterSet.expectedResultLocalTZ, dt2.toString());
        assertEquals(parameterSet.expectedResult, dt3.toString());
        assertEquals(dt1, dt3);
    }

    @Test
    public void formattingTest() {
        DateTimeType dt = createDateTimeType();
        Locale locale = parameterSet.locale;
        String pattern = parameterSet.pattern;
        if (locale != null && pattern != null) {
            assertEquals(parameterSet.expectedFormattedResult, dt.format(locale, pattern));
        } else {
            assertEquals(parameterSet.expectedFormattedResult, dt.format(pattern));
        }
    }

    @Test
    public void changingZoneTest() {
        DateTimeType dt = createDateTimeType();
        DateTimeType dt2 = dt.toLocaleZone();
        assertEquals(parameterSet.expectedResultLocalTZ, dt2.toFullString());
        dt2 = dt.toZone(parameterSet.defaultTimeZone.toZoneId());
        assertEquals(parameterSet.expectedResultLocalTZ, dt2.toFullString());
    }

    @Test(expected = DateTimeException.class)
    public void changingZoneThrowsExceptionTest() {
        DateTimeType dt = createDateTimeType();
        dt.toZone("XXX");
    }

    private DateTimeType createDateTimeType() throws DateTimeException {
        Map<String, Integer> inputTimeMap = parameterSet.inputTimeMap;
        TimeZone inputTimeZone = parameterSet.inputTimeZone;
        String inputTimeString = parameterSet.inputTimeString;
        if (inputTimeMap != null && inputTimeZone != null) {
            int durationInNano = (int) TimeUnit.NANOSECONDS.convert(inputTimeMap.get("milliseconds"),
                    TimeUnit.MILLISECONDS);

            LocalDateTime dateTime = LocalDateTime.of(inputTimeMap.get("year"), inputTimeMap.get("month") + 1,
                    inputTimeMap.get("date"), inputTimeMap.get("hourOfDay"), inputTimeMap.get("minute"),
                    inputTimeMap.get("second"), durationInNano);
            ZonedDateTime zonedDate = ZonedDateTime.of(dateTime, inputTimeZone.toZoneId());
            return new DateTimeType(zonedDate);
        } else if (inputTimeString != null) {
            return new DateTimeType(inputTimeString);
        }
        throw new DateTimeException("Invalid inputs in parameter set");
    }
}
