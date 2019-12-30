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

import static org.junit.Assert.*;

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

import org.junit.Test;
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

    @Test(expected = CancellationException.class, timeout = 500)
    public void testAfterCancelled() throws InterruptedException, InvocationTargetException, ExecutionException {
        AtomicBoolean check = new AtomicBoolean();
        Callable<Boolean> callable = () -> check.getAndSet(true);
        ScheduledCompletableFuture<Boolean> after = scheduler.after(callable, Duration.ofMillis(200_0000));
        after.cancel(true);
        Thread.sleep(100);
        assertTrue("Scheduled job cancelled before timeout", after.isCancelled());
        Thread.sleep(200);
        assertFalse("Callable method should not been called", check.get());
        after.get();
    }

    @Test(timeout = 300)
    public void testAfterResolved() throws InterruptedException, ExecutionException {
        AtomicInteger check = new AtomicInteger();
        Callable<Integer> callable = () -> {
            check.set(10);
            return 5;
        };
        ScheduledCompletableFuture<Integer> after = scheduler.after(callable, Duration.ofMillis(100));
        Thread.sleep(200);
        assertTrue("Scheduled job should finish done", after.isDone());
        assertEquals("After CompletableFuture should return correct value", 10, check.get());
        assertEquals("After CompletableFuture should return correct value", 5, after.get().intValue());
    }

    @Test(expected = FileNotFoundException.class, timeout = 300)
    public void testAfterResolvedWithException() throws Throwable {
        Callable<Void> callable = () -> {
            // Pass a exception not very likely thrown by the scheduler it self to avoid missing real exceptions.
            throw new FileNotFoundException("testBeforeTimeoutException");
        };
        ScheduledCompletableFuture<Void> after = scheduler.after(callable, Duration.ofMillis(100));
        Thread.sleep(200);
        assertTrue("Scheduled job should be done", after.isDone());
        assertTrue("After CompletableFuture should have completed with an exception",
                after.getPromise().isCompletedExceptionally());
        try {
            after.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = TimeoutException.class, timeout = 300)
    public void testBeforeTimeoutException() throws Throwable {
        CompletableFuture<Integer> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Integer> before = scheduler.before(d, Duration.ofMillis(100));
        Thread.sleep(200);
        d.complete(3);
        d.get();
        assertTrue("Scheduled job should be done", before.isDone());
        assertTrue("Before CompletableFuture should have completed with an exception",
                before.getPromise().isCompletedExceptionally());
        try {
            before.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = CancellationException.class, timeout = 300)
    public void testBeforeCancelled() throws InterruptedException, InvocationTargetException, ExecutionException {
        CompletableFuture<Integer> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Integer> before = scheduler.before(d, Duration.ofMillis(100));
        before.cancel(true);
        assertTrue("Scheduled job cancelled before timeout", before.getPromise().isCompletedExceptionally());
        before.get();
    }

    @Test(timeout = 300)
    public void testBeforeResolved() throws InterruptedException, ExecutionException {
        CompletableFuture<Boolean> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Boolean> before = scheduler.before(d, Duration.ofMillis(100));
        d.complete(Boolean.TRUE);
        assertTrue("Scheduled job should finish done", before.isDone());
        assertTrue("Before CompletableFuture should return correct value", before.get());
    }

    @Test(expected = FileNotFoundException.class, timeout = 300)
    public void testBeforeResolvedWithException() throws Throwable {
        CompletableFuture<Integer> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Integer> before = scheduler.before(d, Duration.ofMillis(100));
        // Pass an exception not very likely thrown by the scheduler it self to avoid missing real exceptions.
        d.completeExceptionally(new FileNotFoundException("testBeforeTimeoutException"));
        assertTrue("Scheduled job should be done", before.isDone());
        assertTrue("Before CompletableFuture should have completed with an exception",
                before.getPromise().isCompletedExceptionally());
        try {
            before.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = TimeoutException.class, timeout = 300)
    public void testAfterTimeoutException() throws Throwable {
        CompletableFuture<Integer> d = new CompletableFuture<>();
        ScheduledCompletableFuture<Integer> before = scheduler.before(d, Duration.ofMillis(100));
        Thread.sleep(200);
        d.complete(3);
        d.get();
        assertTrue("Scheduled job should be done", before.isDone());
        assertTrue("Before CompletableFuture should have completed with an exception",
                before.getPromise().isCompletedExceptionally());
        try {
            before.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(timeout = 1000)
    public void testSchedule() throws InterruptedException {
        Semaphore s = new Semaphore(0);
        TestSchedulerTemporalAdjuster temporalAdjuster = new TestSchedulerTemporalAdjuster();
        scheduler.schedule(s::release, temporalAdjuster);
        s.acquire(3);
        Thread.sleep(300); // wait a little longer to see if not more are scheduled.
        assertEquals("Scheduler should not have released more after done", 0, s.availablePermits());
        assertEquals("Scheduler should have run 3 times", 3, temporalAdjuster.getCount());
    }

    @Test(timeout = 1000)
    public void testScheduleCancel() throws InterruptedException {
        Semaphore s = new Semaphore(0);
        TestSchedulerTemporalAdjuster temporalAdjuster = new TestSchedulerTemporalAdjuster();
        ScheduledCompletableFuture<Void> schedule = scheduler.schedule(s::release, temporalAdjuster);
        s.acquire(1);
        Thread.sleep(50);
        schedule.cancel(true);
        Thread.sleep(300); // wait a little longer to see if not more are scheduled.
        assertEquals("Scheduler should not have released more after cancel", 0, s.availablePermits());
        assertEquals("Scheduler should have run 1 time", 1, temporalAdjuster.getCount());
    }

    @Test(timeout = 1000)
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
        assertEquals("Scheduler should not have released more after cancel", 0, s.availablePermits());
        assertEquals("Scheduler should have run 0 time", 0, temporalAdjuster.getCount());
    }

    @Test(timeout = 300)
    public void testNegative() throws InterruptedException {
        Semaphore s = new Semaphore(0);
        scheduler.after(() -> {
            s.release(1);
            return null;
        }, Duration.ofMillis(-1000));
        Thread.sleep(200);
        assertEquals("Negative value should mean after finished right away", 1, s.availablePermits());
    }

    @Test(timeout = 1000)
    public void testDelay() throws InterruptedException {
        long duration = 5000;
        ScheduledCompletableFuture<Instant> future = scheduler.after(Duration.ofMillis(duration));
        Thread.sleep(500);
        long timeLeft = future.getDelay(TimeUnit.MILLISECONDS);
        long expectedTimeLeft = duration - 450; // add some slack so don't check at exact time
        assertTrue("Delay should be less:" + timeLeft + " < " + expectedTimeLeft, timeLeft < expectedTimeLeft);
        future.cancel(true);
    }

    @Test(timeout = 1000)
    public void testCompareTo() throws InterruptedException {
        long duration = 5000;
        ScheduledCompletableFuture<Instant> future1 = scheduler.after(Duration.ofMillis(duration));
        Thread.sleep(500);
        ScheduledCompletableFuture<Instant> future2 = scheduler.after(Duration.ofMillis(duration));
        int compare = future1.compareTo(future2);
        assertTrue("First future should be less than second", compare < 0);
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
