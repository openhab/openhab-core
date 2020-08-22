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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
public class CronSchedulerImplTest {
    private final CronSchedulerImpl cronScheduler = new CronSchedulerImpl(new SchedulerImpl());

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    public void testCronReboot() throws Exception {
        long now = System.currentTimeMillis();
        Semaphore s = new Semaphore(0);
        ScheduledCompletableFuture<Void> future = cronScheduler.schedule(() -> {
        }, "@reboot");
        future.getPromise().thenAccept(x -> s.release());
        s.acquire(1);

        long diff = System.currentTimeMillis() - now;
        assertTrue(diff < 200, "Time difference should be less 200 but was: " + diff);
        assertTrue(future.isDone(), "Scheduler should be done once reboot call done.");
    }

    @Test
    @Timeout(value = 6, unit = TimeUnit.SECONDS)
    public void testCronScheduling() throws Exception {
        long now = System.currentTimeMillis();
        AtomicReference<Object> ref = new AtomicReference<>();

        Semaphore s = new Semaphore(0);
        cronScheduler.schedule(foo -> {
            s.release();
            ref.set(foo.get("foo"));
        }, Map.of("foo", "bar"), "#\n" //
                + "\n" //
                + " foo = bar \n" //
                + "# bla bla foo=foo\n" //
                + "0/2 * * * * *");
        s.acquire(2);

        long diff = (System.currentTimeMillis() - now + 50) / 1000;
        assertTrue(diff >= 3 && diff <= 4, "Difference calculation should be between 3 and 4 but was: " + diff);
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
