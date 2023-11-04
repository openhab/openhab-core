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
package org.openhab.core.ui.internal.chart;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Period;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class ChartServletPeriodParamTest {

    @Test
    public void convertToPeriodFromNull() {
        Period period = ChartServlet.convertToPeriod(null, Period.ZERO);
        assertTrue(period.isZero());
    }

    @Test
    public void convertToPeriodFromHours() {
        Period period = ChartServlet.convertToPeriod("2h", Period.ZERO);
        assertTrue(period.isZero());
    }

    @Test
    public void convertToPeriodFromDays() {
        Period period = ChartServlet.convertToPeriod("D", Period.ZERO);
        assertEquals(1, period.getDays());
        assertEquals(0, period.getMonths());
        assertEquals(0, period.getYears());

        period = ChartServlet.convertToPeriod("4D", Period.ZERO);
        assertEquals(4, period.getDays());
        assertEquals(0, period.getMonths());
        assertEquals(0, period.getYears());
    }

    @Test
    public void convertToPeriodFromWeeks() {
        Period period = ChartServlet.convertToPeriod("W", Period.ZERO);
        assertEquals(7, period.getDays());
        assertEquals(0, period.getMonths());
        assertEquals(0, period.getYears());

        period = ChartServlet.convertToPeriod("2W", Period.ZERO);
        assertEquals(14, period.getDays());
        assertEquals(0, period.getMonths());
        assertEquals(0, period.getYears());
    }

    @Test
    public void convertToPeriodFromMonths() {
        Period period = ChartServlet.convertToPeriod("M", Period.ZERO);
        assertEquals(0, period.getDays());
        assertEquals(1, period.getMonths());
        assertEquals(0, period.getYears());

        period = ChartServlet.convertToPeriod("3M", Period.ZERO);
        assertEquals(0, period.getDays());
        assertEquals(3, period.getMonths());
        assertEquals(0, period.getYears());
    }

    @Test
    public void convertToPeriodFromYears() {
        Period period = ChartServlet.convertToPeriod("Y", Period.ZERO);
        assertEquals(0, period.getDays());
        assertEquals(0, period.getMonths());
        assertEquals(1, period.getYears());

        period = ChartServlet.convertToPeriod("2Y", Period.ZERO);
        assertEquals(0, period.getDays());
        assertEquals(0, period.getMonths());
        assertEquals(2, period.getYears());
    }

    @Test
    public void convertToPeriodFromISO8601() {
        Period period = ChartServlet.convertToPeriod("P2Y3M4D", Period.ZERO);
        assertEquals(4, period.getDays());
        assertEquals(3, period.getMonths());
        assertEquals(2, period.getYears());

        period = ChartServlet.convertToPeriod("P1DT12H30M15S", Period.ZERO);
        assertTrue(period.isZero());
    }

    @Test
    public void convertToDurationFromNull() {
        Duration duration = ChartServlet.convertToDuration(null, Duration.ZERO);
        assertTrue(duration.isZero());
    }

    @Test
    public void convertToDurationFromHours() {
        Duration duration = ChartServlet.convertToDuration("h", Duration.ZERO);
        assertEquals(1 * 60 * 60, duration.getSeconds());

        duration = ChartServlet.convertToDuration("12h", Duration.ZERO);
        assertEquals(12 * 60 * 60, duration.getSeconds());
    }

    @Test
    public void convertToDurationFromDays() {
        Duration duration = ChartServlet.convertToDuration("D", Duration.ZERO);
        assertEquals(24 * 60 * 60, duration.getSeconds());

        duration = ChartServlet.convertToDuration("2D", Duration.ZERO);
        assertEquals(48 * 60 * 60, duration.getSeconds());
    }

    @Test
    public void convertToDurationFromWeeks() {
        Duration duration = ChartServlet.convertToDuration("2W", Duration.ZERO);
        assertTrue(duration.isZero());
    }

    @Test
    public void convertToDurationFromMonths() {
        Duration duration = ChartServlet.convertToDuration("3M", Duration.ZERO);
        assertTrue(duration.isZero());
    }

    @Test
    public void convertToDurationFromYears() {
        Duration duration = ChartServlet.convertToDuration("2Y", Duration.ZERO);
        assertTrue(duration.isZero());
    }

    @Test
    public void convertToDurationFromISO8601() {
        Duration duration = ChartServlet.convertToDuration("P1DT12H30M15S", Duration.ZERO);
        assertEquals(36 * 60 * 60 + 30 * 60 + 15, duration.getSeconds());

        duration = ChartServlet.convertToDuration("P2Y3M4D", Duration.ZERO);
        assertTrue(duration.isZero());
    }
}
