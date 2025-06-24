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

    @Test
    public void testParseCustom() {
        assertEquals(350, DurationUtils.parse("350ms").toMillis());
        assertEquals(1000, DurationUtils.parse("1s").toMillis());
        assertEquals(60000, DurationUtils.parse("1m").toMillis());
        assertEquals(3600000, DurationUtils.parse("1h").toMillis());
        assertEquals(86400000, DurationUtils.parse("1d").toMillis());

        // Mixed units
        assertEquals(61000, DurationUtils.parse("1m 1s").toMillis());
        assertEquals(3661000, DurationUtils.parse("1h 1m 1s").toMillis());
        assertEquals(3661000, DurationUtils.parse("1h1m1s").toMillis());
    }
}
