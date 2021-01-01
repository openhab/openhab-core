/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.scheduler.SchedulerRunnable;
import org.openhab.core.scheduler.SchedulerTemporalAdjuster;

/**
 * Test class for {@link SchedulerImpl}.
 * Because the test run on scheduler all tests are guarded by a timeout to avoid having a test blocking.
 *
 * @author Peter Kriens - Initial contribution
 * @author Simon Kaufmann - adapted to Java 8
 * @author Hilbrand Bouwkamp - moved cron and periodic scheduling to it's their own interfaces
 */
public class SchedulerImplTest {
    private SchedulerImpl scheduler = new SchedulerImpl();

    @Test
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    public void testAfterCancelled() throws InterruptedException, InvocationTargetException, ExecutionException {
        AtomicBoolean check = new AtomicBoolean();
        Callable<Boolean> callable = () -> check.getAndSet(true);
        ScheduledCompletableFuture<Boolean> after = scheduler.after(callable, Duration.ofMillis(200_0000));
        after.cancel(true);
        Thread.sleep(100);
        assertTrue(after.isCancelled(), "Scheduled job cancelled before timeout");
        Thread.sleep(200);
        assertFalse(check.get(), "Callable method should not been called");
        assertThrows(CancellationException.class, () -> after.get());
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    public void testAfterResolved() throws InterruptedException, ExecutionException {
        AtomicInteger check = new AtomicInteger();
        Callable<Integer> callable = () -> {
            check.set(10);
            return 5;
        };
        ScheduledCompletableFuture<Integer> after = scheduler.after(callable, Duration.ofMillis(100));
        Thread.sleep(200);
        assertTrue(after.isDone(), "Scheduled job should finish done");
        assertEquals(10, check.get(), "After CompletableFuture should return correct value");
        assertEquals(5, after.get().intValue(), "After CompletableFuture should return correct value");
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    public void testAfterResolvedWithException() throws Throwable {
        Callable<Void> callable = () -> {
            // Pass a exception not very likely thrown by the scheduler it self to avoid missing real exceptions.
            throw new FileNotFoundException("testBeforeTimeoutException");
        };
        ScheduledCompletableFuture<Void> after = scheduler.after(callable, Duration.ofMillis(100));
        Thread.sleep(200);
        assertTrue(after.isDone(), "Scheduled job should be done");
        assertTrue(after.getPromise().isCompletedExceptionally(),
                "After CompletableFuture should have completed with an exception");

        assertThrows(FileNotFoundException.class, () -> {
            try {
                after.get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    public void testBeforeTimeoutException() throws Throwable {
        CompletableFuture<Integer> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Integer> before = scheduler.before(d, Duration.ofMillis(100));
        Thread.sleep(200);
        d.complete(3);
        d.get();
        assertTrue(before.isDone(), "Scheduled job should be done");
        assertTrue(before.getPromise().isCompletedExceptionally(),
                "Before CompletableFuture should have completed with an exception");
        assertThrows(TimeoutException.class, () -> {
            try {
                before.get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    public void testBeforeCancelled() throws InterruptedException, InvocationTargetException, ExecutionException {
        CompletableFuture<Integer> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Integer> before = scheduler.before(d, Duration.ofMillis(100));
        before.cancel(true);
        assertTrue(before.getPromise().isCompletedExceptionally(), "Scheduled job cancelled before timeout");
        assertThrows(CancellationException.class, () -> {
            before.get();
        });
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    public void testBeforeResolved() throws InterruptedException, ExecutionException {
        CompletableFuture<Boolean> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Boolean> before = scheduler.before(d, Duration.ofMillis(100));
        d.complete(Boolean.TRUE);
        assertTrue(before.isDone(), "Scheduled job should finish done");
        assertTrue(before.get(), "Before CompletableFuture should return correct value");
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    public void testBeforeResolvedWithException() throws Throwable {
        CompletableFuture<Integer> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Integer> before = scheduler.before(d, Duration.ofMillis(100));
        // Pass an exception not very likely thrown by the scheduler it self to avoid missing real exceptions.
        d.completeExceptionally(new FileNotFoundException("testBeforeTimeoutException"));
        assertTrue(before.isDone(), "Scheduled job should be done");
        assertTrue(before.getPromise().isCompletedExceptionally(),
                "Before CompletableFuture should have completed with an exception");
        assertThrows(FileNotFoundException.class, () -> {
            try {
                before.get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    public void testAfterTimeoutException() throws Throwable {
        CompletableFuture<Integer> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Integer> before = scheduler.before(d, Duration.ofMillis(100));
        Thread.sleep(200);
        d.complete(3);
        d.get();
        assertTrue(before.isDone(), "Scheduled job should be done");
        assertTrue(before.getPromise().isCompletedExceptionally(),
                "Before CompletableFuture should have completed with an exception");
        assertThrows(TimeoutException.class, () -> {
            try {
                before.get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    public void testSchedule() throws InterruptedException {
        Semaphore s = new Semaphore(0);
        TestSchedulerTemporalAdjuster temporalAdjuster = new TestSchedulerTemporalAdjuster();
        scheduler.schedule(s::release, temporalAdjuster);
        s.acquire(3);
        Thread.sleep(300); // wait a little longer to see if not more are scheduled.
        assertEquals(0, s.availablePermits(), "Scheduler should not have released more after done");
        assertEquals(3, temporalAdjuster.getCount(), "Scheduler should have run 3 times");
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    public void testScheduleCancel() throws InterruptedException {
        Semaphore s = new Semaphore(0);
        TestSchedulerTemporalAdjuster temporalAdjuster = new TestSchedulerTemporalAdjuster();
        ScheduledCompletableFuture<Void> schedule = scheduler.schedule(s::release, temporalAdjuster);
        s.acquire(1);
        Thread.sleep(50);
        schedule.cancel(true);
        Thread.sleep(300); // wait a little longer to see if not more are scheduled.
        assertEquals(0, s.availablePermits(), "Scheduler should not have released more after cancel");
        assertEquals(1, temporalAdjuster.getCount(), "Scheduler should have run 1 time");
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    public void testScheduleException() throws InterruptedException {
        Semaphore s = new Semaphore(0);
        TestSchedulerTemporalAdjuster temporalAdjuster = new TestSchedulerTemporalAdjuster();
        SchedulerRunnable runnable = () -> {
            // Pass a exception not very likely thrown by the scheduler it self to avoid missing real exceptions.
            throw new FileNotFoundException("testBeforeTimeoutException");
        };

        ScheduledCompletableFuture<Void> schedule = scheduler.schedule(runnable, temporalAdjuster);
        schedule.getPromise().exceptionally(e -> {
            if (e instanceof FileNotFoundException) {
                s.release();
            }
            return null;
        });
        s.acquire(1);
        Thread.sleep(300); // wait a little longer to see if not more are scheduled.
        assertEquals(0, s.availablePermits(), "Scheduler should not have released more after cancel");
        assertEquals(0, temporalAdjuster.getCount(), "Scheduler should have run 0 time");
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.MILLISECONDS)
    public void testNegative() throws InterruptedException {
        Semaphore s = new Semaphore(0);
        scheduler.after(() -> {
            s.release(1);
            return null;
        }, Duration.ofMillis(-1000));
        Thread.sleep(200);
        assertEquals(1, s.availablePermits(), "Negative value should mean after finished right away");
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    public void testDelay() throws InterruptedException {
        long duration = 5000;
        ScheduledCompletableFuture<Instant> future = scheduler.after(Duration.ofMillis(duration));
        Thread.sleep(500);
        long timeLeft = future.getDelay(TimeUnit.MILLISECONDS);
        long expectedTimeLeft = duration - 450; // add some slack so don't check at exact time
        assertTrue(timeLeft < expectedTimeLeft, "Delay should be less:" + timeLeft + " < " + expectedTimeLeft);
        future.cancel(true);
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    public void testCompareTo() throws InterruptedException {
        long duration = 5000;
        ScheduledCompletableFuture<Instant> future1 = scheduler.after(Duration.ofMillis(duration));
        Thread.sleep(500);
        ScheduledCompletableFuture<Instant> future2 = scheduler.after(Duration.ofMillis(duration));
        int compare = future1.compareTo(future2);
        assertTrue(compare < 0, "First future should be less than second");
        future1.cancel(true);
        future2.cancel(true);
    }

    private final class TestSchedulerTemporalAdjuster implements SchedulerTemporalAdjuster {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Temporal adjustInto(Temporal arg0) {
            return arg0.plus(100, ChronoUnit.MILLIS);
        }

        @Override
        public boolean isDone(Temporal temporal) {
            counter.incrementAndGet();
            return counter.get() >= 3;
        }

        public int getCount() {
            return counter.get();
        }
    }
}
