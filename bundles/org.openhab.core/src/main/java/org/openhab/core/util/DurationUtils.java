/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?:([0-9]+)D)?\\s*(?:([0-9]+)H)?\\s*(?:([0-9]+)M)?\\s*(?:([0-9]+)S)?\\s*(?:([0-9]+)MS)?",
            Pattern.CASE_INSENSITIVE);
    private static final ChronoUnit[] DURATION_UNITS = { ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES,
            ChronoUnit.SECONDS, ChronoUnit.MILLIS };

    /**
     * Parses a duration string in ISO-8601 format or a custom format like
     * "1d 1h 15m 30s 500ms" where
     * 'd' stands for days,
     * 'h' for hours,
     * 'm' for minutes,
     * 's' for seconds, and
     * 'ms' for milliseconds.
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
