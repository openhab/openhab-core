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

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DurationUtils}
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class DurationUtilsTest {

    @Test
    public void testParseISO8601() {
        assertEquals(0, DurationUtils.parse("PT0S").toMillis());
        assertEquals(300, DurationUtils.parse("PT0.3S").toMillis());
        assertEquals(1000, DurationUtils.parse("PT1S").toMillis());
        assertEquals(60000, DurationUtils.parse("PT1M").toMillis());
        assertEquals(3600000, DurationUtils.parse("PT1H").toMillis());
        assertEquals(86400000, DurationUtils.parse("P1D").toMillis());

        // Mixed units
        assertEquals(61000, DurationUtils.parse("PT1M1S").toMillis());
        assertEquals(3661000, DurationUtils.parse("PT1H1M1S").toMillis());
    }

    private void testUnitCombinations(long expectedMillis, String number, String... units) {
        for (String unit : units) {
            assertEquals(expectedMillis, DurationUtils.parse(number + unit).toMillis());
            assertEquals(expectedMillis, DurationUtils.parse(number + " " + unit).toMillis());
        }
    }

    @Test
    public void testParseCustom() {
        testUnitCombinations(350, "350", "ms", "millisecond", "milliseconds");
        testUnitCombinations(1000, "1", "s", "sec", "secs", "second", "seconds");
        testUnitCombinations(60000, "1", "m", "min", "mins", "minute", "minutes");
        testUnitCombinations(3600000, "1", "h", "hr", "hrs", "hour", "hours");
        testUnitCombinations(86400000, "1", "d", "day", "days");

        // Mixed units
        assertEquals(61000, DurationUtils.parse("1m 1s").toMillis());
        assertEquals(61000, DurationUtils.parse("1 m 1 s").toMillis());
        assertEquals(61000, DurationUtils.parse("1 min 1 sec").toMillis());
        assertEquals(3661000, DurationUtils.parse("1h 1m 1s").toMillis());
        assertEquals(3661000, DurationUtils.parse("1h1m1s").toMillis());
    }
}
