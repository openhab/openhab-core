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

/**
 * <strong>Grid fees that depend on demand peaks rather than on energy taken.</strong> A capacity tariff bills a
 * household on the worst quarter of hour it had all month, which makes the shape of consumption cost money
 * independently of its quantity - and makes it something a planner can act on.
 * <p>
 * The two archetypes documented in openhab-core issue #3478 are both here:
 * {@link org.openhab.core.energy.grid.BucketPeakFee} for the billed maximum of fixed buckets, which is the Belgian
 * 15-minute monthly peak, and {@link org.openhab.core.energy.grid.TopNAverageFee} for a tiered fee on the mean of the
 * N highest slots, which is the Norwegian top-three-hours form. They answer the same two questions through
 * {@link org.openhab.core.energy.grid.PeakFeeModel}: what the period is billed on, and what the next slot may draw
 * for free. {@link org.openhab.core.energy.grid.MeteringSlots} puts the boundaries where the supplier's clock puts
 * them, {@link org.openhab.core.energy.grid.CapacityTracker} integrates and projects the slot in progress, and
 * {@link org.openhab.core.energy.grid.CapacityBudget} turns the free budget into a threshold to act on.
 * <p>
 * <strong>Why it is in this bundle.</strong> It is arithmetic over a meter reading: no Item is read, none is written,
 * and nothing here knows what openHAB is. That is the placement rule the corpus settled as D40 - arithmetic in the
 * engine, anything touching openHAB's own data surfaces in an opt-in companion - and a region's actual tariff
 * parameters, which change on regulatory timetables rather than openHAB's, are configuration fed in from outside
 * rather than constants compiled in here.
 * <p>
 * <strong>Everything is an unsigned import magnitude in watts.</strong> The signed grid convention where import is
 * negative belongs to measurements; a bill has no sign. Carrying signed values into a fee model inverts its
 * comparisons silently, because a {@code min} over negative watts selects the larger import - a mistake already made
 * once in this corpus's own decision record and corrected there.
 *
 * @author Stamate Viorel - Initial contribution
 */
@org.eclipse.jdt.annotation.NonNullByDefault
package org.openhab.core.energy.grid;
