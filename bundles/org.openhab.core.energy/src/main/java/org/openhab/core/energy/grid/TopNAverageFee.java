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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A tiered fee on the average of the N highest slots of a period: the Norwegian top-three-hours archetype.
 * <p>
 * Where the bucket archetype is billed on one slot, this is billed on a mean, which changes what a planner can do
 * with it. A single expensive hour no longer sets the bill on its own, so one bad hour is cheaper here than there;
 * but it is also never free, because every hour above the current Nth-highest displaces that one and lifts the mean.
 * That is what the "avoiding a tier jump" trade is made against.
 * <p>
 * Until the period has recorded N slots there is no free budget above the tariff's floor, and the model says so
 * rather than inventing one: with fewer than N slots in hand, any further draw enters the mean.
 *
 * @author Stamate Viorel - Initial contribution
 */
@NonNullByDefault
public class TopNAverageFee implements PeakFeeModel {

    private final int slotsAveraged;
    private final double minimumBillableDemandW;
    private final List<Double> highest = new ArrayList<>();

    /**
     * @param slotsAveraged how many of the period's highest slots the fee averages. Norway bills on three.
     * @param minimumBillableDemandW the tariff's minimum billable demand as an unsigned magnitude in watts, or zero
     * @throws IllegalArgumentException if the count is not positive, or the floor is negative or not finite
     */
    public TopNAverageFee(int slotsAveraged, double minimumBillableDemandW) {
        if (slotsAveraged < 1) {
            throw new IllegalArgumentException("slotsAveraged must be at least 1 but was " + slotsAveraged);
        }
        if (!Double.isFinite(minimumBillableDemandW) || minimumBillableDemandW < 0) {
            throw new IllegalArgumentException(
                    "minimumBillableDemandW must be a finite magnitude but was " + minimumBillableDemandW);
        }
        this.slotsAveraged = slotsAveraged;
        this.minimumBillableDemandW = minimumBillableDemandW;
    }

    @Override
    public double billedPeakW() {
        if (highest.isEmpty()) {
            return minimumBillableDemandW;
        }
        double sum = 0;
        for (double v : highest) {
            sum += v;
        }
        return Math.max(sum / highest.size(), minimumBillableDemandW);
    }

    @Override
    public double allowanceW() {
        // Only once N slots are in hand is anything free: below the Nth-highest a slot displaces nothing, so the
        // mean is unchanged. Before that every slot enters the mean, and the floor is all there is.
        if (highest.size() < slotsAveraged) {
            return minimumBillableDemandW;
        }
        return Math.max(highest.get(highest.size() - 1), minimumBillableDemandW);
    }

    @Override
    public void commit(double slotAverageW) {
        if (!Double.isFinite(slotAverageW)) {
            return;
        }
        highest.add(Math.abs(slotAverageW));
        Collections.sort(highest, Collections.reverseOrder());
        while (highest.size() > slotsAveraged) {
            highest.remove(highest.size() - 1);
        }
    }

    @Override
    public void startPeriod() {
        highest.clear();
    }

    /** The period's highest slots, largest first, at most as many as the fee averages. */
    public List<Double> recordedHighestW() {
        return List.copyOf(highest);
    }
}
