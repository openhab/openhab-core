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

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.zone.ZoneRulesException;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 * A primitive immutable type that holds a date, time and timezone using the Christian/Gregorian calendar.
 *
 * @implNote This type has the concept of <i>authoritative</i> timezone. An authoritative timezone is the originating
 *           timezone for the date and time data. If the originating timezone is unknown, an arbitrary timezone can be
 *           used, in which case the timezone is non-authoritative. A non-authoritative {@link DateTimeType} will be
 *           converted to the configured timezone, and made authoritative, before being published on the event bus.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Erdoan Hadzhiyusein - Refactored to use ZonedDateTime
 * @author Jan N. Klug - add ability to use time or date only
 * @author Wouter Born - increase parsing and formatting precision
 * @author Laurent Garnier - added methods toLocaleZone and toZone
 * @author Gaël L'hopital - added ability to use second and milliseconds unix time
 * @author Jimmy Tanagra - implement Comparable
 * @author Jacob Laursen - Refactored to use {@link Instant} internally
 * @author Ravi Nadahar - Resurrected timezone
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

    private final Instant instant;
    private final ZoneOffset zoneOffset;
    private final ZoneId zoneId;
    private final boolean authoritativeZone;

    /**
     * Create a new {@link DateTimeType} representing the current instant from the system clock with the JVM default
     * timezone as a non-authoritative timezone.
     *
     * @throws DateTimeException If the JVM default timezone has an invalid format.
     * @throws ZoneRulesException If the JVM default timezone can't be found or if no rules are available for that ID.
     */
    public DateTimeType() throws DateTimeException, ZoneRulesException {
        this(Instant.now());
    }

    /**
     * Create a new {@link DateTimeType} representing the specified instant with the JVM default timezone as a
     * non-authoritative timezone.
     *
     * @param instant the moment in time.
     * @throws DateTimeException If the JVM default timezone has an invalid format.
     * @throws ZoneRulesException If the JVM default timezone can't be found or if no rules are available for that ID.
     */
    public DateTimeType(Instant instant) throws DateTimeException, ZoneRulesException {
        this.instant = instant;
        ZoneId zoneId = ZoneId.systemDefault();
        this.zoneId = zoneId;
        if (zoneId instanceof ZoneOffset zoneOffset) {
            this.zoneOffset = zoneOffset;
        } else {
            this.zoneOffset = zoneId.getRules().getOffset(instant);
        }
        this.authoritativeZone = false;
    }

    /**
     * Create a new {@link DateTimeType} representing the specified instant and an authoritative timezone or offset.
     *
     * @param instant the moment in time.
     * @param zoneId the {@link ZoneId} or {@link ZoneOffset}.
     * @throws ZoneRulesException If no rules are available for the zone ID.
     */
    public DateTimeType(Instant instant, ZoneId zoneId) throws ZoneRulesException {
        this(instant, zoneId, true);
    }

    /**
     * Create a new {@link DateTimeType} representing the specified instant and timezone or offset. Whether the
     * timezone is considered authoritative is decided by the specified parameter.
     *
     * @param instant the moment in time.
     * @param zoneId the {@link ZoneId} or {@link ZoneOffset}.
     * @param authoritativeZone {@code true} if the timezone should be considered authoritative, {@code false} if it
     *            shouldn't.
     * @throws ZoneRulesException If no rules are available for the zone ID.
     */
    public DateTimeType(Instant instant, ZoneId zoneId, boolean authoritativeZone) throws ZoneRulesException {
        this.instant = instant;
        ZoneOffset resolvedOffset;
        if (zoneId instanceof ZoneOffset offset) {
            resolvedOffset = offset;
        } else {
            resolvedOffset = zoneId.getRules().getOffset(instant);
        }

        this.zoneId = zoneId;
        this.zoneOffset = resolvedOffset;
        this.authoritativeZone = authoritativeZone;
    }

    /**
     * Create a new {@link DateTimeType} representing the instant dictated by the specified local date and time in
     * combination with the specified or default timezone or offset.
     * <p>
     * <b>Note:</b> The resulting {@link DateTimeType} has an authoritative timezone of offset if {@code ZoneId} is
     * specified.
     * If {@code ZoneId} is {@code null}, the JVM default timezone will be used to interpret the local date and time,
     * and the timezone will be non-authoritative.
     *
     * @param localDateTime the local date and time without timezone information.
     * @param zoneId the {@link ZoneId} or {@link ZoneOffset}.
     * @throws DateTimeException If {@code zoneId} is {@code null} and the JVM default timezone has an invalid format.
     * @throws ZoneRulesException If {@code zoneId} is {@code null} and the JVM default timezone can't be found, or if
     *             no rules are available for the zone ID.
     */
    public DateTimeType(LocalDateTime localDateTime, @Nullable ZoneId zoneId)
            throws DateTimeException, ZoneRulesException {
        ZoneId resolvedZoneId;
        ZoneOffset resolvedOffset;
        boolean resolvedAuthoritative;
        if (zoneId instanceof ZoneOffset offset) {
            resolvedZoneId = zoneId;
            resolvedOffset = offset;
            resolvedAuthoritative = true;
        } else if (zoneId == null) {
            resolvedZoneId = ZoneId.systemDefault();
            if (resolvedZoneId instanceof ZoneOffset offset) {
                resolvedOffset = offset;
            } else {
                resolvedOffset = resolvedZoneId.getRules().getOffset(localDateTime);
            }
            resolvedAuthoritative = false;
        } else {
            resolvedZoneId = zoneId;
            resolvedOffset = zoneId.getRules().getOffset(localDateTime);
            resolvedAuthoritative = true;
        }

        this.instant = localDateTime.toInstant(resolvedOffset);
        this.zoneId = resolvedZoneId;
        this.zoneOffset = resolvedOffset;
        this.authoritativeZone = resolvedAuthoritative;
    }

    /**
     * Create a new {@link DateTimeType} representing the instant and authoritative timezone provided by the specified
     * {@link ZonedDateTime}.
     *
     * @param zoned the moment in time and timezone.
     */
    public DateTimeType(ZonedDateTime zoned) {
        this(zoned, true);
    }

    /**
     * Create a new {@link DateTimeType} representing the instant and timezone provided by the specified
     * {@link ZonedDateTime}. Whether the timezone is considered authoritative is decided by the specified parameter.
     *
     * @param zoned the moment in time and timezone.
     * @param authoritativeZone {@code true} if the timezone should be considered authoritative, {@code false} if it
     *            shouldn't.
     */
    public DateTimeType(ZonedDateTime zoned, boolean authoritativeZone) {
        this.instant = zoned.toInstant();
        this.zoneId = zoned.getZone();
        this.zoneOffset = zoned.getOffset();
        this.authoritativeZone = authoritativeZone;
    }

    /**
     * Create a new {@link DateTimeType} representing the date, time and potentially timezone or offset parsed from the
     * provided string.
     * <p>
     * If a timezone is parsed, the timezone is considered authoritative, unless the string starts with a {@code ?}. If
     * a timezone isn't parsed, the JVM default timezone is used, and the timezone is considered non-authoritative.
     * <p>
     * For details about the supported formats, see {@link #parseDateTime(String)}.
     *
     * @param value the string to parse.
     * @throws DateTimeException If no timezone could be parsed and the JVM default zone ID has an invalid format.
     * @throws IllegalArgumentException If the specified value can't be parsed.
     * @throws ZoneRulesException If no timezone could be parsed and the JVM default zone ID cannot be found.
     */
    public DateTimeType(String value) throws DateTimeException, IllegalArgumentException, ZoneRulesException {
        ParsedDateTimeResult result = parseDateTime(value);
        instant = result.zdt.toInstant();
        zoneId = result.zdt.getZone();
        zoneOffset = result.zdt.getOffset();
        authoritativeZone = result.authoritativeZone;
    }

    /**
     * Get this date and time represented as a {@link ZonedDateTime}.
     * <p>
     * <b>Note:</b> Since `ZonedDateTime` has no authoritative timezone concept, the current timezone will be used
     * whether this {@link DateTimeType} is authoritative or not.
     *
     * @return The {@link ZonedDateTime} representation.
     * @throws DateTimeException If the result exceeds the supported range.
     */
    public ZonedDateTime getZonedDateTime() throws DateTimeException {
        return getZonedDateTime(zoneId);
    }

    /**
     * Get this date and time represented as a {@link ZonedDateTime} with the the provided timezone applied.
     *
     * @return The {@link ZonedDateTime} representation.
     * @throws DateTimeException If the result exceeds the supported range.
     */
    public ZonedDateTime getZonedDateTime(ZoneId zoneId) throws DateTimeException {
        return instant.atZone(zoneId);
    }

    /**
     * Get this date and time represented as a {@link OffsetDateTime}.
     * <p>
     * <b>Note:</b> Since `OffsetDateTime` has no authoritative timezone concept, the current offset will be used
     * whether this {@link DateTimeType} is authoritative or not.
     *
     * @return The {@link OffsetDateTime} representation.
     * @throws DateTimeException If the result exceeds the supported range.
     */
    public OffsetDateTime getOffsetDateTime() throws DateTimeException {
        return OffsetDateTime.ofInstant(instant, zoneOffset);
    }

    /**
     * Get the date and time represented in UTC as an {@link Instant}.
     *
     * @return The UTC-aligned {@link Instant}.
     */
    public Instant getInstant() {
        return instant;
    }

    /**
     * @return The {@link ZoneId} of this {@link DateTimeType}.
     */
    public ZoneId getZoneId() {
        return zoneId;
    }

    /**
     * @return The {@link ZoneOffset} of this {@link DateTimeType}.
     */
    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }

    /**
     * @return {@code true} if this {@link DateTimeType} has an authoritative timezone, {@code false} otherwise.
     */
    public boolean isZoneAuthoritative() {
        return authoritativeZone;
    }

    /**
     * Parse the given string and create a new {@link DateTimeType}.
     *
     * @param value the string to parse.
     * @return a new {@link DateTimeType} representing the parsed date, time and zone information.
     * @throws DateTimeException If no timezone could be parsed and the JVM default zone ID has an invalid format.
     * @throws IllegalArgumentException If the specified value can't be parsed.
     * @throws ZoneRulesException If no timezone could be parsed and the JVM default zone ID cannot be found.
     */
    public static DateTimeType valueOf(String value)
            throws DateTimeException, IllegalArgumentException, ZoneRulesException {
        return new DateTimeType(value);
    }

    /**
     * Create a new {@link DateTimeType} representing the current moment from the system clock with the JVM default
     * timezone as a non-authoritative timezone.
     *
     * @return The new {@link DateTimeType}.
     * @throws DateTimeException If the JVM default timezone has an invalid format.
     * @throws ZoneRulesException If the JVM default timezone can't be found or if no rules are available for that ID.
     */
    public static DateTimeType now() throws DateTimeException, ZoneRulesException {
        return new DateTimeType(Instant.now());
    }

    /**
     * Create a new {@link DateTimeType} representing the current moment from the system clock and the specified
     * authoritative timezone or offset. For details about the supported format,
     * see {@link ZoneId#of(String)}.
     *
     * @param zone the string to parse as a zone ID or offset.
     * @return The new {@link DateTimeType} instance.
     * @throws DateTimeException If the zone has an invalid format or the result exceeds the supported date range.
     * @throws ZoneRulesException If the zone is a region ID that cannot be found or if no rules are available for the
     *             zone ID.
     */
    public static DateTimeType now(String zone) throws DateTimeException, ZoneRulesException {
        return new DateTimeType(Instant.now(), ZoneId.of(zone));
    }

    /**
     * Create a new {@link DateTimeType} representing the current moment from the system clock and the specified
     * authoritative timezone or offset.
     *
     * @param zoneId the {@link ZoneId} or {@link ZoneOffset}.
     * @return The new {@link DateTimeType} instance.
     * @throws ZoneRulesException If no rules are available for the zone ID.
     */
    public static DateTimeType now(ZoneId zoneId) throws ZoneRulesException {
        return new DateTimeType(Instant.now(), zoneId);
    }

    /**
     * Format this {@link DateTimeType} using an optional {@code pattern}.
     * <p>
     * If {@code pattern} is {@code null} a default pattern ({@link #DATE_PATTERN}) is used.
     * <p>
     * <b>Note:</b> This method uses the current timezone of this {@link DateTimeType} for formatting, whether it is
     * authoritative or not. For more control over the timezone used for formatting, use {@link #format(String, ZoneId)}
     * or {@link #format(Locale, String, ZoneId)}.
     *
     * @param pattern the format pattern, or {@code null} to use the default.
     * @return The formatted date-time string.
     * @throws IllegalFormatException If a format string contains an illegal syntax, a format specifier that is
     *             incompatible with the given arguments, insufficient arguments given the format string, or other
     *             illegal conditions.
     * @throws DateTimeException If the result exceeds the supported range or if an error occurs during formatting.
     *
     * @deprecated This method uses the JVM default locale for formatting the date-time. Use
     *             {@link #format(Locale, String)} for correct presentation.
     */
    @Deprecated(forRemoval = false)
    @Override
    public String format(@Nullable String pattern) throws IllegalFormatException, DateTimeException {
        return format(pattern, zoneId);
    }

    /**
     * Format this {@link DateTimeType} using an optional {@code pattern} and the specified timezone.
     * <p>
     * If {@code pattern} is {@code null} a default pattern ({@link #DATE_PATTERN}) is used.
     *
     * @param pattern the format pattern, or {@code null} to use the default.
     * @param zoneId the zone to use for formatting.
     * @return The formatted date-time string.
     * @throws IllegalFormatException If a format string contains an illegal syntax, a format specifier that is
     *             incompatible with the given arguments, insufficient arguments given the format string, or other
     *             illegal conditions.
     * @throws DateTimeException If the result exceeds the supported range or if an error occurs during formatting.
     *
     * @deprecated This method uses the JVM default locale for formatting the date-time. Use
     *             {@link #format(Locale, String, ZoneId)} for correct presentation.
     */
    @Deprecated(forRemoval = false)
    public String format(@Nullable String pattern, ZoneId zoneId) throws IllegalFormatException, DateTimeException {
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        if (pattern == null) {
            return DateTimeFormatter.ofPattern(DATE_PATTERN).format(zonedDateTime);
        }

        return String.format(pattern, zonedDateTime);
    }

    /**
     * Format this {@link DateTimeType} using the provided {@code locale} and optional {@code pattern}.
     * <p>
     * If {@code pattern} is {@code null} a default pattern ({@link #DATE_PATTERN}) is used.
     * <p>
     * <b>Note:</b> This method uses the current timezone of this {@link DateTimeType} for formatting, whether it is
     * authoritative or not. For more control over the timezone used for formatting, use
     * {@link #format(Locale, String, ZoneId)} instead.
     *
     * @param locale the locale to use for formatting.
     * @param pattern the format pattern, or {@code null} to use the default.
     * @return The formatted date-time string.
     * @throws IllegalFormatException If a format string contains an illegal syntax, a format specifier that is
     *             incompatible with the given arguments, insufficient arguments given the format string, or other
     *             illegal conditions.
     * @throws DateTimeException If the result exceeds the supported range or if an error occurs during formatting.
     */
    public String format(Locale locale, @Nullable String pattern) throws IllegalFormatException, DateTimeException {
        return format(locale, pattern, zoneId);
    }

    /**
     * Format this {@link DateTimeType} using the provided {@code locale}, optional {@code pattern} and specified
     * {@code zoneId}.
     * <p>
     * If {@code pattern} is {@code null} a default pattern ({@link #DATE_PATTERN}) is used.
     *
     * @param locale the locale to use for formatting.
     * @param pattern the format pattern, or {@code null} to use the default.
     * @param zoneId the zone to use for formatting.
     * @return The formatted date-time string.
     * @throws IllegalFormatException If a format string contains an illegal syntax, a format specifier that is
     *             incompatible with the given arguments, insufficient arguments given the format string, or other
     *             illegal conditions.
     * @throws DateTimeException If the result exceeds the supported range or if an error occurs during formatting.
     */
    public String format(Locale locale, @Nullable String pattern, ZoneId zoneId)
            throws IllegalFormatException, DateTimeException {
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        if (pattern == null) {
            return DateTimeFormatter.ofPattern(DATE_PATTERN, locale).format(zonedDateTime);
        }

        return String.format(locale, pattern, zonedDateTime);
    }

    /**
     * Truncation returns a copy of this {@code DateTimeType} with fields smaller than the specified unit set to zero.
     * For example, truncating with the {@link ChronoUnit#MINUTES minutes} unit will set the second-of-minute and
     * nano-of-second field to zero.
     * <p>
     * The unit must have a {@linkplain TemporalUnit#getDuration() duration} that divides into the length of a standard
     * day without remainder. This includes all supplied time units on {@link ChronoUnit} and {@link ChronoUnit#DAYS
     * DAYS}. Other units throw an exception.
     * <p>
     * This operates on the local time-line, {@link LocalDateTime#truncatedTo(TemporalUnit) truncating}, which is then
     * converted back to a {@link DateTimeType} using the zone ID to obtain the offset.
     * <p>
     * When converting back to {@code DateTimeType}, if the local date-time is in an overlap, then the offset will be
     * retained if possible, otherwise the earlier offset will be used. If in a gap, the local date-time will be
     * adjusted forward by the length of the gap.
     *
     * @param unit the unit to truncate to.
     * @return The resulting {@code DateTimeType}.
     * @throws DateTimeException If unable to truncate.
     * @throws UnsupportedTemporalTypeException If the unit is not supported.
     */
    public DateTimeType truncatedTo(TemporalUnit unit) throws DateTimeException, UnsupportedTemporalTypeException {
        return new DateTimeType(getZonedDateTime().truncatedTo(unit), authoritativeZone);
    }

    /**
     * Calculate the amount of time between this and a {@link Temporal} object in terms of a single
     * {@code TemporalUnit}. The start and end points are {@code this} and the specified date-time. The result will be
     * negative if the end is before the start.
     * <p>
     * The {@code Temporal} passed to this method is converted to a {@code ZonedDateTime} using
     * {@link ZonedDateTime#from(TemporalAccessor)}. If the time-zone differs between the two zoned date-times, the
     * specified end date-time is normalized to have the same zone as this {@code ZonedDateTime}.
     * <p>
     * The calculation returns a whole number, representing the number of complete units between the two date-times. For
     * example, the amount in months between {@code 2012-06-15T00:00Z} and {@code 2012-08-14T23:59Z} will only be one
     * month as it is one minute short of two months.
     * <p>
     * The calculation is implemented in this method for {@link ChronoUnit}. The units {@code NANOS}, {@code MICROS},
     * {@code MILLIS}, {@code SECONDS}, {@code MINUTES}, {@code HOURS} and {@code HALF_DAYS}, {@code DAYS},
     * {@code WEEKS}, {@code MONTHS}, {@code YEARS}, {@code DECADES}, {@code CENTURIES}, {@code MILLENNIA} and
     * {@code ERAS} are supported. Other {@code ChronoUnit} values will throw an exception.
     * <p>
     * The calculation for date and time units differ.
     * <p>
     * Date units operate on the local time-line, using the local date-time. For example, the period from noon on day 1
     * to noon the following day in days will always be counted as exactly one day, irrespective of whether there was a
     * daylight savings change or not.
     * <p>
     * Time units operate on the instant time-line. The calculation effectively converts both zoned date-times to
     * instants and then calculates the period between the instants. For example, the period from noon on day 1 to noon
     * the following day in hours may be 23, 24 or 25 hours (or some other amount) depending on whether there was a
     * daylight savings change or not.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method is obtained by invoking
     * {@code TemporalUnit.between(Temporal, Temporal)} passing {@link #getZonedDateTime()} as the first argument and
     * the converted input temporal as the second argument.
     *
     * @param endExclusive the end date-time, exclusive.
     * @param unit the unit to measure the amount in.
     * @return the amount of time between this {@link DateTimeType} and the end date-time.
     * @throws ArithmeticException If numeric overflow occurs.
     * @throws DateTimeException If the amount cannot be calculated, or the end temporal cannot be converted to a
     *             {@code ZonedDateTime}, or if the result exceeds the supported range.
     * @throws UnsupportedTemporalTypeException If the unit is not supported.
     */
    public long until(Temporal endExclusive, TemporalUnit unit)
            throws ArithmeticException, DateTimeException, UnsupportedTemporalTypeException {
        if (unit instanceof ChronoUnit && !unit.isDateBased()) {
            return instant.until(endExclusive, unit);
        }
        return getZonedDateTime().until(endExclusive, unit);
    }

    /**
     * Calculate the amount of time between this and another {@link DateTimeType} in terms of a single
     * {@code TemporalUnit}. The start and end points are {@code this} and the specified {@link DateTimeType}. The
     * result will be negative if the end is before the start.
     * <p>
     * The {@link DateTimeType} passed to this method is converted to a {@code ZonedDateTime} using
     * {@link #getZonedDateTime()}. If the time-zone differs between the two zoned {@link DateTimeType}s, the specified
     * end {@code DateTimeType} is normalized to have the same zone as this {@code DateTimeType}.
     * <p>
     * The calculation returns a whole number, representing the number of complete units between the two
     * {@link DateTimeType}s. For example, the amount in months between {@code 2012-06-15T00:00Z} and
     * {@code 2012-08-14T23:59Z} will only be one month as it is one minute short of two months.
     * <p>
     * The calculation is implemented in this method for {@link ChronoUnit}. The units {@code NANOS}, {@code MICROS},
     * {@code MILLIS}, {@code SECONDS}, {@code MINUTES}, {@code HOURS} and {@code HALF_DAYS}, {@code DAYS},
     * {@code WEEKS}, {@code MONTHS}, {@code YEARS}, {@code DECADES}, {@code CENTURIES}, {@code MILLENNIA} and
     * {@code ERAS} are supported. Other {@code ChronoUnit} values will throw an exception.
     * <p>
     * The calculation for date and time units differ.
     * <p>
     * Date units operate on the local time-line, using the local date-time. For example, the period from noon on day 1
     * to noon the following day in days will always be counted as exactly one day, irrespective of whether there was a
     * daylight savings change or not.
     * <p>
     * Time units operate on the instant time-line. The calculation effectively converts both {@link DateTimeType}s to
     * instants and then calculates the period between the instants. For example, the period from noon on day 1 to noon
     * the following day in hours may be 23, 24 or 25 hours (or some other amount) depending on whether there was a
     * daylight savings change or not.
     * <p>
     * If the unit is not a {@code ChronoUnit}, then the result of this method is obtained by invoking
     * {@code TemporalUnit.between(Temporal, Temporal)} passing {@link #getZonedDateTime()} as the first argument and
     * {@code endExclusive.getZonedDateTime()} as the second argument.
     *
     * @param endExclusive the end {@link DateTimeType}, exclusive.
     * @param unit the unit to measure the amount in.
     * @return the amount of time between this and the other {@link DateTimeType}s.
     * @throws ArithmeticException If numeric overflow occurs.
     * @throws DateTimeException If the amount cannot be calculated, or the end temporal cannot be converted to a
     *             {@code ZonedDateTime}, or if the result exceeds the supported range.
     * @throws UnsupportedTemporalTypeException If the unit is not supported.
     */
    public long until(DateTimeType endExclusive, TemporalUnit unit)
            throws ArithmeticException, DateTimeException, UnsupportedTemporalTypeException {
        return until(endExclusive.getZonedDateTime(), unit);
    }

    /**
     * Returns a new {@link DateTimeType}, based on this one, with the specified amount added. The amount is typically
     * {@link Period} or {@link Duration} but may be any other type implementing the {@link TemporalAmount}
     * interface.
     *
     * @param amountToAdd the amount to add.
     * @return The resulting {@code DateTimeType}.
     * @throws ArithmeticException If numeric overflow occurs.
     * @throws DateTimeException If the addition cannot be made or if the result exceeds the supported range.
     */
    public DateTimeType plus(TemporalAmount amountToAdd) throws ArithmeticException, DateTimeException {
        return new DateTimeType(getZonedDateTime().plus(amountToAdd), authoritativeZone);
    }

    /**
     * Return a new {@link DateTimeType}, based on this one, with the amount in terms of the unit added. If it is
     * not possible to add the amount, because the unit is not supported or for some other reason, an exception is
     * thrown.
     * <p>
     * The calculation for date and time units differ. Date units operate on the local time-line. The period is first
     * added to the local date-time, then converted back to a zoned date-time using the zone ID.
     * <p>
     * Time units operate on the instant time-line.
     * <p>
     * If the field is not a {@code ChronoUnit}, then the result of this method is obtained by invoking
     * {@code TemporalUnit.addTo(Temporal, long)}. In this case, the unit determines whether and how to perform the
     * addition.
     *
     * @param amountToAdd the amount of the unit to add to the result, may be negative.
     * @param unit the unit of the amount to add.
     * @return The resulting {@code DateTimeType}.
     * @throws ArithmeticException If numeric overflow occurs.
     * @throws DateTimeException If the addition cannot be made or if the result exceeds the supported range.
     * @throws UnsupportedTemporalTypeException If the unit is not supported.
     * @throws ZoneRulesException If no rules are available for the zone ID.
     */
    public DateTimeType plus(long amountToAdd, TemporalUnit unit)
            throws ArithmeticException, DateTimeException, UnsupportedTemporalTypeException, ZoneRulesException {
        if (unit instanceof ChronoUnit && !unit.isDateBased()) {
            return new DateTimeType(instant.plus(amountToAdd, unit), zoneId, authoritativeZone);
        }
        return new DateTimeType(getZonedDateTime().plus(amountToAdd, unit), authoritativeZone);
    }

    /**
     * Return a new {@link DateTimeType}, based on this one, with the specified amount subtracted. The amount is
     * typically {@link Period} or {@link Duration} but may be any other type implementing the {@link TemporalAmount}
     * interface.
     *
     * @param amountToSubtract the amount to subtract.
     * @return The resulting {@code DateTimeType}.
     * @throws ArithmeticException If numeric overflow occurs.
     * @throws DateTimeException If the subtraction cannot be made or if the result exceeds the supported range.
     */
    public DateTimeType minus(TemporalAmount amountToSubtract) throws ArithmeticException, DateTimeException {
        return new DateTimeType(getZonedDateTime().minus(amountToSubtract), authoritativeZone);
    }

    /**
     * Return a new {@link DateTimeType}, based on this one, with the amount in terms of the unit subtracted. If
     * it is not possible to subtract the amount, because the unit is not supported or for some other reason, an
     * exception is thrown.
     * <p>
     * The calculation for date and time units differ. Date units operate on the local time-line. The period is first
     * subtracted from the local date-time, then converted back to a zoned date-time using the zone ID.
     * <p>
     * Time units operate on the instant time-line.
     * <p>
     * This method is equivalent to {@link #plus(long, TemporalUnit)} with the amount negated.
     *
     * @param amountToSubtract the amount of the unit to subtract.
     * @param unit the unit of the amount to subtract.
     * @return The resulting {@code DateTimeType}.
     * @throws ArithmeticException If numeric overflow occurs.
     * @throws DateTimeException If the subtracted cannot be made or if the result exceeds the supported range.
     * @throws UnsupportedTemporalTypeException If the unit is not supported.
     * @throws ZoneRulesException If no rules are available for the zone ID.
     */
    public DateTimeType minus(long amountToSubtract, TemporalUnit unit)
            throws ArithmeticException, DateTimeException, UnsupportedTemporalTypeException, ZoneRulesException {
        return (amountToSubtract == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit)
                : plus(-amountToSubtract, unit));
    }

    /**
     * Check whether this {@link DateTimeType} is after the specified {@link DateTimeType}.
     *
     * @param moment the {@link DateTimeType} to compare to.
     * @return {@code true} if this is after the specified moment, {@code false} otherwise.
     * @throws DateTimeException If the result exceeds the supported range.
     */
    public boolean isAfter(DateTimeType moment) throws DateTimeException {
        return getZonedDateTime().isAfter(moment.getZonedDateTime());
    }

    /**
     * Check whether this {@link DateTimeType} is after the specified {@link ChronoZonedDateTime}.
     *
     * @param moment the zoned date-time to compare to.
     * @return {@code true} if this is after the specified moment, {@code false} otherwise.
     * @throws DateTimeException If the result exceeds the supported range.
     */
    public boolean isAfter(ChronoZonedDateTime<?> moment) throws DateTimeException {
        return getZonedDateTime().isAfter(moment);
    }

    /**
     * Check whether this {@link DateTimeType} is after the specified {@link Instant}.
     *
     * @param instant the moment in time to compare to.
     * @return {@code true} if this is after the specified instant, {@code false} otherwise.
     */
    public boolean isAfter(Instant instant) {
        return this.instant.isAfter(instant);
    }

    /**
     * Check whether this {@link DateTimeType} is before the specified {@link DateTimeType}.
     *
     * @param moment the {@link DateTimeType} to compare to.
     * @return {@code true} if this is before the specified moment, {@code false} otherwise.
     * @throws DateTimeException If the result exceeds the supported range.
     */
    public boolean isBefore(DateTimeType moment) throws DateTimeException {
        return getZonedDateTime().isBefore(moment.getZonedDateTime());
    }

    /**
     * Check whether this {@link DateTimeType} is before the specified {@link ChronoZonedDateTime}.
     *
     * @param moment the zoned date-time to compare to.
     * @return {@code true} if this is before the specified moment, {@code false} otherwise.
     * @throws DateTimeException If the result exceeds the supported range.
     */
    public boolean isBefore(ChronoZonedDateTime<?> moment) throws DateTimeException {
        return getZonedDateTime().isBefore(moment);
    }

    /**
     * Check whether this {@link DateTimeType} is before the specified {@link Instant}.
     *
     * @param instant the moment in time to compare to.
     * @return {@code true} if this is before the specified instant, {@code false} otherwise.
     */
    public boolean isBefore(Instant instant) {
        return this.instant.isBefore(instant);
    }

    /**
     * Return a {@link DateTimeType} with a fixed offset zone. If this instance already has a fixed offset zone it is
     * returned unchanged, otherwise a new instance with the current offset is returned.
     * <p>
     * The returned {@link DateTimeType} has an authoritative timezone.
     *
     * @return The {@link DateTimeType} using a fixed offset zone.
     * @throws DateTimeException If the result exceeds the supported date range.
     */
    public DateTimeType toFixedOffset() throws DateTimeException {
        return authoritativeZone && zoneId instanceof ZoneOffset ? this : toZone(zoneOffset);
    }

    /**
     * Parse the specified timezone (zone ID, zone offset or zone name), and returns a new {@link DateTimeType} that
     * represents the same moment in time, expressed in the parsed timezone. For details about the supported format,
     * see {@link ZoneId#of(String)}.
     * <p>
     * The returned {@link DateTimeType} has an authoritative timezone.
     *
     * @param zone the target zone as a string.
     * @return The {@link DateTimeType} translated to the specified zone.
     * @throws DateTimeException If the zone has an invalid format or the result exceeds the supported date range.
     * @throws ZoneRulesException If the zone is a region ID that cannot be found or if no rules are available for the
     *             zone ID.
     */
    public DateTimeType toZone(String zone) throws DateTimeException, ZoneRulesException {
        return toZone(ZoneId.of(zone));
    }

    /**
     * Return a {@link DateTimeType} representing this instant with the supplied fixed {@link ZoneOffset}.
     * <p>
     * The returned {@link DateTimeType} has an authoritative timezone.
     *
     * @param offset the fixed offset to use.
     * @return The {@link DateTimeType} adjusted to the provided offset.
     * @throws DateTimeException If the result exceeds the supported date range.
     */
    public DateTimeType toOffset(ZoneOffset offset) throws DateTimeException {
        return toZone(offset);
    }

    /**
     * Create a new {@link DateTimeType} that represents the same moment in time, expressed in the specified timezone.
     * <p>
     * The returned {@link DateTimeType} has an authoritative timezone.
     *
     * @param zoneId the target {@link ZoneId} or {@link ZoneOffset}.
     * @return The new {@link DateTimeType} translated to the given zone.
     * @throws DateTimeException If the result exceeds the supported date range.
     * @throws ZoneRulesException If no rules are available for the zone ID.
     */
    public DateTimeType toZone(ZoneId zoneId) throws DateTimeException, ZoneRulesException {
        return this.authoritativeZone && this.zoneId.equals(zoneId) ? this : new DateTimeType(instant, zoneId);
    }

    @Override
    public String toString() {
        return toString(zoneId);
    }

    public String toString(ZoneId zoneId) {
        try {
            String formatted = instant.atZone(zoneId).format(FORMATTER_TZ_RFC);
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
        } catch (DateTimeException e) {
            return "DateTimeException: " + e.getMessage();
        }
    }

    @Override
    public String toFullString() {
        return toFullString(zoneId, false);
    }

    public String toFullString(ZoneId zoneId) {
        return toFullString(zoneId, true);
    }

    private String toFullString(ZoneId zoneId, boolean explicitZone) {
        try {
            String formatted = instant.atZone(zoneId).format(DateTimeFormatter.ISO_DATE_TIME);
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
            return explicitZone || authoritativeZone ? formatted : '?' + formatted;
        } catch (DateTimeException e) {
            return "DateTimeException: " + e.getMessage();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(authoritativeZone, instant, zoneId, zoneOffset);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DateTimeType)) {
            return false;
        }
        DateTimeType other = (DateTimeType) obj;
        return authoritativeZone == other.authoritativeZone && Objects.equals(instant, other.instant)
                && Objects.equals(zoneId, other.zoneId) && Objects.equals(zoneOffset, other.zoneOffset);
    }

    @Override
    public int compareTo(DateTimeType o) {
        return instant.compareTo(o.instant);
    }

    /**
     * A record containing a {@link ZonedDateTime} and whether the timezone is considered authoritative or not. The
     * timezone is considered authoritative if it was parsed from the value itself, and non-authoritative if it was
     * otherwise inferred, guessed or a default was applied.
     */
    public static record ParsedDateTimeResult(ZonedDateTime zdt, boolean authoritativeZone) {
    }

    /**
     * Parse the specified string into a date, time and optionally a timezone (a zone with rules or a fixed offset from
     * UTC).
     * <p>
     * Several different formats are attempted, in order, until one succeeds or parsing fails. For any of the supported
     * formats, a {@code ?} prefix means that the timezone will be considered non-authoritative, even if it is
     * successfully parsed from the specified string.
     * <p>
     * Formats are processed in this order:
     * <ul>
     * <li>{@code 2022-12-22 12:22*} is changed to {@code 2022-12-22T12:22*} to conform with the ISO syntax. All the
     * following variants are then attempted on the resulting string.</li>
     * <li>{@code 2022-12-22T12:22+00:00} / {@code 2022-12-22T12:22:11+0000} / {@code 2022-12-22T12:22:11.000+00:00}<br>
     * The zone offset is always 4 digits, with or without {@code :}</li>
     * <li>{@code 2022-12-22T12:22Z} / {@code 2022-12-22T12:22:11+0000} / {@code 2022-12-22T12:22:11.000+00:00}<br>
     * The zone offset is the letter Z for UTC, or 2, 4 or 6 digits, with or without {@code :}</li>
     * <li>{@code 2022-12-22T12:22} / {@code 2022-12-22T12:22:11+01:00} /
     * {@code 2022-12-22T12:22:11.000+01:00[Europe/Paris]}<br>
     * The time zone is specified both as an offset and optionally as a zone ID</li>
     * <li>{@code 2022-12-22T12:22PST} / {@code 2022-12-22T12:22:11CET} /
     * {@code 2022-12-22T12:22:11.000Central European Time}<br>
     * The time zone is the time zone name in either abbreviated or full form</li>
     * <li>{@code 2022-12-22T12:22} / {@code 2022-12-22T12:22:11} / {@code 2022-12-22T12:22:11.000}<br>
     * No time zone information, results in a {@link LocalDateTime}</li>
     * </ul>
     * If none of the above succeeds, {@code 1970-01-01T} is prepended in case the specified string is a time only. All
     * the above formats are then attempted again.
     * <p>
     * If this too fails, an attempt is made to parse the string as a number. If that succeeds, it will be interpreted
     * as the number of seconds or milliseconds since {@code 1970-01-01T00:00:00Z}. If the number of digits are below
     * 12, it is assumed to be seconds. If it's 12 or more, it's assumed to be milliseconds.
     * <p>
     * If the value isn't a number either, a final attempt is made to append {@code T00:00:00} to the string, in case
     * it's a date only. All the above formats are then attempted again.
     * <p>
     * If that fails as well, an {@link IllegalArgumentException} is thrown and the parsing is considered a failure.
     *
     * @param value the string to parse into a date-time and timezone.
     * @return The resulting {@link ParsedDateTimeResult}.
     * @throws DateTimeException If no timezone could be parsed and the JVM default zone ID has an invalid format.
     * @throws IllegalArgumentException If the specified value can't be parsed.
     * @throws ZoneRulesException If no timezone could be parsed and the JVM default zone ID cannot be found.
     */
    public static ParsedDateTimeResult parseDateTime(String value)
            throws DateTimeException, IllegalArgumentException, ZoneRulesException {
        String dateTime;
        boolean explicitNotAuthoritative;
        if (value.charAt(0) == '?') {
            dateTime = value.substring(1);
            explicitNotAuthoritative = true;
        } else {
            dateTime = value;
            explicitNotAuthoritative = false;
        }
        try {
            // direct parsing (date and time)
            Temporal temporal;
            try {
                if (DATE_PARSE_PATTERN_WITH_SPACE.matcher(dateTime).matches()) {
                    temporal = parse(dateTime.substring(0, 10) + "T" + dateTime.substring(11));
                } else {
                    temporal = parse(dateTime);
                }
            } catch (DateTimeParseException fullDtException) {
                // time only
                try {
                    temporal = parse("1970-01-01T" + dateTime);
                } catch (DateTimeParseException timeOnlyException) {
                    try {
                        long epoch = Double.valueOf(dateTime).longValue();
                        int length = (int) (Math.log10(epoch >= 0 ? epoch : epoch * -1) + 1);
                        // Assume that below 12 digits we're in seconds
                        if (length < 12) {
                            temporal = Instant.ofEpochSecond(epoch);
                        } else {
                            temporal = Instant.ofEpochMilli(epoch);
                        }
                    } catch (NumberFormatException notANumberException) {
                        // date only
                        if (dateTime.length() == 10) {
                            temporal = parse(dateTime + "T00:00:00");
                        } else {
                            temporal = parse(dateTime.substring(0, 10) + "T00:00:00" + dateTime.substring(10));
                        }
                    }
                }
            }

            boolean authoritativeZone;
            if (temporal instanceof LocalDateTime localDateTime) {
                temporal = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
                authoritativeZone = false;
            } else if (temporal instanceof Instant instant) {
                temporal = instant.atZone(ZoneId.systemDefault());
                authoritativeZone = false;
            } else {
                authoritativeZone = true;
            }
            return new ParsedDateTimeResult((ZonedDateTime) temporal, !explicitNotAuthoritative && authoritativeZone);
        } catch (DateTimeParseException invalidFormatException) {
            throw new IllegalArgumentException(dateTime + " is not in a valid format.", invalidFormatException);
        }
    }

    /**
     * Attempt to parse the specified string using 4 different parsing patterns in the following order:
     * <ul>
     * <li>{@code 2022-12-22T12:22+00:00} / {@code 2022-12-22T12:22:11+0000} / {@code 2022-12-22T12:22:11.000+00:00}<br>
     * The zone offset is always 4 digits, with or without {@code :}</li>
     * <li>{@code 2022-12-22T12:22Z} / {@code 2022-12-22T12:22:11+0000} / {@code 2022-12-22T12:22:11.000+00:00}<br>
     * The zone offset is the letter Z for UTC, or 2, 4 or 6 digits, with or without {@code :}</li>
     * <li>{@code 2022-12-22T12:22} / {@code 2022-12-22T12:22:11+01:00} /
     * {@code 2022-12-22T12:22:11.000+01:00[Europe/Paris]}<br>
     * The time zone is specified both as an offset and optionally as a zone ID</li>
     * <li>{@code 2022-12-22T12:22PST} / {@code 2022-12-22T12:22:11CET} /
     * {@code 2022-12-22T12:22:11.000Central European Time}<br>
     * The time zone is the time zone name in either abbreviated or full form</li>
     * <li>{@code 2022-12-22T12:22} / {@code 2022-12-22T12:22:11} / {@code 2022-12-22T12:22:11.000}<br>
     * No time zone information, results in a {@link LocalDateTime}</li>
     * </ul>
     *
     * The result is always a {@link ZonedDateTime} or a {@link LocalDateTime}.
     *
     * @param value the string to parse.
     * @return the resulting {@link ZonedDateTime} or {@link LocalDateTime}.
     * @throws DateTimeParseException If the parsing fails.
     */
    private static Temporal parse(String value) throws DateTimeParseException {
        try {
            return ZonedDateTime.parse(value, PARSER_TZ_RFC);
        } catch (DateTimeParseException tzMsRfcException) {
            try {
                return ZonedDateTime.parse(value, PARSER_TZ_ISO);
            } catch (DateTimeParseException tzMsIsoException) {
                try {
                    return (Temporal) DateTimeFormatter.ISO_DATE_TIME.parseBest(value, ZonedDateTime::from,
                            LocalDateTime::from);
                } catch (DateTimeParseException tzException) {
                    try {
                        return ZonedDateTime.parse(value, PARSER_TZ);
                    } catch (DateTimeParseException isoException) {
                        try {
                            return ZonedDateTime.parse(value);
                        } catch (DateTimeParseException e) {
                            return LocalDateTime.parse(value, PARSER);
                        }
                    }
                }
            }
        }
    }
}
