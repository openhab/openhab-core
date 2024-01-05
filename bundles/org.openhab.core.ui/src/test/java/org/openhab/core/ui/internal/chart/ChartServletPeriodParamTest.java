/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class ChartServletPeriodParamTest {

    @Test
    public void convertToTemporalAmountFromNull() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount(null, Duration.ZERO);
        assertEquals(0, period.get(ChronoUnit.SECONDS));
    }

    @Test
    public void convertToTemporalAmountFromHours() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("h", Duration.ZERO);
        assertEquals(1 * 60 * 60, period.get(ChronoUnit.SECONDS));

        period = ChartServlet.convertToTemporalAmount("12h", Duration.ZERO);
        assertEquals(12 * 60 * 60, period.get(ChronoUnit.SECONDS));
    }

    @Test
    public void convertToTemporalAmountFromDays() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("D", Duration.ZERO);
        assertEquals(1, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("4D", Duration.ZERO);
        assertEquals(4, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));
    }

    @Test
    public void convertToTemporalAmountFromWeeks() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("W", Duration.ZERO);
        assertEquals(7, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("2W", Duration.ZERO);
        assertEquals(14, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));
    }

    @Test
    public void convertToTemporalAmountFromMonths() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("M", Duration.ZERO);
        assertEquals(0, period.get(ChronoUnit.DAYS));
        assertEquals(1, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("3M", Duration.ZERO);
        assertEquals(0, period.get(ChronoUnit.DAYS));
        assertEquals(3, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));
    }

    @Test
    public void convertToTemporalAmountFromYears() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("Y", Duration.ZERO);
        assertEquals(0, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(1, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("2Y", Duration.ZERO);
        assertEquals(0, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(2, period.get(ChronoUnit.YEARS));
    }

    @Test
    public void convertToTemporalAmountFromISO8601() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("P2Y3M4D", Duration.ZERO);
        assertEquals(4, period.get(ChronoUnit.DAYS));
        assertEquals(3, period.get(ChronoUnit.MONTHS));
        assertEquals(2, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("P1DT12H30M15S", Duration.ZERO);
        assertEquals(36 * 60 * 60 + 30 * 60 + 15, period.get(ChronoUnit.SECONDS));
    }
}
