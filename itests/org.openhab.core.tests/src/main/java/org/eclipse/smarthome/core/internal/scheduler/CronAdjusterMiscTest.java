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
package org.eclipse.smarthome.core.internal.scheduler;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Test cases for {@link CronAdjuster} to test error handling and misc cases.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
public class CronAdjusterMiscTest {

    @Test
    public void testReboot() {
        assertThat("Cron expression @reboot should be reboot", new CronAdjuster("@reboot").isReboot(),
                is(equalTo(true)));
        assertThat("Cron expression @daily should be not reboot", new CronAdjuster("@daily").isReboot(),
                is(equalTo(false)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToShort() {
        new CronAdjuster("* * * * *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLong() {
        new CronAdjuster("* * * * * * * *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDayInWeek() {
        new CronAdjuster("* * * * * FRI#X");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMonthName() {
        new CronAdjuster("* * * * FXB *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWeekNumber() {
        new CronAdjuster("* * * XW * *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBeforeSlash() {
        new CronAdjuster("* * * 1/X * *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAfterSlash() {
        new CronAdjuster("* * * X/10 * *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeWeekDay() {
        new CronAdjuster("* * * * * FRI-1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeLeftSideError() {
        new CronAdjuster("X-3 * * * * *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRangeRightSideError() {
        new CronAdjuster("3-X * * * * *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBelowMin() {
        new CronAdjuster("0 0 0 0 0 0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAboveMax() {
        new CronAdjuster("99 * * * * *");
    }
}
