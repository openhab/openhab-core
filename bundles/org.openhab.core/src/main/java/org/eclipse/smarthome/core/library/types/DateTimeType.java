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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.PrimitiveType;
import org.eclipse.smarthome.core.types.State;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Erdoan Hadzhiyusein - Refactored to use ZonedDateTime
 * @author Jan N. Klug - add ability to use time or date only
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

    // internal format patterns for parsing
    private static final String DATE_PARSE_PATTERN_WITHOUT_TZ = "yyyy-MM-dd'T'HH:mm[:ss[.SSS]]";
    private static final String DATE_PARSE_PATTERN_WITH_TZ = "yyyy-MM-dd'T'HH:mm[:ss[.SSS]]z";
    private static final String DATE_PARSE_PATTERN_WITH_TZ_RFC = "yyyy-MM-dd'T'HH:mm[:ss[.SSS]]Z";
    private static final String DATE_PARSE_PATTERN_WITH_TZ_ISO = "yyyy-MM-dd'T'HH:mm[:ss[.SSS]]X";

    private ZonedDateTime zonedDateTime;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITHOUT_TZ);
    private final DateTimeFormatter formatterTz = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITH_TZ);
    private final DateTimeFormatter formatterTzRFC = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITH_TZ_RFC);
    private final DateTimeFormatter formatterTzIso = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITH_TZ_ISO);

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
                    // date only
                    if (zonedValue.length() == 10) {
                        date = parse(zonedValue + "T00:00:00");
                    } else {
                        date = parse(zonedValue.substring(0, 10) + "T00:00:00" + zonedValue.substring(10));
                    }
                }
            }
        } catch (DateTimeParseException invalidFormatException) {
            throw new IllegalArgumentException(zonedValue + " is not in a valid format.", invalidFormatException);
        }

        zonedDateTime = date;
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

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return zonedDateTime.format(formatterTzRFC);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getZonedDateTime() == null) ? 0 : getZonedDateTime().hashCode());
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
        if (zonedDateTime == null) {
            if (other.zonedDateTime != null) {
                return false;
            }
        } else if (zonedDateTime.compareTo(other.zonedDateTime) != 0) {
            return false;
        }
        return true;
    }

    private ZonedDateTime parse(String value) throws DateTimeParseException {
        ZonedDateTime date = null;
        try {
            date = ZonedDateTime.parse(value, formatterTzRFC);
        } catch (DateTimeParseException tzMsRfcException) {
            try {
                date = ZonedDateTime.parse(value, formatterTzIso);
            } catch (DateTimeParseException tzMsIsoException) {
                try {
                    date = ZonedDateTime.parse(value, formatterTz);
                } catch (DateTimeParseException tzException) {
                    LocalDateTime localDateTime = LocalDateTime.parse(value, formatter);
                    date = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
                }
            }
        }

        return date;
    }

}
