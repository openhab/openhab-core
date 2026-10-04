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
 * What the next metering slot may draw before a planner should start shedding.
 * <p>
 * The threshold is the fee model's free budget less a margin, and the margin exists because the projection is an
 * extrapolation: acting exactly at the budget means acting after it has already been crossed often enough to matter.
 * The shipped 300 W is a starting value a site changes, not a property of any tariff - a house with a 63 A service
 * and one with a 25 A service have no reason to share it.
 *
 * @param fee the period's fee model, which owns the free budget and its floor
 * @param shedMarginW how far below that budget to act, as an unsigned magnitude in watts
 *
 * @author Stamate Viorel - Initial contribution
 */
@NonNullByDefault
public record CapacityBudget(PeakFeeModel fee, double shedMarginW) {

    /** What ships when a site configures nothing, seeded from the reference implementation's own value. */
    public static final double DEFAULT_SHED_MARGIN_W = 300;

    /**
     * Validates the margin.
     *
     * @throws IllegalArgumentException if the margin is negative or not finite
     */
    public CapacityBudget {
        if (!Double.isFinite(shedMarginW) || shedMarginW < 0) {
            throw new IllegalArgumentException("shedMarginW must be a finite magnitude but was " + shedMarginW);
        }
    }

    /** The budget a site gets before it has configured a margin of its own. */
    public static CapacityBudget withDefaultMargin(PeakFeeModel fee) {
        return new CapacityBudget(fee, DEFAULT_SHED_MARGIN_W);
    }

    /**
     * The draw above which a planner should shed, as an unsigned magnitude in watts.
     * <p>
     * Never negative: a margin wider than the whole budget means shed everything, not owe the grid power.
     */
    public double shedThresholdW() {
        return Math.max(fee.allowanceW() - shedMarginW, 0);
    }

    /**
     * Whether a projected slot average has crossed the threshold.
     *
     * @param projectionW the projection from {@link CapacityTracker#projectionW()}, an unsigned magnitude
     */
    public boolean shouldShed(double projectionW) {
        return Double.isFinite(projectionW) && Math.abs(projectionW) > shedThresholdW();
    }

    /** How far over the threshold a projection sits, or zero when it is under. The amount a planner must find. */
    public double overshootW(double projectionW) {
        if (!Double.isFinite(projectionW)) {
            return 0;
        }
        return Math.max(Math.abs(projectionW) - shedThresholdW(), 0);
    }
}
