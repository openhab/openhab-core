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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

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
 */
@NonNullByDefault
public class DateTimeType implements PrimitiveType, State, Command {

    // external format patterns for output
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_PATTERN_WITH_TZ = "yyyy-MM-dd'T'HH:mm:ssz";
    // this pattern returns the time zone in RFC822 format
    public static final String DATE_PATTERN_WITH_TZ_AND_MS = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DATE_PATTERN_WITH_TZ_AND_MS_GENERAL = "yyyy-MM-dd'T'HH:mm:ss.SSSz";
    public static final String DATE_PATTERN_WITH_TZ_AND_MS_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

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

    // internal patterns for formatting
    private static final String DATE_FORMAT_PATTERN_WITH_TZ_RFC = "yyyy-MM-dd'T'HH:mm[:ss[.SSSSSSSSS]]Z";
    private static final DateTimeFormatter FORMATTER_TZ_RFC = DateTimeFormatter
            .ofPattern(DATE_FORMAT_PATTERN_WITH_TZ_RFC);

    private ZonedDateTime zonedDateTime;

    /**
     * @deprecated The constructor uses Calendar object hence it doesn't store time zone. A new constructor is
     *             available. Use {@link #DateTimeType(ZonedDateTime)} instead.
     *
     * @param calendar The Calendar object containing the time stamp.
     */
    @Deprecated
    public DateTimeType(Calendar calendar) {
        this.zonedDateTime = ZonedDateTime.ofInstant(calendar.toInstant(), TimeZone.getDefault().toZoneId())
                .withFixedOffsetZone();
    }

    public DateTimeType() {
        this(ZonedDateTime.now());
    }

    public DateTimeType(ZonedDateTime zoned) {
        this.zonedDateTime = ZonedDateTime.from(zoned).withFixedOffsetZone();
    }

    public DateTimeType(String zonedValue) {
        ZonedDateTime date = null;

        try {
            // direct parsing (date and time)
            try {
                date = parse(zonedValue);
            } catch (DateTimeParseException fullDtException) {
                // time only
                try {
                    date = parse("1970-01-01T" + zonedValue);
                } catch (DateTimeParseException timeOnlyException) {
                    try {
                        long epoch = Double.valueOf(zonedValue).longValue();
                        int length = (int) (Math.log10(epoch >= 0 ? epoch : epoch * -1) + 1);
                        Instant i;
                        // Assume that below 12 digits we're in seconds
                        if (length < 12) {
                            i = Instant.ofEpochSecond(epoch);
                        } else {
                            i = Instant.ofEpochMilli(epoch);
                        }
                        date = ZonedDateTime.ofInstant(i, ZoneOffset.UTC);
                    } catch (NumberFormatException notANumberException) {
                        // date only
                        if (zonedValue.length() == 10) {
                            date = parse(zonedValue + "T00:00:00");
                        } else {
                            date = parse(zonedValue.substring(0, 10) + "T00:00:00" + zonedValue.substring(10));
                        }
                    }
                }
            }
        } catch (DateTimeParseException invalidFormatException) {
            throw new IllegalArgumentException(zonedValue + " is not in a valid format.", invalidFormatException);
        }

        zonedDateTime = date.withFixedOffsetZone();
    }

    /**
     * @deprecated The method is deprecated. You can use {@link #getZonedDateTime()} instead.
     */
    @Deprecated
    public Calendar getCalendar() {
        return GregorianCalendar.from(zonedDateTime);
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public static DateTimeType valueOf(String value) {
        return new DateTimeType(value);
    }

    @Override
    public String format(@Nullable String pattern) {
        if (pattern == null) {
            return DateTimeFormatter.ofPattern(DATE_PATTERN).format(zonedDateTime);
        }

        return String.format(pattern, zonedDateTime);
    }

    public String format(Locale locale, String pattern) {
        return String.format(locale, pattern, zonedDateTime);
    }

    /**
     * Create a {@link DateTimeType} being the translation of the current object to the locale time zone
     *
     * @return a {@link DateTimeType} translated to the locale time zone
     * @throws DateTimeException if the converted zone ID has an invalid format or the result exceeds the supported date
     *             range
     * @throws ZoneRulesException if the converted zone region ID cannot be found
     */
    public DateTimeType toLocaleZone() throws DateTimeException, ZoneRulesException {
        return toZone(ZoneId.systemDefault());
    }

    /**
     * Create a {@link DateTimeType} being the translation of the current object to a given zone
     *
     * @param zone the target zone as a string
     * @return a {@link DateTimeType} translated to the given zone
     * @throws DateTimeException if the zone has an invalid format or the result exceeds the supported date range
     * @throws ZoneRulesException if the zone is a region ID that cannot be found
     */
    public DateTimeType toZone(String zone) throws DateTimeException, ZoneRulesException {
        return toZone(ZoneId.of(zone));
    }

    /**
     * Create a {@link DateTimeType} being the translation of the current object to a given zone
     *
     * @param zoneId the target {@link ZoneId}
     * @return a {@link DateTimeType} translated to the given zone
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public DateTimeType toZone(ZoneId zoneId) throws DateTimeException {
        return new DateTimeType(zonedDateTime.withZoneSameInstant(zoneId));
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        String formatted = zonedDateTime.format(FORMATTER_TZ_RFC);
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
        result = prime * result + getZonedDateTime().hashCode();
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
        return zonedDateTime.compareTo(other.zonedDateTime) == 0;
    }

    private ZonedDateTime parse(String value) throws DateTimeParseException {
        ZonedDateTime date = null;
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

        return date;
    }
}
