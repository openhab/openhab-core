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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The supplier's metering intervals, as wall-clock boundaries in the site's own zone.
 * <p>
 * A capacity tariff bills the average over a fixed interval that starts at a wall-clock boundary - :00, :15, :30 and
 * :45 for a quarter-hour tariff - so this aligns to the local clock and not to an offset from some epoch. The
 * distinction is not pedantic. A trailing window of the same width slides across those boundaries and averages across
 * two of the supplier's intervals at once, so it can report a peak the meter never recorded and miss one it did. No
 * rolling form is offered here, deliberately: a rolling average never matches a bill.
 * <p>
 * Alignment happens in local time rather than on the instant, which is what makes a daylight-saving change behave.
 * The clock still reads :00, :15, :30 and :45 through the transition, so the quarters keep landing where the meter
 * puts them, and the one quarter that is shorter or longer in absolute time is the one the meter also treats that way.
 *
 * @param zone the site's zone, as core's {@code TimeZoneProvider} reports it
 * @param slotLength the supplier's metering interval
 *
 * @author Stamate Viorel - Initial contribution
 */
@NonNullByDefault
public record MeteringSlots(ZoneId zone, Duration slotLength) {

    private static final Duration HOUR = Duration.ofHours(1);

    /**
     * Validates the geometry.
     *
     * @throws IllegalArgumentException if the slot length is not a positive whole number of minutes that divides an
     *             hour exactly. Anything else cannot land on a repeating wall-clock boundary, which is the only
     *             alignment a meter uses.
     */
    public MeteringSlots {
        if (slotLength.isZero() || slotLength.isNegative()) {
            throw new IllegalArgumentException("slotLength must be positive but was " + slotLength);
        }
        if (slotLength.toSecondsPart() != 0 || slotLength.toNanosPart() != 0) {
            throw new IllegalArgumentException("slotLength must be a whole number of minutes but was " + slotLength);
        }
        if (HOUR.toMinutes() % slotLength.toMinutes() != 0) {
            throw new IllegalArgumentException(
                    "slotLength must divide an hour exactly but was " + slotLength.toMinutes() + " minutes");
        }
    }

    /** The quarter-hour geometry of the Belgian capacity tariff, and of every other 15-minute metered market. */
    public static MeteringSlots quarterHourly(ZoneId zone) {
        return new MeteringSlots(zone, Duration.ofMinutes(15));
    }

    /** The start of the metering interval containing {@code at}, inclusive. */
    public Instant startOf(Instant at) {
        ZonedDateTime local = at.atZone(zone).truncatedTo(ChronoUnit.MINUTES);
        long minutes = slotLength.toMinutes();
        return local.withMinute((int) (local.getMinute() / minutes * minutes)).toInstant();
    }

    /** The exclusive end of the metering interval containing {@code at}, which is the next interval's start. */
    public Instant endOf(Instant at) {
        return startOf(at).plus(slotLength);
    }

    /** Whether two instants fall in the same metering interval, which is what a rollover check asks. */
    public boolean sameSlot(Instant a, Instant b) {
        return startOf(a).equals(startOf(b));
    }
}
