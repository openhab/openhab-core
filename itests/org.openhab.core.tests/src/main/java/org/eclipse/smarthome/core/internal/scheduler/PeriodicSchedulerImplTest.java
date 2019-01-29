/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.internal.scheduler;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.scheduler.ScheduledCompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link PeriodicSchedulerImpl}.
 * Because the test run on scheduler all tests are guarded by a timeout to avoid having a test blocking.
 *
 * @author Peter Kriens - initial contribution and API
 * @author Simon Kaufmann - adapted to Java 8
 * @author Hilbrand Bouwkamp - moved periodic scheduling to it's own interface, rewritten test not to use sleep
 */
public class PeriodicSchedulerImplTest {
    private final PeriodicSchedulerImpl periodicScheduler = new PeriodicSchedulerImpl();
    private final SchedulerImpl scheduler = new SchedulerImpl();

    @Before
    public void setUp() {
        periodicScheduler.setScheduler(scheduler);
    }

    @After
    public void tearDown() {
        periodicScheduler.unsetScheduler(scheduler);
    }

    @Test(timeout = 5000)
    public void testSchedule() throws InterruptedException, IOException {
        Queue<Long> times = new ArrayDeque<>();
        Semaphore semaphore = new Semaphore(0);
        Duration[] delays = { Duration.ofMillis(100), Duration.ofMillis(200), Duration.ofMillis(300) };
        final long now = System.currentTimeMillis();

        ScheduledCompletableFuture<Object> future = periodicScheduler.schedule(() -> {
            times.add((System.currentTimeMillis() - now) / 100);
            semaphore.release();
        }, delays);
        semaphore.acquire(6);
        future.cancel(true);
        // Because starting scheduler takes some time and we don't know how long
        // the first time set is the offset on which we check the next values.
        long offset = times.poll();
        long[] expectedResults = { 2, 5, 8, 11, 14 };
        for (int i = 0; i < expectedResults.length; i++) {
            assertEquals("Expected periodic time", offset + expectedResults[i], times.poll().longValue());
        }
        assertFalse("No more jobs should have been scheduled", semaphore.tryAcquire(1, TimeUnit.SECONDS));
    }
}
