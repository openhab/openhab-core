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
package org.openhab.core.energy.grid;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Instant;
import java.time.ZoneId;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * {@code grid-constraints} <em>Peak-based fee models</em> and <em>Capacity budget denominated in the billed
 * quantity</em>, one test per scenario.
 * <p>
 * The arithmetic is small and the mistakes it invites are not: a selector pointed the wrong way sheds a house all
 * month for no billing benefit, and an estimate committed as a billed quantity invents a peak the meter never saw.
 * Both are tested for directly.
 *
 * @author Stamate Viorel - Initial contribution
 */
@NonNullByDefault
public class CapacityTariffTest {

    private static final ZoneId BRUSSELS = ZoneId.of("Europe/Brussels");

    /** Belgium: quarter-hour buckets, 2.5 kW minimum billable demand. */
    private static CapacityTracker belgianTracker(double minimumBillableW) {
        return new CapacityTracker(MeteringSlots.quarterHourly(BRUSSELS), new BucketPeakFee(minimumBillableW));
    }

    /** "Established peak as budget": a peak already paid for is a budget, not a line to creep away from. */
    @Test
    public void amonthToDatePeakIsFreeBudgetForTheRestOfTheMonth() {
        BucketPeakFee fee = new BucketPeakFee(2500);
        fee.commit(4200);

        assertThat(fee.billedPeakW(), is(4200.0));
        assertThat(fee.allowanceW(), is(4200.0));
    }

    /** "Below the minimum billable demand there is nothing to shave." */
    @Test
    public void underTheFloorTheAnswerIsTheFloor() {
        BucketPeakFee fee = new BucketPeakFee(2500);
        fee.commit(1800);

        assertThat(fee.recordedPeakW(), is(1800.0));
        assertThat(fee.allowanceW(), is(2500.0));
    }

    /**
     * "The free budget is the larger of the two, in both directions."
     * <p>
     * This is the one the decision record got backwards once. The reference computes it over signed watts where
     * import is negative, so its {@code min} selects the larger import; restated in magnitudes the selector is
     * {@code max}. With {@code min} here, a house whose month-to-date peak is already 4.2 kW would be shed down to
     * the 2.5 kW floor for the rest of the month, for no billing benefit at all.
     */
    @Test
    public void theFreeBudgetIsTheLargerOfPeakAndFloorBothWays() {
        BucketPeakFee established = new BucketPeakFee(2500);
        established.commit(4200);
        assertThat(established.allowanceW(), is(4200.0));

        BucketPeakFee quietMonth = new BucketPeakFee(2500);
        quietMonth.commit(1800);
        assertThat(quietMonth.allowanceW(), is(2500.0));
    }

    /**
     * "Projection reconciled while it still matters": four minutes into a quarter, acted on now rather than
     * discovered at the boundary.
     * <p>
     * The fixture discriminates all three readings of "how much is this slot drawing". Nothing is drawn for the
     * first four minutes and 6.8 kW from then on, so the instantaneous draw is 6800, the average so far is 0, and
     * only the projection - 6800 held to the boundary, spread over the whole quarter - gives 4987.
     */
    @Test
    public void theSlotInProgressIsExtrapolatedToItsEnd() {
        CapacityTracker tracker = belgianTracker(2500);
        tracker.observe(Instant.parse("2026-09-25T05:00:00Z"), 0);
        tracker.observe(Instant.parse("2026-09-25T05:04:00Z"), -6800);

        assertThat(tracker.projectionW(), closeTo(6800.0 * 660 / 900, 0.5));
    }

    /**
     * "Estimates never become the billed number."
     * <p>
     * 12 kW for the first five minutes of a quarter and nothing after it. The projection peaked at 12 kW while that
     * load ran; the metered average for the quarter is 4 kW, and 4 kW is what the month is billed on.
     */
    @Test
    public void onlyTheMeteredAverageIsEverCommitted() {
        CapacityTracker tracker = belgianTracker(2500);
        tracker.observe(Instant.parse("2026-09-25T05:00:00Z"), -12000);
        assertThat(tracker.projectionW(), closeTo(12000, 0.5));

        tracker.observe(Instant.parse("2026-09-25T05:05:00Z"), 0);
        tracker.observe(Instant.parse("2026-09-25T05:15:30Z"), 0);

        assertThat(tracker.fee().billedPeakW(), closeTo(4000, 1.0));
    }

    /** "Rollover at the supplier's boundary": the quarter commits and a fresh one starts. */
    @Test
    public void crossingTheBoundaryCommitsAndStartsAgain() {
        CapacityTracker tracker = belgianTracker(0);
        tracker.observe(Instant.parse("2026-09-25T05:00:00Z"), -8000);
        tracker.observe(Instant.parse("2026-09-25T05:16:00Z"), -1000);

        assertThat(tracker.fee().billedPeakW(), closeTo(8000, 1.0));
        assertThat(tracker.slotStart(), is(Instant.parse("2026-09-25T05:15:00Z")));
        assertThat(tracker.projectionW(), closeTo(1000, 1.0));
    }

    /** A new billing month discards the old month's peak rather than carrying it. */
    @Test
    public void aNewMonthStartsItsOwnPeak() {
        CapacityTracker tracker = belgianTracker(2500);
        tracker.observe(Instant.parse("2026-09-25T05:00:00Z"), -22600);
        tracker.observe(Instant.parse("2026-09-25T05:16:00Z"), -1000);
        assertThat(tracker.fee().billedPeakW(), closeTo(22600, 1.0));

        tracker.observe(Instant.parse("2026-10-01T05:00:00Z"), -1000);
        assertThat(tracker.fee().billedPeakW(), is(2500.0));
    }

    /** "A site that configures nothing still has a margin." */
    @Test
    public void theShippedMarginIsThreeHundredWatts() {
        BucketPeakFee fee = new BucketPeakFee(2500);
        fee.commit(4200);

        assertThat(CapacityBudget.DEFAULT_SHED_MARGIN_W, is(300.0));
        assertThat(CapacityBudget.withDefaultMargin(fee).shedThresholdW(), is(3900.0));
    }

    /** "Shed margin is a stated configuration value." */
    @Test
    public void aConfiguredMarginIsTheOneApplied() {
        BucketPeakFee fee = new BucketPeakFee(2500);
        fee.commit(4200);

        CapacityBudget budget = new CapacityBudget(fee, 800);
        assertThat(budget.shedThresholdW(), is(3400.0));
        assertThat(budget.shouldShed(3500), is(true));
        assertThat(budget.shouldShed(3300), is(false));
        assertThat(budget.overshootW(3500), closeTo(100, 1e-9));
        assertThat(budget.overshootW(3300), is(0.0));
    }

    /**
     * "Avoiding a tier jump": under a top-N-average fee, a slot below the current Nth-highest displaces nothing and
     * leaves the mean alone, while one above it lifts the mean and can cross a tier.
     */
    @Test
    public void underATopNFeeOnlySlotsAboveTheNthHighestCost() {
        TopNAverageFee fee = new TopNAverageFee(3, 0);
        fee.commit(9000);
        fee.commit(7000);
        fee.commit(5000);

        assertThat(fee.billedPeakW(), closeTo(7000, 1e-9));
        assertThat(fee.allowanceW(), closeTo(5000, 1e-9));

        fee.commit(4000);
        assertThat(fee.billedPeakW(), closeTo(7000, 1e-9));

        fee.commit(6000);
        assertThat(fee.billedPeakW(), closeTo((9000 + 7000 + 6000) / 3.0, 1e-9));
    }

    /** Before N slots exist nothing above the floor is free, and the model says so rather than inventing a budget. */
    @Test
    public void aTopNFeeOffersNoFreeBudgetBeforeItHasNSlots() {
        TopNAverageFee fee = new TopNAverageFee(3, 2500);
        fee.commit(9000);
        fee.commit(7000);

        assertThat(fee.allowanceW(), is(2500.0));
        fee.commit(5000);
        assertThat(fee.allowanceW(), is(5000.0));
    }

    /** The sign of a reading is discarded: a bill is not signed, and the grid convention is about measurement. */
    @Test
    public void theSignOfAReadingIsNotTheSignOfABill() {
        CapacityTracker importNegative = belgianTracker(0);
        importNegative.observe(Instant.parse("2026-09-25T05:00:00Z"), -5000);

        CapacityTracker importPositive = belgianTracker(0);
        importPositive.observe(Instant.parse("2026-09-25T05:00:00Z"), 5000);

        assertThat(importNegative.projectionW(), is(importPositive.projectionW()));
        assertThat(importNegative.projectionW(), closeTo(5000, 1e-9));
    }
}
