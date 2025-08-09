/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.internal.common;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps the ScheduledThreadPoolExecutor to implement the {@link #afterExecute(Runnable, Throwable)} method
 * and log the exception in case the scheduled runnable threw an exception. The error will otherwise go unnoticed
 * because an exception thrown in the runnable will simply end with no logging unless the user handles it. This
 * wrapper removes the burden for the user to always catch errors in scheduled runnables for logging, and it also
 * catches unchecked exceptions that can be the cause of very hard to catch bugs because no error is ever shown if the
 * user doesn't catch the error in the runnable itself.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public class WrappedScheduledExecutorService extends ScheduledThreadPoolExecutor {

    final Logger logger = LoggerFactory.getLogger(WrappedScheduledExecutorService.class);

    public WrappedScheduledExecutorService(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    /**
     * A base class that checks the time a scheduled task takes to complete, and if it takes too long,
     * it outputs a log message with the stack trace from whence the task was originally scheduled.
     */
    private abstract class TimedAbstractTask {
        private static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(5000);

        private final Exception stackTraceHolder;
        private Instant timeout;

        protected TimedAbstractTask() {
            this.stackTraceHolder = new Exception();
            this.timeout = Instant.MAX;
        }

        protected void clockStart() {
            timeout = Instant.now().plus(DEFAULT_TIMEOUT);
        }

        protected void clockStop() {
            if (Instant.now().isAfter(timeout)) {
                logger.debug("Scheduled task took more than {}; it was created here: ", DEFAULT_TIMEOUT,
                        stackTraceHolder);
            }
        }
    }

    /**
     * A running time checker for a {@link Runnable}
     */
    private class TimedRunnable extends TimedAbstractTask implements Runnable {
        private final Runnable runnable;

        protected TimedRunnable(@Nullable Runnable runnable) {
            super();
            this.runnable = Objects.requireNonNull(runnable);
        }

        @Override
        public void run() {
            try {
                clockStart();
                runnable.run();
            } finally {
                clockStop();
            }
        }
    }

    /**
     * A running time checker for a {@link Callable}
     */
    private class TimedCallable<V> extends TimedAbstractTask implements Callable<V> {
        private final Callable<V> callable;

        protected TimedCallable(@Nullable Callable<V> callable) {
            super();
            this.callable = Objects.requireNonNull(callable);
        }

        @Override
        public V call() throws Exception {
            try {
                clockStart();
                return callable.call();
            } finally {
                clockStop();
            }
        }
    }

    @Override
    protected void afterExecute(@Nullable Runnable r, @Nullable Throwable t) {
        super.afterExecute(r, t);
        Throwable actualThrowable = t;
        if (actualThrowable == null && r instanceof Future<?> f) {
            // The Future is the wrapper task around our scheduled Runnable. This is only "done" if an Exception
            // occurred, the Task was completed, or aborted. A periodic Task (scheduleWithFixedDelay etc.) is NEVER
            // "done" unless there was an Exception because the outer Task is always rescheduled.
            if (f.isDone()) {
                try {
                    // we are NOT interested in the result of the Future but we have to call get() to obtain a possible
                    // Exception from it
                    f.get();
                } catch (CancellationException ce) {
                    // ignore canceled tasks
                } catch (ExecutionException ee) {
                    actualThrowable = ee.getCause();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (actualThrowable != null) {
            logger.warn("Scheduled runnable ended with an exception: ", actualThrowable);
        }
    }

    @Override
    public ScheduledFuture<?> schedule(@Nullable Runnable runnable, long delay, @Nullable TimeUnit unit) {
        return super.schedule(new TimedRunnable(runnable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nullable Runnable runnable, long initialDelay, long period,
            @Nullable TimeUnit unit) {
        return super.scheduleAtFixedRate(new TimedRunnable(runnable), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nullable Runnable runnable, long initialDelay, long delay,
            @Nullable TimeUnit unit) {
        return super.scheduleWithFixedDelay(new TimedRunnable(runnable), initialDelay, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nullable Callable<V> callable, long delay, @Nullable TimeUnit unit) {
        return super.schedule(new TimedCallable<V>(callable), delay, unit);
    }
}
