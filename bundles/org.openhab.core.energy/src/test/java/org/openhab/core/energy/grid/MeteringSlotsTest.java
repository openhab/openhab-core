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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * {@code grid-constraints} <em>Metering slots follow the supplier's clock</em>.
 * <p>
 * The requirement is as much about what this must never be - a rolling window over the trailing interval - as about
 * what it is, because the two agree on the average and disagree on where a slot begins, and only one of them matches
 * a bill.
 *
 * @author Stamate Viorel - Initial contribution
 */
@NonNullByDefault
public class MeteringSlotsTest {

    private static final ZoneId BRUSSELS = ZoneId.of("Europe/Brussels");

    /** The requirement's own scenario: the running average commits at :15 and a fresh quarter starts. */
    @Test
    public void aQuarterBeginsOnTheSuppliersBoundaryAndNotWhereTheReadingStarted() {
        MeteringSlots slots = MeteringSlots.quarterHourly(BRUSSELS);

        // 07:07:30 local is inside the quarter that began at 07:00, whatever time the first reading arrived
        Instant insideTheQuarter = Instant.parse("2026-09-25T05:07:30Z");
        assertThat(slots.startOf(insideTheQuarter), is(Instant.parse("2026-09-25T05:00:00Z")));
        assertThat(slots.endOf(insideTheQuarter), is(Instant.parse("2026-09-25T05:15:00Z")));

        // and :15 is a different slot, not a window that has slid 15 minutes along
        assertThat(slots.sameSlot(insideTheQuarter, Instant.parse("2026-09-25T05:15:00Z")), is(false));
        assertThat(slots.sameSlot(insideTheQuarter, Instant.parse("2026-09-25T05:14:59Z")), is(true));
    }

    /**
     * Alignment is in local wall clock, so the quarters keep landing on :00/:15/:30/:45 through a daylight-saving
     * change. Aligning on the instant instead would put them at :30 past for half the year in a zone with a
     * half-hour offset, and would move them under a transition.
     */
    @Test
    public void theBoundariesFollowTheWallClockThroughADaylightSavingChange() {
        MeteringSlots slots = MeteringSlots.quarterHourly(BRUSSELS);

        // 2026-10-25 03:20 local, after Brussels falls back from CEST to CET
        Instant afterTheChange = Instant.parse("2026-10-25T02:20:00Z");
        assertThat(slots.startOf(afterTheChange).atZone(BRUSSELS).getMinute(), is(15));
        assertThat(slots.startOf(afterTheChange).atZone(BRUSSELS).getHour(), is(3));
    }

    /** A length that cannot land on a repeating wall-clock boundary is rejected rather than quietly drifting. */
    @Test
    public void onlyLengthsThatDivideAnHourAreAccepted() {
        assertThrows(IllegalArgumentException.class, () -> new MeteringSlots(BRUSSELS, Duration.ofMinutes(7)));
        assertThrows(IllegalArgumentException.class, () -> new MeteringSlots(BRUSSELS, Duration.ofSeconds(90)));
        assertThrows(IllegalArgumentException.class, () -> new MeteringSlots(BRUSSELS, Duration.ZERO));
        assertThat(new MeteringSlots(BRUSSELS, Duration.ofMinutes(30)).slotLength(), is(Duration.ofMinutes(30)));
        assertThat(new MeteringSlots(BRUSSELS, Duration.ofMinutes(60)).slotLength(), is(Duration.ofMinutes(60)));
    }
}
