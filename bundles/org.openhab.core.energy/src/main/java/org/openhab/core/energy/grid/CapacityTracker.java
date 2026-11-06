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

import java.time.Instant;
import java.time.YearMonth;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Follows the meter through its own intervals: integrates the slot in progress, projects where it will close, and
 * commits the metered average to the period's fee model at each boundary.
 * <p>
 * <strong>The projection is the part a planner acts on, and it is deliberately not the number that gets
 * billed.</strong>
 * Four minutes into a quarter the meter has not decided anything yet, but a planner that waits for the boundary finds
 * out it blew the month's peak after it has been set. So the slot in progress is extrapolated to its end at the
 * current draw and acted on now, and then thrown away: every cycle recomputes it from the meter rather than carrying
 * an estimate forward. Only the closed slot's metered average is ever committed, so no estimate can become a billed
 * quantity.
 * <p>
 * The head of a slot before the first reading is treated as having been drawn at that first reading's value, which is
 * the same assumption the projection makes about the tail. Assuming anything else - zero, most obviously - would make
 * the first projection of every slot read low exactly when a load has just started.
 *
 * @author Stamate Viorel - Initial contribution
 */
@NonNullByDefault
public class CapacityTracker {

    private final MeteringSlots slots;
    private final PeakFeeModel fee;

    private @Nullable Instant slotStart;
    private @Nullable Instant lastAt;
    private @Nullable YearMonth period;
    private double lastW;
    private double accumulatedWattSeconds;

    public CapacityTracker(MeteringSlots slots, PeakFeeModel fee) {
        this.slots = slots;
        this.fee = fee;
    }

    /**
     * Takes one grid reading.
     *
     * @param at when it was read
     * @param importW the import at that moment. The sign is discarded: a bill is not signed, and the signed grid
     *            convention is a statement about a measurement rather than about money.
     */
    public void observe(Instant at, double importW) {
        if (!Double.isFinite(importW)) {
            return;
        }
        double magnitude = Math.abs(importW);
        YearMonth nowPeriod = YearMonth.from(at.atZone(slots.zone()));
        if (!nowPeriod.equals(period)) {
            fee.startPeriod();
            period = nowPeriod;
            beginSlot(at, magnitude);
            return;
        }
        Instant start = slotStart;
        Instant previous = lastAt;
        if (start == null || previous == null) {
            beginSlot(at, magnitude);
            return;
        }
        if (!slots.sameSlot(at, start)) {
            // close the slot that just ended at the value that held through its tail, commit the metered average,
            // and start again from this reading - intervening whole slots we have no readings for are not invented
            Instant end = start.plus(slots.slotLength());
            accumulatedWattSeconds += lastW * secondsBetween(previous, end);
            fee.commit(accumulatedWattSeconds / slots.slotLength().toSeconds());
            beginSlot(at, magnitude);
            return;
        }
        accumulatedWattSeconds += lastW * secondsBetween(previous, at);
        lastAt = at;
        lastW = magnitude;
    }

    /**
     * Where the slot in progress will close if the current draw holds, as an unsigned magnitude in watts.
     *
     * @return the projection, or the last reading itself when no slot has begun
     */
    public double projectionW() {
        Instant start = slotStart;
        Instant previous = lastAt;
        if (start == null || previous == null) {
            return lastW;
        }
        Instant end = start.plus(slots.slotLength());
        double remaining = secondsBetween(previous, end);
        return (accumulatedWattSeconds + lastW * remaining) / slots.slotLength().toSeconds();
    }

    /** The fee model this tracker commits closed slots to. */
    public PeakFeeModel fee() {
        return fee;
    }

    /** The start of the metering interval currently being integrated, or null before the first reading. */
    public @Nullable Instant slotStart() {
        return slotStart;
    }

    private void beginSlot(Instant at, double magnitude) {
        Instant start = slots.startOf(at);
        slotStart = start;
        lastAt = at;
        lastW = magnitude;
        accumulatedWattSeconds = magnitude * secondsBetween(start, at);
    }

    private static double secondsBetween(Instant from, Instant to) {
        double seconds = (to.toEpochMilli() - from.toEpochMilli()) / 1000.0;
        return seconds > 0 ? seconds : 0;
    }
}
