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
package org.openhab.core.internal.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openhab.core.scheduler.ScheduledCompletableFuture;

/**
 * Test class for {@link PeriodicSchedulerImpl}.
 * Because the test run on scheduler all tests are guarded by a timeout to avoid having a test blocking.
 *
 * @author Peter Kriens - Initial contribution
 * @author Simon Kaufmann - adapted to Java 8
 * @author Hilbrand Bouwkamp - moved periodic scheduling to it's own interface, rewritten test not to use sleep
 */
@NonNullByDefault
public class PeriodicSchedulerImplTest {
    private final PeriodicSchedulerImpl periodicScheduler = new PeriodicSchedulerImpl(new SchedulerImpl());

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testSchedule() throws InterruptedException, IOException {
        Queue<Long> times = new ArrayDeque<>();
        Semaphore semaphore = new Semaphore(0);
        Duration[] delays = { Duration.ofMillis(100), Duration.ofMillis(200), Duration.ofMillis(400) };
        final long now = System.currentTimeMillis();

        ScheduledCompletableFuture<Object> future = periodicScheduler.schedule(() -> {
            times.add(System.currentTimeMillis() - now);
            semaphore.release();
        }, delays);
        semaphore.acquire(6);
        future.cancel(true);
        // Because starting scheduler takes some time and we don't know how long
        // the first time set is the offset on which we check the next values.
        long offset = times.poll().longValue();
        long[] expectedResults = { 200, 400, 400, 400, 400 };
        for (long expectedResult : expectedResults) {
            long actualValue = times.poll().longValue();
            assertEquals((offset + expectedResult) / 100.0, actualValue / 100.0, 1.5,
                    "Expected periodic time, total: " + actualValue);
            offset = actualValue;
        }
        assertFalse(semaphore.tryAcquire(1, TimeUnit.SECONDS), "No more jobs should have been scheduled");
    }
}
