/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.internal.scheduler;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.scheduler.CronAdjuster;

/**
 * Test cases for {@link CronAdjuster} to test error handling and misc cases.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class CronAdjusterMiscTest {

    @Test
    public void testReboot() {
        assertThat("Cron expression @reboot should be reboot", new CronAdjuster("@reboot").isReboot(),
                is(equalTo(true)));
        assertThat("Cron expression @daily should be not reboot", new CronAdjuster("@daily").isReboot(),
                is(equalTo(false)));
    }

    @Test
    public void testToShort() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("* * * * *"));
    }

    @Test
    public void testToLong() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("* * * * * * * *"));
    }

    @Test
    public void testDayInWeek() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("* * * * * FRI#X"));
    }

    @Test
    public void testIllegalDayInWeek() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("* * * * * NO"));
    }

    @Test
    public void testMonthName() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("* * * * FXB *"));
    }

    @Test
    public void testWeekNumber() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("* * * XW * *"));
    }

    @Test
    public void testBeforeSlash() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("* * * 1/X * *"));
    }

    @Test
    public void testAfterSlash() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("* * * X/10 * *"));
    }

    @Test
    public void testRangeLeftSideError() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("X-3 * * * * *"));
    }

    @Test
    public void testRangeRightSideError() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("3-X * * * * *"));
    }

    @Test
    public void testBelowMin() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("0 0 0 0 0 0"));
    }

    @Test
    public void testAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("99 * * * * *"));
    }

    @Test
    public void testAboveWeekday() {
        assertThrows(IllegalArgumentException.class, () -> new CronAdjuster("* * * * * 8"));
    }
}
