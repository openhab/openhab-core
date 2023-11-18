/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.types;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.time.Instant;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DecimalType;

/**
 * The {@link TimeSeriesTest} contains tests for {@link TimeSeries}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TimeSeriesTest {

    @Test
    public void testAdditionOrderDoesNotMatter() {
        Instant time1 = Instant.now();
        Instant time2 = time1.plusSeconds(1000);
        Instant time3 = time1.minusSeconds(1000);
        Instant time4 = time1.plusSeconds(50);

        TimeSeries timeSeries = new TimeSeries(TimeSeries.Policy.ADD);
        assertThat(timeSeries.getPolicy(), is(TimeSeries.Policy.ADD));

        timeSeries.add(time1, new DecimalType(time1.toEpochMilli()));
        timeSeries.add(time2, new DecimalType(time2.toEpochMilli()));
        timeSeries.add(time3, new DecimalType(time3.toEpochMilli()));
        timeSeries.add(time4, new DecimalType(time4.toEpochMilli()));

        assertThat(timeSeries.size(), is(4));

        // assert begin end time
        assertThat(timeSeries.getBegin(), is(time3));
        assertThat(timeSeries.getEnd(), is(time2));

        // assert order of events and content
        List<TimeSeries.Entry> entries = timeSeries.getStates().toList();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                // assert order
                assertThat(entries.get(i).timestamp(), is(greaterThan(entries.get(i - 1).timestamp())));
            }
            assertThat(entries.get(i).timestamp().toEpochMilli(),
                    is(entries.get(i).state().as(DecimalType.class).longValue()));
        }
    }
}
