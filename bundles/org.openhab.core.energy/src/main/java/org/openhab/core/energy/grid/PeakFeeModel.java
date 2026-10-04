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
 * A grid fee that depends on demand peaks rather than on energy taken.
 * <p>
 * Two archetypes are documented in openhab-core issue #3478 and both are implemented here: the billed maximum of
 * fixed-size power buckets over a period - the Belgian 15-minute monthly peak - and a tiered fee on the average of
 * the N highest slots of a period, which is the Norwegian top-three-hours form. They differ in how a slot enters the
 * bill, but a planner asks both the same two questions, which is why they share this type.
 * <p>
 * <strong>Everything here is an unsigned import magnitude in watts.</strong> The signed grid convention that
 * {@code energy-participants} fixes, where import is negative, is a convention about a measurement; a bill is not
 * signed. Mixing the two inverts comparisons silently - a {@code min} over negative watts selects the larger import -
 * so the whole of this package works in magnitudes and converts at the edge.
 *
 * @author Stamate Viorel - Initial contribution
 */
@NonNullByDefault
public interface PeakFeeModel {

    /**
     * What this period would be billed on if it ended now, as an unsigned import magnitude in watts.
     */
    double billedPeakW();

    /**
     * The largest average the next metering slot may draw without increasing what the period is billed on.
     * <p>
     * This is the free budget, and it is never below the tariff's minimum billable demand: under that floor every
     * peak is billed identically, so shaving one buys nothing and costs the user comfort for no billing benefit.
     */
    double allowanceW();

    /**
     * Takes a closed metering slot into the period.
     *
     * @param slotAverageW the slot's metered average, an unsigned import magnitude in watts
     */
    void commit(double slotAverageW);

    /** Starts a new billing period, discarding what the previous one accumulated. */
    void startPeriod();
}
