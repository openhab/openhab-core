/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Gaël L'hopital - Added Timezone and Milliseconds
 * @author Erdoan Hadzhiyusein - Added ZonedDateTime tests
 * @author Laurent Garnier - Enhanced tests
 * @author Gaël L'hopital - added ability to use second and milliseconds unix time
 */
@NonNullByDefault
public class DateTimeTypeTest {

    @Nullable
    private static TimeZone initialTimeZone;

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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ParameterSet [").append("defaultTimeZone=").append(defaultTimeZone.toZoneId()).append(", ");
            if (inputTimeMap != null) {
                sb.append("inputTimeMap=").append(inputTimeMap).append(", ");
            }
            TimeZone zone = initialTimeZone;
            if (zone != null) {
                sb.append("inputTimeZone=").append(zone.toZoneId()).append(", ");
            }
            if (inputTimeString != null) {
                sb.append("inputTimeString=").append(inputTimeString).append(", ");
            }
            sb.append("expectedResult=").append(expectedResult).append(", ").append("expectedResultLocalTZ=")
                    .append(expectedResultLocalTZ).append(", ");
            if (locale != null) {
                sb.append("locale=").append(locale).append(", ");
            }
            if (pattern != null) {
                sb.append("pattern=").append(pattern).append(", ");
            }
            sb.append("expectedFormattedResult=").append(expectedFormattedResult).append("]");
            return sb.toString();
        }
    }

    private record LocalDateTestCase(LocalDateTime ldt, @Nullable ZoneId zone, DateTimeType expectedResult) {
    }

    @BeforeAll
    public static void setUpClass() {
        initialTimeZone = TimeZone.getDefault();
    }

    @AfterAll
    @SuppressWarnings("PMD.SetDefaultTimeZone")
    public static void tearDownClass() {
        // Set the default time zone to its initial value.
        TimeZone.setDefault(initialTimeZone);
    }

    /**
     * Test parameter maps collection.
     *
     * @return collection
     */
    public static Collection<Object[]> parameters() {
        // for simplicity we use always the same input time.
        return List.of(new Object[][] {
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
        return Map.ofEntries(entry("year", 2014), entry("month", 2), entry("date", 30), entry("hourOfDay", 10),
                entry("minute", 58), entry("second", 47), entry("milliseconds", 33));
    }

    @Test
    public void serializationTest() {
        ZonedDateTime zoned = ZonedDateTime.now(ZoneId.systemDefault());
        DateTimeType dt = new DateTimeType(zoned);
        DateTimeType sdt = new DateTimeType(dt.toFullString());
        assertEquals(dt.getZonedDateTime(), sdt.getZonedDateTime());
    }

    @Test
    public void equalityTest() {
        ZonedDateTime zoned = ZonedDateTime.now(ZoneId.systemDefault());
        DateTimeType dt1 = new DateTimeType(zoned);
        DateTimeType dt2 = DateTimeType.valueOf(dt1.toFullString());

        assertTrue(dt1.toString().equals(dt2.toString()));
        assertTrue(dt1.getZonedDateTime().equals(dt2.getZonedDateTime()));
        assertTrue(dt1.getInstant().equals(dt2.getInstant()));
        assertTrue(dt1.equals(dt2));
    }

    @Test
    public void comparabilityTest() {
        ZonedDateTime zoned = ZonedDateTime.now(ZoneId.systemDefault());
        DateTimeType dt1 = new DateTimeType(zoned);
        DateTimeType dt2 = new DateTimeType(zoned.plusSeconds(1));
        DateTimeType dt3 = new DateTimeType(zoned.minusSeconds(1));

        assertTrue(dt1.compareTo(dt2) < 0);
        assertTrue(dt1.compareTo(dt3) > 0);
        assertTrue(dt1.compareTo(dt1) == 0);
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "2024-09-05T15:30:00Z", //
            "2024-09-05 15:30Z", //
            "2024-09-05 15:30+0000", //
            "2024-09-05 16:30+0100", //
            "2024-09-05T17:30:00.000+0200", //
            "2024-09-05T17:30:00.000+02:00", //
            "2024-09-05T17:30+02:00", //
            "2024-09-05T17:30+02:00[Europe/Berlin]" //
    })
    public void parserTest(String input) {
        Instant instantReference = Instant.parse("2024-09-05T15:30:00Z");

        Instant instant = new DateTimeType(input).getInstant();
        assertThat(instant, is(instantReference));
    }

    @Test
    public void zonedParsingTest() {
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
    public void instantParsingTest() {
        DateTimeType dt1 = new DateTimeType("2019-06-12T17:30:00Z");
        DateTimeType dt2 = new DateTimeType("2019-06-12T17:30:00+0000");
        DateTimeType dt3 = new DateTimeType("2019-06-12T19:30:00+0200");
        assertThat(dt1, is(dt2));

        Instant i1 = dt1.getInstant();
        Instant i2 = dt2.getInstant();
        Instant i3 = dt3.getInstant();
        assertThat(i1, is(i2));
        assertThat(i1, is(i3));
    }

    @Test
    public void epochTest() {
        DateTimeType zdtEpoch = new DateTimeType("1970-01-01T00:00:00+0000");
        DateTimeType zdtStandard = new DateTimeType("2014-03-30T10:58:47+0000");
        DateTimeType epochSecond = new DateTimeType("0").toZone(ZoneOffset.UTC);
        DateTimeType epochStandard = new DateTimeType("1396177127").toZone(ZoneOffset.UTC);
        DateTimeType epochMilliseconds = new DateTimeType("000000000000").toZone(ZoneOffset.UTC);
        assertThat(epochSecond, is(zdtEpoch));
        assertThat(epochMilliseconds, is(zdtEpoch));
        assertThat(epochStandard, is(zdtStandard));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    @SuppressWarnings("PMD.SetDefaultTimeZone")
    public void createDate(ParameterSet parameterSet) {
        TimeZone.setDefault(parameterSet.defaultTimeZone);
        // get DateTimeType from the current parameter
        DateTimeType dt1;
        DateTimeType dt2;
        DateTimeType dt3;
        Map<String, Integer> inputTimeMap = parameterSet.inputTimeMap;
        TimeZone inputTimeZone = parameterSet.inputTimeZone;
        String inputTimeString = parameterSet.inputTimeString;
        if (inputTimeMap != null && inputTimeZone != null) {
            LocalDateTime dateTime = createLocalDateTimeFromInput(inputTimeMap);
            ZonedDateTime zonedDate = ZonedDateTime.of(dateTime, inputTimeZone.toZoneId());
            dt1 = new DateTimeType(zonedDate);
            dt3 = new DateTimeType(
                    zonedDate.format((DateTimeFormatter.ofPattern(DateTimeType.DATE_PATTERN_WITH_TZ_AND_MS))))
                    .toZone(inputTimeZone.toZoneId());
            zonedDate = ZonedDateTime.of(dateTime, inputTimeZone.toZoneId()).toInstant()
                    .atZone(parameterSet.defaultTimeZone.toZoneId());
            dt2 = new DateTimeType(zonedDate);
        } else if (inputTimeString != null) {
            dt1 = new DateTimeType(inputTimeString);
            dt2 = new DateTimeType(dt1.getZonedDateTime().withZoneSameInstant(ZoneId.systemDefault()));
            dt3 = new DateTimeType(dt1.getZonedDateTime(), dt1.isZoneAuthoritative());
        } else {
            throw new DateTimeException("Invalid inputs in parameter set");
        }
        // Test
        assertEquals(dt1.toString(), dt1.toString());
        assertEquals(dt2.toString(), dt2.toString());
        assertEquals(parameterSet.expectedResult, dt1.toString());
        assertEquals(parameterSet.expectedResultLocalTZ, dt2.toString());
        assertEquals(parameterSet.expectedResult, dt3.toString());
        assertEquals(dt1, dt3);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    @SuppressWarnings({ "PMD.SetDefaultTimeZone", "deprecation" })
    public void formattingTest(ParameterSet parameterSet) {
        TimeZone.setDefault(parameterSet.defaultTimeZone);
        DateTimeType dt = createDateTimeType(parameterSet);
        Locale locale = parameterSet.locale;
        String pattern = parameterSet.pattern;
        if (locale != null && pattern != null) {
            assertEquals(parameterSet.expectedFormattedResult, dt.format(locale, pattern));
        } else {
            assertEquals(parameterSet.expectedFormattedResult, dt.format(pattern));
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    @SuppressWarnings("PMD.SetDefaultTimeZone")
    public void changingZoneTest(ParameterSet parameterSet) {
        TimeZone.setDefault(parameterSet.defaultTimeZone);
        DateTimeType dt = createDateTimeType(parameterSet);
        DateTimeType dt2 = dt.toZone(parameterSet.defaultTimeZone.toZoneId());
        assertEquals(parameterSet.expectedResultLocalTZ, dt2.toString());
        dt2 = dt.toZone(parameterSet.defaultTimeZone.toZoneId());
        assertEquals(parameterSet.expectedResultLocalTZ, dt2.toString());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    @SuppressWarnings("PMD.SetDefaultTimeZone")
    public void changingZoneThrowsExceptionTest(ParameterSet parameterSet) {
        TimeZone.setDefault(parameterSet.defaultTimeZone);
        DateTimeType dt = createDateTimeType(parameterSet);
        assertThrows(DateTimeException.class, () -> dt.toZone("XXX"));
    }

    @SuppressWarnings("deprecation")
    @ParameterizedTest
    @MethodSource("provideTestCasesForFormatWithZone")
    void formatWithZone(String instant, @Nullable String pattern, ZoneId zoneId, String expected) {
        DateTimeType dt = new DateTimeType(Instant.parse(instant));
        String actual = dt.format(pattern, zoneId);
        assertThat(actual, is(equalTo(expected)));
    }

    public static final List<LocalDateTestCase> LOCAL_DATE_TEST_CASES = List.of(
            new LocalDateTestCase(LocalDateTime.parse("2002-04-08T13:50:02"), null,
                    DateTimeType.valueOf("?2002-04-08T13:50:02PST")),
            new LocalDateTestCase(LocalDateTime.parse("2002-04-08T13:50:02"), ZoneOffset.ofHours(4),
                    DateTimeType.valueOf("2002-04-08T13:50:02+0400")),
            new LocalDateTestCase(LocalDateTime.parse("2002-04-08T13:50:02"), ZoneId.of("Asia/Kathmandu"),
                    DateTimeType.valueOf("2002-04-08T13:50:02+05:45[Asia/Kathmandu]")));

    @ParameterizedTest
    @FieldSource("LOCAL_DATE_TEST_CASES")
    @SuppressWarnings("PMD.SetDefaultTimeZone")
    public void localDateTimeConstructorTest(LocalDateTestCase testCase) {
        if (testCase.zone == null) {
            TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        }
        DateTimeType dt = new DateTimeType(testCase.ldt, testCase.zone);
        assertEquals(testCase.expectedResult.toFullString(), dt.toFullString());
        assertEquals(testCase.expectedResult, dt);
    }

    @Test
    public void variousGettersTest() {
        DateTimeType dt = DateTimeType.valueOf("2002-04-08T13:50:02+0400");
        LocalDateTime ldt = LocalDateTime.parse("2002-04-08T13:50:02");
        assertEquals(ldt.atOffset(ZoneOffset.ofHours(4)), dt.getOffsetDateTime());
        assertEquals(ldt.atZone(ZoneOffset.ofHours(4)), dt.getZonedDateTime());
        assertEquals(ldt.atZone(ZoneOffset.ofHours(4)).withZoneSameInstant(ZoneId.of("Asia/Kathmandu")),
                dt.getZonedDateTime(ZoneId.of("Asia/Kathmandu")));
        assertEquals(ldt.atZone(ZoneOffset.ofHours(4)).toInstant(), dt.getInstant());
        assertEquals(ZoneOffset.ofHours(4), dt.getZoneId());
        assertEquals(ZoneOffset.ofHours(4), dt.getZoneOffset());
        assertTrue(dt.isZoneAuthoritative());

        dt = DateTimeType.valueOf("?2002-04-08T13:50:02+0400");
        ldt = LocalDateTime.parse("2002-04-08T13:50:02");
        assertEquals(ldt.atOffset(ZoneOffset.ofHours(4)), dt.getOffsetDateTime());
        assertEquals(ldt.atZone(ZoneOffset.ofHours(4)), dt.getZonedDateTime());
        assertEquals(ldt.atZone(ZoneOffset.ofHours(4)).withZoneSameInstant(ZoneId.of("Asia/Kathmandu")),
                dt.getZonedDateTime(ZoneId.of("Asia/Kathmandu")));
        assertEquals(ldt.atZone(ZoneOffset.ofHours(4)).toInstant(), dt.getInstant());
        assertEquals(ZoneOffset.ofHours(4), dt.getZoneId());
        assertEquals(ZoneOffset.ofHours(4), dt.getZoneOffset());
        assertFalse(dt.isZoneAuthoritative());

        dt = DateTimeType.valueOf("1989-11-19T17:32:49+01:00[Europe/Berlin]");
        ldt = LocalDateTime.parse("1989-11-19T17:32:49");
        assertEquals(ldt.atOffset(ZoneOffset.ofHours(1)), dt.getOffsetDateTime());
        assertEquals(ldt.atZone(ZoneId.of("Europe/Berlin")), dt.getZonedDateTime());
        assertEquals(ldt.atZone(ZoneId.of("Europe/Berlin")).withZoneSameInstant(ZoneId.of("Asia/Kathmandu")),
                dt.getZonedDateTime(ZoneId.of("Asia/Kathmandu")));
        assertEquals(ldt.atZone(ZoneId.of("Europe/Berlin")).toInstant(), dt.getInstant());
        assertEquals(ZoneId.of("Europe/Berlin"), dt.getZoneId());
        assertEquals(ZoneOffset.ofHours(1), dt.getZoneOffset());
        assertTrue(dt.isZoneAuthoritative());

        dt = dt.plus(6, ChronoUnit.MONTHS);
        ldt = ldt.plus(6, ChronoUnit.MONTHS);
        assertEquals(ldt.atOffset(ZoneOffset.ofHours(2)), dt.getOffsetDateTime());
        assertEquals(ldt.atZone(ZoneId.of("Europe/Berlin")), dt.getZonedDateTime());
        assertEquals(ldt.atZone(ZoneId.of("Europe/Berlin")).withZoneSameInstant(ZoneId.of("Asia/Kathmandu")),
                dt.getZonedDateTime(ZoneId.of("Asia/Kathmandu")));
        assertEquals(ldt.atZone(ZoneId.of("Europe/Berlin")).toInstant(), dt.getInstant());
        assertEquals(ZoneId.of("Europe/Berlin"), dt.getZoneId());
        assertEquals(ZoneOffset.ofHours(2), dt.getZoneOffset());
        assertTrue(dt.isZoneAuthoritative());
    }

    @Test
    public void variousFunctionsTest() {
        DateTimeType dt = DateTimeType.valueOf("2002-04-08T13:50:02+0400");
        assertEquals(DateTimeType.valueOf("2002-04-08T13:50:00+0400"), dt.truncatedTo(ChronoUnit.MINUTES));
        assertEquals(DateTimeType.valueOf("2002-04-08T00:00:00+0400"), dt.truncatedTo(ChronoUnit.DAYS));
        DateTimeType dt2 = dt.plus(Duration.ofHours(5));
        assertEquals(5, dt.until(dt2, ChronoUnit.HOURS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        assertFalse(dt.isBefore(dt));
        assertFalse(dt.isAfter(dt));
        dt2 = dt.minus(-3, ChronoUnit.MONTHS);
        assertEquals(3, dt.until(dt2, ChronoUnit.MONTHS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        dt2 = dt.minus(5, ChronoUnit.MONTHS);
        assertEquals(-151, dt.until(dt2, ChronoUnit.DAYS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        dt2 = dt.plus(5, ChronoUnit.MONTHS);
        assertEquals(3672, dt.until(dt2, ChronoUnit.HOURS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        Instant inst = Instant.parse("2002-11-11T16:50:00Z");
        assertEquals(312899, dt.until(inst, ChronoUnit.MINUTES));
        assertTrue(dt.isBefore(inst));
        assertFalse(dt2.isAfter(inst));
        dt2 = dt.minus(63, ChronoUnit.MINUTES);
        assertEquals(-3780, dt.until(dt2, ChronoUnit.SECONDS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        dt2 = dt.minus(Period.of(1, 8, 13));
        assertEquals(-14904, dt.until(dt2, ChronoUnit.HOURS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        ZonedDateTime zdt = ZonedDateTime.parse("2002-11-11T16:50:00-06:00");
        assertTrue(dt.isBefore(zdt));
        assertFalse(dt2.isAfter(zdt));
        assertSame(dt, dt.toFixedOffset());
        assertSame(dt, dt.toZone(ZoneOffset.ofHours(4)));
        assertSame(dt, dt.toOffset(ZoneOffset.ofHours(4)));
        assertEquals(new DateTimeType(Instant.parse("2002-04-08T09:50:02Z"), ZoneId.of("CET")).toFullString(),
                dt.toZone("CET").toFullString());
        assertEquals(DateTimeType.valueOf("2002-04-08T11:50:02+0200"), dt.toOffset(ZoneOffset.ofHours(2)));

        dt = DateTimeType.valueOf("?2002-04-08T13:50:02+0400");
        assertEquals(DateTimeType.valueOf("?2002-04-08T13:50:00+0400"), dt.truncatedTo(ChronoUnit.MINUTES));
        assertEquals(DateTimeType.valueOf("?2002-04-08T00:00:00+0400"), dt.truncatedTo(ChronoUnit.DAYS));
        dt2 = dt.plus(Duration.ofHours(5));
        assertEquals(5, dt.until(dt2, ChronoUnit.HOURS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        assertFalse(dt.isBefore(dt));
        assertFalse(dt.isAfter(dt));
        dt2 = dt.minus(-3, ChronoUnit.MONTHS);
        assertEquals(3, dt.until(dt2, ChronoUnit.MONTHS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        dt2 = dt.minus(5, ChronoUnit.MONTHS);
        assertEquals(-151, dt.until(dt2, ChronoUnit.DAYS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        dt2 = dt.plus(5, ChronoUnit.MONTHS);
        assertEquals(3672, dt.until(dt2, ChronoUnit.HOURS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        inst = Instant.parse("2002-11-11T16:50:00Z");
        assertEquals(312899, dt.until(inst, ChronoUnit.MINUTES));
        assertTrue(dt.isBefore(inst));
        assertFalse(dt2.isAfter(inst));
        dt2 = dt.minus(63, ChronoUnit.MINUTES);
        assertEquals(-3780, dt.until(dt2, ChronoUnit.SECONDS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        dt2 = dt.minus(Period.of(1, 8, 13));
        assertEquals(-14904, dt.until(dt2, ChronoUnit.HOURS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        zdt = ZonedDateTime.parse("2002-11-11T16:50:00-06:00");
        assertTrue(dt.isBefore(zdt));
        assertFalse(dt2.isAfter(zdt));
        assertEquals(0, dt.compareTo(dt.toFixedOffset()));
        assertEquals(0, dt.compareTo(dt.toZone(ZoneOffset.ofHours(4))));
        assertEquals(0, dt.compareTo(dt.toOffset(ZoneOffset.ofHours(4))));
        assertEquals(new DateTimeType(Instant.parse("2002-04-08T09:50:02Z"), ZoneId.of("CET")).toFullString(),
                dt.toZone("CET").toFullString());
        assertEquals(DateTimeType.valueOf("2002-04-08T11:50:02+0200"), dt.toOffset(ZoneOffset.ofHours(2)));

        dt = DateTimeType.valueOf("1989-11-19T17:32:49+01:00[Europe/Berlin]");
        assertEquals(DateTimeType.valueOf("1989-11-19T17:32:00+01:00[Europe/Berlin]"),
                dt.truncatedTo(ChronoUnit.MINUTES));
        assertEquals(DateTimeType.valueOf("1989-11-19T00:00:00+01:00[Europe/Berlin]"), dt.truncatedTo(ChronoUnit.DAYS));
        dt2 = dt.plus(Duration.ofHours(5));
        assertEquals(5, dt.until(dt2, ChronoUnit.HOURS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        assertFalse(dt.isBefore(dt));
        assertFalse(dt.isAfter(dt));
        dt2 = dt.minus(-3, ChronoUnit.MONTHS);
        assertEquals(3, dt.until(dt2, ChronoUnit.MONTHS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        dt2 = dt.minus(5, ChronoUnit.MONTHS);
        assertEquals(-153, dt.until(dt2, ChronoUnit.DAYS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        dt2 = dt.plus(5, ChronoUnit.MONTHS);
        assertEquals(3623, dt.until(dt2, ChronoUnit.HOURS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        inst = Instant.parse("1989-11-11T16:50:00Z");
        assertEquals(-11502, dt.until(inst, ChronoUnit.MINUTES));
        assertFalse(dt.isBefore(inst));
        assertTrue(dt2.isAfter(inst));
        dt2 = dt.minus(63, ChronoUnit.MINUTES);
        assertEquals(-3780, dt.until(dt2, ChronoUnit.SECONDS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        dt2 = dt.minus(Period.of(1, 8, 13));
        assertEquals(-14952, dt.until(dt2, ChronoUnit.HOURS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        zdt = ZonedDateTime.parse("1989-11-11T16:50:00-06:00");
        assertFalse(dt.isBefore(zdt));
        assertFalse(dt2.isAfter(zdt));
        assertEquals(DateTimeType.valueOf("1989-11-19T17:32:49+01:00"), dt.toFixedOffset());
        assertSame(dt, dt.toZone(ZoneId.of("Europe/Berlin")));
        assertEquals(0, dt.compareTo(dt.toOffset(ZoneOffset.ofHours(1))));
        assertEquals(new DateTimeType(Instant.parse("1989-11-19T16:32:49Z"), ZoneId.of("CET")).toFullString(),
                dt.toZone("CET").toFullString());
        assertEquals(DateTimeType.valueOf("1989-11-19T18:32:49+0200"), dt.toOffset(ZoneOffset.ofHours(2)));

        dt = dt.plus(6, ChronoUnit.MONTHS);
        assertEquals(DateTimeType.valueOf("1990-05-19T17:32:00.000+02:00[Europe/Berlin]"),
                dt.truncatedTo(ChronoUnit.MINUTES));
        assertEquals(DateTimeType.valueOf("1990-05-19T00:00:00.000+02:00[Europe/Berlin]"),
                dt.truncatedTo(ChronoUnit.DAYS));
        dt2 = dt.plus(Duration.ofHours(5));
        assertEquals(5, dt.until(dt2, ChronoUnit.HOURS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        assertFalse(dt.isBefore(dt));
        assertFalse(dt.isAfter(dt));
        dt2 = dt.minus(-3, ChronoUnit.MONTHS);
        assertEquals(3, dt.until(dt2, ChronoUnit.MONTHS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        dt2 = dt.minus(5, ChronoUnit.MONTHS);
        assertEquals(-151, dt.until(dt2, ChronoUnit.DAYS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        dt2 = dt.plus(5, ChronoUnit.MONTHS);
        assertEquals(3673, dt.until(dt2, ChronoUnit.HOURS));
        assertTrue(dt.isBefore(dt2));
        assertTrue(dt2.isAfter(dt));
        inst = Instant.parse("1990-11-11T16:50:00Z");
        assertEquals(253517, dt.until(inst, ChronoUnit.MINUTES));
        assertTrue(dt.isBefore(inst));
        assertFalse(dt2.isAfter(inst));
        dt2 = dt.minus(63, ChronoUnit.MINUTES);
        assertEquals(-3780, dt.until(dt2, ChronoUnit.SECONDS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        dt2 = dt.minus(Period.of(1, 8, 13));
        assertEquals(-14880, dt.until(dt2, ChronoUnit.HOURS));
        assertFalse(dt.isBefore(dt2));
        assertFalse(dt2.isAfter(dt));
        zdt = ZonedDateTime.parse("2002-11-11T16:50:00-06:00");
        assertTrue(dt.isBefore(zdt));
        assertFalse(dt2.isAfter(zdt));
        assertEquals(DateTimeType.valueOf("1990-05-19T17:32:49.000+0200"), dt.toFixedOffset());
        assertSame(dt, dt.toZone(ZoneId.of("Europe/Berlin")));
        assertEquals(0, dt.compareTo(dt.toOffset(ZoneOffset.ofHours(2))));
        assertEquals(new DateTimeType(Instant.parse("1990-05-19T15:32:49Z"), ZoneId.of("CET")).toFullString(),
                dt.toZone("CET").toFullString());
        assertEquals(DateTimeType.valueOf("1990-05-19T16:32:49+01:00"), dt.toOffset(ZoneOffset.ofHours(1)));
    }

    private static Stream<Arguments> provideTestCasesForFormatWithZone() {
        return Stream.of( //
                Arguments.of("2024-11-11T20:39:01Z", null, ZoneId.of("UTC"), "2024-11-11T20:39:01"), //
                Arguments.of("2024-11-11T20:39:01Z", "%1$td.%1$tm.%1$tY %1$tH:%1$tM", ZoneId.of("Europe/Paris"),
                        "11.11.2024 21:39"), //
                Arguments.of("2024-11-11T20:39:01Z", "%1$td.%1$tm.%1$tY %1$tH:%1$tM", ZoneId.of("US/Alaska"),
                        "11.11.2024 11:39") //
        );
    }

    private DateTimeType createDateTimeType(ParameterSet parameterSet) throws DateTimeException {
        Map<String, Integer> inputTimeMap = parameterSet.inputTimeMap;
        TimeZone inputTimeZone = parameterSet.inputTimeZone;
        String inputTimeString = parameterSet.inputTimeString;
        if (inputTimeMap != null && inputTimeZone != null) {
            LocalDateTime dateTime = createLocalDateTimeFromInput(inputTimeMap);
            ZonedDateTime zonedDate = ZonedDateTime.of(dateTime, inputTimeZone.toZoneId());
            return new DateTimeType(zonedDate);
        } else if (inputTimeString != null) {
            return new DateTimeType(inputTimeString);
        }
        throw new DateTimeException("Invalid inputs in parameter set");
    }

    private LocalDateTime createLocalDateTimeFromInput(Map<String, Integer> inputTimeMap) {
        Integer year = Objects.requireNonNull(inputTimeMap.get("year"));
        Integer month = Objects.requireNonNull(inputTimeMap.get("month"));
        Integer dayOfMonth = Objects.requireNonNull(inputTimeMap.get("date"));
        Integer hourOfDay = Objects.requireNonNull(inputTimeMap.get("hourOfDay"));
        Integer minute = Objects.requireNonNull(inputTimeMap.get("minute"));
        Integer second = Objects.requireNonNull(inputTimeMap.get("second"));
        Integer milliseconds = Objects.requireNonNull(inputTimeMap.get("milliseconds"));
        int durationInNano = (int) TimeUnit.NANOSECONDS.convert(milliseconds, TimeUnit.MILLISECONDS);

        return LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute, second, durationInNano);
    }

    @ParameterizedTest
    @MethodSource("provideTestCasesForToFullStringWithZone")
    void toFullStringWithZone(String instant, ZoneId zoneId, String expected) {
        DateTimeType dt = new DateTimeType(Instant.parse(instant));
        String actual = dt.toString(zoneId);
        assertThat(actual, is(equalTo(expected)));
    }

    private static Stream<Arguments> provideTestCasesForToFullStringWithZone() {
        return Stream.of( //
                Arguments.of("2024-11-11T20:39:00Z", ZoneId.of("UTC"), "2024-11-11T20:39:00.000+0000"), //
                Arguments.of("2024-11-11T20:39:00.000000000Z", ZoneId.of("UTC"), "2024-11-11T20:39:00.000+0000"), //
                Arguments.of("2024-11-11T20:39:00.000000001Z", ZoneId.of("UTC"), "2024-11-11T20:39:00.000000001+0000"), //
                Arguments.of("2024-11-11T20:39:00.123000000Z", ZoneId.of("UTC"), "2024-11-11T20:39:00.123+0000"), //
                Arguments.of("2024-11-11T20:39:00.123456000Z", ZoneId.of("UTC"), "2024-11-11T20:39:00.123456+0000"), //
                Arguments.of("2024-11-11T20:39:00.123456789Z", ZoneId.of("UTC"), "2024-11-11T20:39:00.123456789+0000"), //
                Arguments.of("2024-11-11T20:39:00.123Z", ZoneId.of("Europe/Paris"), "2024-11-11T21:39:00.123+0100"), //
                Arguments.of("2024-11-11T04:59:59.999Z", ZoneId.of("America/New_York"), "2024-11-10T23:59:59.999-0500") //
        );
    }
}
