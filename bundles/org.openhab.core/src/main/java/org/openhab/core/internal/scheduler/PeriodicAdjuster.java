/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.internal.scheduler;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.scheduler.SchedulerTemporalAdjuster;

/**
 * This is a Temporal Adjuster that takes a list of delays.
 *
 * The given delays are used sequentially.
 * If no more values are present, the last value is re-used.
 * This scheduler runs as a fixed rate scheduler from the first
 * time adjustInto is called.
 *
 * @author Peter Kriens - Initial contribution
 * @author Hilbrand Bouwkamp - implemented adjuster as a fixed rate scheduler
 */
@NonNullByDefault
class PeriodicAdjuster implements SchedulerTemporalAdjuster {

    private final Iterator<Duration> iterator;
    private @Nullable Duration current;
    private @Nullable Temporal timeDone;

    PeriodicAdjuster(Duration... delays) {
        iterator = Arrays.stream(delays).iterator();
    }

    @Override
    public boolean isDone(Temporal temporal) {
        return false;
    }

    @Override
    public Temporal adjustInto(@Nullable Temporal temporal) {
        if (timeDone == null) {
            timeDone = temporal;
        }
        if (iterator.hasNext()) {
            current = iterator.next();
        }
        Temporal nextTime = timeDone.plus(current);
        timeDone = nextTime;
        return nextTime;
    }
}
