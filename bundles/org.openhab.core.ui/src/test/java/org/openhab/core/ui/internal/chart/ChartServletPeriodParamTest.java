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
package org.openhab.core.ui.internal.chart;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.ui.internal.chart.ChartServlet.PeriodBeginEnd;
import org.openhab.core.ui.internal.chart.ChartServlet.PeriodPastFuture;

/**
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class ChartServletPeriodParamTest {

    @Test
    public void convertToTemporalAmountFromNull() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount(null, Duration.ZERO);
        assertNotNull(period);
        assertEquals(0, period.get(ChronoUnit.SECONDS));
    }

    @Test
    public void convertToTemporalAmountFromHours() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("h", Duration.ZERO);
        assertNotNull(period);
        assertEquals(1 * 60 * 60, period.get(ChronoUnit.SECONDS));

        period = ChartServlet.convertToTemporalAmount("12h", Duration.ZERO);
        assertNotNull(period);
        assertEquals(12 * 60 * 60, period.get(ChronoUnit.SECONDS));
    }

    @Test
    public void convertToTemporalAmountFromDays() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("D", Duration.ZERO);
        assertNotNull(period);
        assertEquals(1, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("4D", Duration.ZERO);
        assertNotNull(period);
        assertEquals(4, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));
    }

    @Test
    public void convertToTemporalAmountFromWeeks() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("W", Duration.ZERO);
        assertNotNull(period);
        assertEquals(7, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("2W", Duration.ZERO);
        assertNotNull(period);
        assertEquals(14, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));
    }

    @Test
    public void convertToTemporalAmountFromMonths() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("M", Duration.ZERO);
        assertNotNull(period);
        assertEquals(0, period.get(ChronoUnit.DAYS));
        assertEquals(1, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("3M", Duration.ZERO);
        assertNotNull(period);
        assertEquals(0, period.get(ChronoUnit.DAYS));
        assertEquals(3, period.get(ChronoUnit.MONTHS));
        assertEquals(0, period.get(ChronoUnit.YEARS));
    }

    @Test
    public void convertToTemporalAmountFromYears() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("Y", Duration.ZERO);
        assertNotNull(period);
        assertEquals(0, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(1, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("2Y", Duration.ZERO);
        assertNotNull(period);
        assertEquals(0, period.get(ChronoUnit.DAYS));
        assertEquals(0, period.get(ChronoUnit.MONTHS));
        assertEquals(2, period.get(ChronoUnit.YEARS));
    }

    @Test
    public void convertToTemporalAmountFromISO8601() {
        TemporalAmount period = ChartServlet.convertToTemporalAmount("P2Y3M4D", Duration.ZERO);
        assertNotNull(period);
        assertEquals(4, period.get(ChronoUnit.DAYS));
        assertEquals(3, period.get(ChronoUnit.MONTHS));
        assertEquals(2, period.get(ChronoUnit.YEARS));

        period = ChartServlet.convertToTemporalAmount("P1DT12H30M15S", Duration.ZERO);
        assertNotNull(period);
        assertEquals(36 * 60 * 60 + 30 * 60 + 15, period.get(ChronoUnit.SECONDS));
    }

    @Test
    public void getPeriodPastFutureByDefault() {
        PeriodPastFuture period = ChartServlet.getPeriodPastFuture(null);
        assertNotNull(period.past());
        assertEquals(ChartServlet.DEFAULT_PERIOD, period.past());
        assertNull(period.future());

        period = ChartServlet.getPeriodPastFuture("-");
        assertNull(period.past());
        assertNotNull(period.future());
        assertEquals(ChartServlet.DEFAULT_PERIOD, period.future());
    }

    @Test
    public void getPeriodPastFutureWithOnlyPast() {
        Period duration = Period.ofDays(2);

        PeriodPastFuture period = ChartServlet.getPeriodPastFuture("2D");
        assertNotNull(period.past());
        assertEquals(duration, period.past());
        assertNull(period.future());

        period = ChartServlet.getPeriodPastFuture("2D-");
        assertNotNull(period.past());
        assertEquals(duration, period.past());
        assertNull(period.future());
    }

    @Test
    public void getPeriodPastFutureWithOnlyFuture() {
        Period duration = Period.ofMonths(3);

        PeriodPastFuture period = ChartServlet.getPeriodPastFuture("-3M");
        assertNull(period.past());
        assertNotNull(period.future());
        assertEquals(duration, period.future());
    }

    @Test
    public void getPeriodPastFutureWithPastAndFuture() {
        Period duration1 = Period.ofDays(2);
        Period duration2 = Period.ofMonths(3);

        PeriodPastFuture period = ChartServlet.getPeriodPastFuture("2D-3M");
        assertNotNull(period.past());
        assertEquals(duration1, period.past());
        assertNotNull(period.future());
        assertEquals(duration2, period.future());
    }

    @Test
    public void getPeriodBeginEndWithBeginAndEnd() {
        ZonedDateTime now = ZonedDateTime.of(2024, 4, 9, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime begin = ZonedDateTime.of(2024, 4, 9, 11, 30, 0, 0, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.of(2024, 4, 9, 13, 30, 0, 0, ZoneId.systemDefault());

        PeriodBeginEnd beginEnd = ChartServlet.getPeriodBeginEnd(begin, end, ChartServlet.getPeriodPastFuture("2D-3M"),
                now);
        assertEquals(begin, beginEnd.begin());
        assertEquals(end, beginEnd.end());
    }

    @Test
    public void getPeriodBeginEndWithBeginButNotEnd() {
        ZonedDateTime now = ZonedDateTime.of(2024, 4, 9, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime begin = ZonedDateTime.of(2024, 4, 9, 11, 30, 0, 0, ZoneId.systemDefault());

        PeriodBeginEnd beginEnd = ChartServlet.getPeriodBeginEnd(begin, null, ChartServlet.getPeriodPastFuture("2D-3M"),
                now);
        assertEquals(begin, beginEnd.begin());
        assertEquals(ZonedDateTime.of(2024, 7, 11, 11, 30, 0, 0, ZoneId.systemDefault()), beginEnd.end());

        beginEnd = ChartServlet.getPeriodBeginEnd(begin, null, ChartServlet.getPeriodPastFuture("2D"), now);
        assertEquals(begin, beginEnd.begin());
        assertEquals(ZonedDateTime.of(2024, 4, 11, 11, 30, 0, 0, ZoneId.systemDefault()), beginEnd.end());

        beginEnd = ChartServlet.getPeriodBeginEnd(begin, null, ChartServlet.getPeriodPastFuture("-3M"), now);
        assertEquals(begin, beginEnd.begin());
        assertEquals(ZonedDateTime.of(2024, 7, 9, 11, 30, 0, 0, ZoneId.systemDefault()), beginEnd.end());
    }

    @Test
    public void getPeriodBeginEndWithEndButNotBegin() {
        ZonedDateTime now = ZonedDateTime.of(2024, 4, 9, 12, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.of(2024, 7, 11, 11, 30, 0, 0, ZoneId.systemDefault());

        PeriodBeginEnd beginEnd = ChartServlet.getPeriodBeginEnd(null, end, ChartServlet.getPeriodPastFuture("2D-3M"),
                now);
        assertEquals(ZonedDateTime.of(2024, 4, 9, 11, 30, 0, 0, ZoneId.systemDefault()), beginEnd.begin());
        assertEquals(end, beginEnd.end());

        beginEnd = ChartServlet.getPeriodBeginEnd(null, end, ChartServlet.getPeriodPastFuture("2D"), now);
        assertEquals(ZonedDateTime.of(2024, 7, 9, 11, 30, 0, 0, ZoneId.systemDefault()), beginEnd.begin());
        assertEquals(end, beginEnd.end());

        beginEnd = ChartServlet.getPeriodBeginEnd(null, end, ChartServlet.getPeriodPastFuture("-3M"), now);
        assertEquals(ZonedDateTime.of(2024, 4, 11, 11, 30, 0, 0, ZoneId.systemDefault()), beginEnd.begin());
        assertEquals(end, beginEnd.end());
    }

    @Test
    public void getPeriodBeginEndWithPeriodButNotBeginEnd() {
        ZonedDateTime now = ZonedDateTime.of(2024, 4, 9, 12, 0, 0, 0, ZoneId.systemDefault());

        PeriodBeginEnd beginEnd = ChartServlet.getPeriodBeginEnd(null, null, ChartServlet.getPeriodPastFuture("2D-3M"),
                now);
        assertEquals(ZonedDateTime.of(2024, 4, 7, 12, 0, 0, 0, ZoneId.systemDefault()), beginEnd.begin());
        assertEquals(ZonedDateTime.of(2024, 7, 9, 12, 0, 0, 0, ZoneId.systemDefault()), beginEnd.end());

        beginEnd = ChartServlet.getPeriodBeginEnd(null, null, ChartServlet.getPeriodPastFuture("2D"), now);
        assertEquals(ZonedDateTime.of(2024, 4, 7, 12, 0, 0, 0, ZoneId.systemDefault()), beginEnd.begin());
        assertEquals(ZonedDateTime.of(2024, 4, 9, 12, 0, 0, 0, ZoneId.systemDefault()), beginEnd.end());

        beginEnd = ChartServlet.getPeriodBeginEnd(null, null, ChartServlet.getPeriodPastFuture("-3M"), now);
        assertEquals(ZonedDateTime.of(2024, 4, 9, 12, 0, 0, 0, ZoneId.systemDefault()), beginEnd.begin());
        assertEquals(ZonedDateTime.of(2024, 7, 9, 12, 0, 0, 0, ZoneId.systemDefault()), beginEnd.end());
    }
}
