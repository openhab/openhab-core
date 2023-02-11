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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openhab.core.scheduler.CronJob;
import org.openhab.core.scheduler.ScheduledCompletableFuture;

/**
 * Test class for {@link CronSchedulerImpl}.
 * Because the test run on scheduler all tests are guarded by a timeout to avoid having a test blocking.
 *
 * @author Peter Kriens - Initial contribution
 * @author Simon Kaufmann - adapted to Java 8
 * @author Hilbrand Bouwkamp - moved cron scheduling to it's own class
 */
@NonNullByDefault
public class CronSchedulerImplTest {
    private final CronSchedulerImpl cronScheduler = new CronSchedulerImpl(new SchedulerImpl());

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testCronReboot() throws InterruptedException {
        Instant start = Instant.now();
        Semaphore s = new Semaphore(0);
        ScheduledCompletableFuture<@Nullable Void> future = cronScheduler.schedule(() -> {
        }, "@reboot");
        future.getPromise().thenAccept(x -> s.release());
        s.acquire(1);

        Duration duration = Duration.between(start, Instant.now());
        Duration maxDuration = Duration.ofSeconds(2);
        assertTrue(duration.compareTo(maxDuration) < 0,
                "Reboot call should occur within " + maxDuration + " but was called after: " + duration);
        assertTrue(future.isDone(), "Scheduler should be done once reboot call done.");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    public void testCronScheduling() throws InterruptedException {
        Instant start = Instant.now();
        AtomicReference<Object> ref = new AtomicReference<>();

        Semaphore s = new Semaphore(0);
        cronScheduler.schedule(foo -> {
            s.release();
            ref.set(Objects.requireNonNull(foo.get("foo")));
        }, Map.of("foo", "bar"), "#\n" //
                + "\n" //
                + " foo = bar \n" //
                + "# bla bla foo=foo\n" //
                + "*/5 * * * * *");
        s.acquire(2);

        Duration duration = Duration.between(start, Instant.now());

        // The call should occur every 5 seconds.
        // So the fastest execution of 2 calls would be immediately and after 5 seconds.
        Duration minDuration = Duration.ofSeconds(5);

        // The slowest execution of 2 calls would be after 2*5 seconds.
        // When the load is high, it can be a bit slower, so we account for this by adding another 2 seconds.
        Duration maxDuration = Duration.ofSeconds(12);

        assertTrue(minDuration.compareTo(duration) < 0 && duration.compareTo(maxDuration) < 0,
                "The two calls should be executed between " + minDuration + " and " + maxDuration
                        + " but the total duration was: " + duration);
        assertEquals("bar", ref.get(), "Environment variable 'foo' should be correctly set");
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    public void testAddRemoveScheduler() throws InterruptedException {
        Semaphore s = new Semaphore(0);
        CronJob cronJob = m -> s.release();
        Map<String, Object> map = Map.of(CronJob.CRON, "* * * * * *");
        cronScheduler.addSchedule(cronJob, map);
        s.acquire();
        cronScheduler.removeSchedule(cronJob);
    }
}
