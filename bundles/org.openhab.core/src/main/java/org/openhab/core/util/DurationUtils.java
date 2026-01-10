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
package org.openhab.core.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Static utility methods that are helpful when dealing with {@link Duration}.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class DurationUtils {

    private static final Pattern DURATION_PATTERN = Pattern.compile("""
            (?:([0-9]+)\\s*(?:d|days?))?
            \\s*
            (?:([0-9]+)\\s*(?:h|hrs?|hours?))?
            \\s*
            (?:([0-9]+)\\s*(?:m|mins?|minutes?))?
            \\s*
            (?:([0-9]+)\\s*(?:s|secs?|seconds?))?
            \\s*
            (?:([0-9]+)\\s*(?:ms|milliseconds?))?
            """, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);

    private static final ChronoUnit[] DURATION_UNITS = { ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES,
            ChronoUnit.SECONDS, ChronoUnit.MILLIS };

    /**
     * Parses a duration string in ISO-8601 duration format or a custom format like
     * "1d 1h 15m 30s 500ms".
     *
     * When specifying a duration, the units must be specified in the order of
     * days, hours, minutes, seconds, and milliseconds,
     * although any individual unit may be omitted.
     * Each unit must be preceded by an integer value.
     * A space between the number and its corresponding unit is permitted but not required.
     * Likewise, whitespace between unit groups is optional.
     *
     * The units supported in the duration format are:
     * <ul>
     * <li>'d|day|days' for days,
     * <li>'h|hr|hrs|hour|hours' for hours,
     * <li>'m|min|mins|minute|minutes' for minutes,
     * <li>'s|sec|secs|second|seconds' for seconds, and
     * <li>'ms|millisecond|milliseconds' for milliseconds.
     * </ul>
     *
     * Examples of valid duration strings:
     * <ul>
     * <li>"1h" represents 1 hour
     * <li>"15m" represents 15 minutes
     * <li>"1h15m" represents 1 day and 15 minutes. It can also be written as "1h 15m", "1 h 15 m", "1 hr 15 mins",
     * "1hour 15 minutes", etc.
     * <li>"1d 1h 30s" represents 1 day, 1 hour, and 30 seconds
     * </ul>
     *
     * The ISO-8601 duration format is supported, but only the following units are recognized:
     * days, hours, minutes, and seconds.
     * Units such as years, months, and weeks are not supported.
     * The number of days, hours and minutes must parse to a long.
     * The number of seconds must parse to a long with optional fraction.
     * The decimal point may be either a dot or a comma.
     * The fractional part may have from zero to 9 digits.
     *
     * Examples of ISO-8601 durations:
     * <ul>
     * <li>"PT1H30M" represents 1 hour and 30 minutes
     * <li>"PT1D" represents 1 day
     * <li>"PT0.5S" represents 0.5 seconds (500 milliseconds)
     * </ul>
     *
     * @param durationString the string representation of the duration
     * @return a Duration object representing the parsed duration
     * @throws IllegalArgumentException if the string cannot be parsed into a valid duration
     */
    public static Duration parse(String durationString) throws IllegalArgumentException {
        try {
            return Duration.parse(durationString);
        } catch (Exception e) {
            // ignore
        }

        Matcher m = DURATION_PATTERN.matcher(durationString);
        if (!m.matches() || (m.group(1) == null && m.group(2) == null && m.group(3) == null && m.group(4) == null
                && m.group(5) == null)) {
            throw new IllegalArgumentException("Invalid duration: " + durationString
                    + ". Expected an ISO8601 duration or something like: '1d 1h 15m 30s 300ms'");
        }

        Duration duration = Duration.ZERO;
        for (int i = 0; i < DURATION_UNITS.length; i++) {
            String value = m.group(i + 1);
            if (value != null) {
                long amount = Long.parseLong(value);
                duration = duration.plus(amount, DURATION_UNITS[i]);
            }
        }
        return duration;
    }
}
