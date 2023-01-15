/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Erdoan Hadzhiyusein - Refactored to use ZonedDateTime
 * @author Jan N. Klug - add ability to use time or date only
 * @author Wouter Born - increase parsing and formatting precision
 * @author Laurent Garnier - added methods toLocaleZone and toZone
 * @author Gaël L'hopital - added ability to use second and milliseconds unix time
 * @author Jimmy Tanagra - implement Comparable
 * @author Jacob Laursen - Refactored to use {@link Instant} internally
 */
@NonNullByDefault
public class DateTimeType implements PrimitiveType, State, Command, Comparable<DateTimeType> {

    // external format patterns for output
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_PATTERN_WITH_TZ = "yyyy-MM-dd'T'HH:mm:ssz";
    // this pattern returns the time zone in RFC822 format
    public static final String DATE_PATTERN_WITH_TZ_AND_MS = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DATE_PATTERN_WITH_TZ_AND_MS_GENERAL = "yyyy-MM-dd'T'HH:mm:ss.SSSz";
    public static final String DATE_PATTERN_WITH_TZ_AND_MS_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    // serialization of Date, Java 17 compatible format
    public static final String DATE_PATTERN_JSON_COMPAT = "MMM d, yyyy, h:mm:ss aaa";

    // internal patterns for parsing
    private static final String DATE_PARSE_PATTERN_WITHOUT_TZ = "yyyy-MM-dd'T'HH:mm"
            + "[:ss[.SSSSSSSSS][.SSSSSSSS][.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]]";
    private static final String DATE_PARSE_PATTERN_WITH_TZ = DATE_PARSE_PATTERN_WITHOUT_TZ + "z";
    private static final String DATE_PARSE_PATTERN_WITH_TZ_RFC = DATE_PARSE_PATTERN_WITHOUT_TZ + "Z";
    private static final String DATE_PARSE_PATTERN_WITH_TZ_ISO = DATE_PARSE_PATTERN_WITHOUT_TZ + "X";

    private static final DateTimeFormatter PARSER = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITHOUT_TZ);
    private static final DateTimeFormatter PARSER_TZ = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITH_TZ);
    private static final DateTimeFormatter PARSER_TZ_RFC = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITH_TZ_RFC);
    private static final DateTimeFormatter PARSER_TZ_ISO = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITH_TZ_ISO);

    private static final Pattern DATE_PARSE_PATTERN_WITH_SPACE = Pattern
            .compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}.*");

    // internal patterns for formatting
    private static final String DATE_FORMAT_PATTERN_WITH_TZ_RFC = "yyyy-MM-dd'T'HH:mm[:ss[.SSSSSSSSS]]Z";
    private static final DateTimeFormatter FORMATTER_TZ_RFC = DateTimeFormatter
            .ofPattern(DATE_FORMAT_PATTERN_WITH_TZ_RFC);

    private Instant instant;

    /**
     * Creates a new {@link DateTimeType} representing the current
     * instant from the system clock.
     */
    public DateTimeType() {
        this(Instant.now());
    }

    /**
     * Creates a new {@link DateTimeType} with the given value.
     *
     * @param instant
     */
    public DateTimeType(Instant instant) {
        this.instant = instant;
    }

    /**
     * Creates a new {@link DateTimeType} with the given value.
     * The time-zone information will be discarded, only the
     * resulting {@link Instant} is preserved.
     *
     * @param zoned
     */
    public DateTimeType(ZonedDateTime zoned) {
        instant = zoned.toInstant();
    }

    public DateTimeType(String zonedValue) {
        try {
            // direct parsing (date and time)
            try {
                if (DATE_PARSE_PATTERN_WITH_SPACE.matcher(zonedValue).matches()) {
                    instant = parse(zonedValue.substring(0, 10) + "T" + zonedValue.substring(11));
                } else {
                    instant = parse(zonedValue);
                }
            } catch (DateTimeParseException fullDtException) {
                // time only
                try {
                    instant = parse("1970-01-01T" + zonedValue);
                } catch (DateTimeParseException timeOnlyException) {
                    try {
                        long epoch = Double.valueOf(zonedValue).longValue();
                        int length = (int) (Math.log10(epoch >= 0 ? epoch : epoch * -1) + 1);
                        // Assume that below 12 digits we're in seconds
                        if (length < 12) {
                            instant = Instant.ofEpochSecond(epoch);
                        } else {
                            instant = Instant.ofEpochMilli(epoch);
                        }
                    } catch (NumberFormatException notANumberException) {
                        // date only
                        if (zonedValue.length() == 10) {
                            instant = parse(zonedValue + "T00:00:00");
                        } else {
                            instant = parse(zonedValue.substring(0, 10) + "T00:00:00" + zonedValue.substring(10));
                        }
                    }
                }
            }
        } catch (DateTimeParseException invalidFormatException) {
            throw new IllegalArgumentException(zonedValue + " is not in a valid format.", invalidFormatException);
        }
    }

    /**
     * Get object represented as a {@link ZonedDateTime} with system
     * default time-zone applied
     *
     * @return a {@link ZonedDateTime} representation of the object
     */
    public ZonedDateTime getZonedDateTime() {
        return instant.atZone(ZoneId.systemDefault());
    }

    /**
     * Get the {@link Instant} value of the object
     *
     * @return the {@link Instant} value of the object
     */
    public Instant getInstant() {
        return instant;
    }

    public static DateTimeType valueOf(String value) {
        return new DateTimeType(value);
    }

    @Override
    public String format(@Nullable String pattern) {
        if (pattern == null) {
            return DateTimeFormatter.ofPattern(DATE_PATTERN).format(getZonedDateTime());
        }

        return String.format(pattern, getZonedDateTime());
    }

    public String format(Locale locale, String pattern) {
        return String.format(locale, pattern, getZonedDateTime());
    }

    /**
     * @deprecated
     *             Create a {@link DateTimeType} being the translation of the current object to the locale time zone
     *
     * @return a {@link DateTimeType} translated to the locale time zone
     * @throws DateTimeException if the converted zone ID has an invalid format or the result exceeds the supported date
     *             range
     * @throws ZoneRulesException if the converted zone region ID cannot be found
     */
    @Deprecated
    public DateTimeType toLocaleZone() throws DateTimeException, ZoneRulesException {
        return new DateTimeType(instant);
    }

    /**
     * @deprecated
     *             Create a {@link DateTimeType} being the translation of the current object to a given zone
     *
     * @param zone the target zone as a string
     * @return a {@link DateTimeType} translated to the given zone
     * @throws DateTimeException if the zone has an invalid format or the result exceeds the supported date range
     * @throws ZoneRulesException if the zone is a region ID that cannot be found
     */
    @Deprecated
    public DateTimeType toZone(String zone) throws DateTimeException, ZoneRulesException {
        return new DateTimeType(instant);
    }

    /**
     * @deprecated
     *             Create a {@link DateTimeType} being the translation of the current object to a given zone
     *
     * @param zoneId the target {@link ZoneId}
     * @return a {@link DateTimeType} translated to the given zone
     * @throws DateTimeException if the result exceeds the supported date range
     */
    @Deprecated
    public DateTimeType toZone(ZoneId zoneId) throws DateTimeException {
        return new DateTimeType(instant);
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        String formatted = getZonedDateTime().format(FORMATTER_TZ_RFC);
        if (formatted.contains(".")) {
            String sign = "";
            if (formatted.contains("+")) {
                sign = "+";
            } else if (formatted.contains("-")) {
                sign = "-";
            }
            if (!sign.isEmpty()) {
                // the formatted string contains 9 fraction-of-second digits
                // truncate at most 2 trailing groups of 000s
                return formatted.replace("000" + sign, sign).replace("000" + sign, sign);
            }
        }
        return formatted;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + instant.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DateTimeType other = (DateTimeType) obj;
        return instant.compareTo(other.instant) == 0;
    }

    @Override
    public int compareTo(DateTimeType o) {
        return instant.compareTo(o.getInstant());
    }

    private Instant parse(String value) throws DateTimeParseException {
        ZonedDateTime date;
        try {
            date = ZonedDateTime.parse(value, PARSER_TZ_RFC);
        } catch (DateTimeParseException tzMsRfcException) {
            try {
                date = ZonedDateTime.parse(value, PARSER_TZ_ISO);
            } catch (DateTimeParseException tzMsIsoException) {
                try {
                    date = ZonedDateTime.parse(value, PARSER_TZ);
                } catch (DateTimeParseException tzException) {
                    LocalDateTime localDateTime = LocalDateTime.parse(value, PARSER);
                    date = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
                }
            }
        }

        return date.toInstant();
    }
}
