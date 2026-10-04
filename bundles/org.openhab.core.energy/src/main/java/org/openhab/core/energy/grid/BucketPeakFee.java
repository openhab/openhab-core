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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The billed maximum of fixed-size power buckets over a period: the Belgian capacity tariff's archetype.
 * <p>
 * Every closed metering slot is a candidate; the period is billed on the largest of them, floored by the tariff's
 * minimum billable demand. That floor is the half of this model that a naive reading misses, and it changes
 * behaviour in both directions. Below it, peaks are billed identically, so a planner that shaves towards zero spends
 * the user's comfort on nothing. Above it, the month-to-date peak is already paid for, so the rest of the month may
 * be scheduled freely up to that level - the established peak is a budget, not a line to creep away from.
 *
 * @author Stamate Viorel - Initial contribution
 */
@NonNullByDefault
public class BucketPeakFee implements PeakFeeModel {

    private final double minimumBillableDemandW;

    private double periodPeakW;

    /**
     * @param minimumBillableDemandW the tariff's minimum billable demand as an unsigned magnitude in watts, below
     *            which every peak is billed the same. Belgium's is 2.5 kW. Pass zero for a tariff that has none.
     * @throws IllegalArgumentException if the floor is negative or not finite
     */
    public BucketPeakFee(double minimumBillableDemandW) {
        if (!Double.isFinite(minimumBillableDemandW) || minimumBillableDemandW < 0) {
            throw new IllegalArgumentException(
                    "minimumBillableDemandW must be a finite magnitude but was " + minimumBillableDemandW);
        }
        this.minimumBillableDemandW = minimumBillableDemandW;
    }

    @Override
    public double billedPeakW() {
        return Math.max(periodPeakW, minimumBillableDemandW);
    }

    @Override
    public double allowanceW() {
        // the larger of the two, in both directions: an established peak is already paid for, and under the floor
        // there is nothing to shave
        return Math.max(periodPeakW, minimumBillableDemandW);
    }

    @Override
    public void commit(double slotAverageW) {
        if (!Double.isFinite(slotAverageW)) {
            return;
        }
        periodPeakW = Math.max(periodPeakW, Math.abs(slotAverageW));
    }

    @Override
    public void startPeriod() {
        periodPeakW = 0;
    }

    /** The largest slot this period has actually recorded, before the minimum billable demand is applied. */
    public double recordedPeakW() {
        return periodPeakW;
    }
}
