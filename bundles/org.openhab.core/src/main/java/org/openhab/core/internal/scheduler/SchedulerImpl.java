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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.scheduler.ScheduledCompletableFuture;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.scheduler.SchedulerRunnable;
import org.openhab.core.scheduler.SchedulerTemporalAdjuster;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link Scheduler}.
 *
 * @author Peter Kriens - Initial contribution
 * @author Simon Kaufmann - ported to CompletableFuture
 * @author Hilbrand Bouwkamp - improved implementation and moved cron and periodic to own implementations.
 */
@Component(service = SchedulerImpl.class, immediate = true)
@NonNullByDefault
public class SchedulerImpl implements Scheduler {

    private static final String SCHEDULER_THREAD_POOL = "scheduler";
    private static final int ALLOWED_DEVIATION_MILLISECONDS = 2000;

    private final Logger logger = LoggerFactory.getLogger(SchedulerImpl.class);

    private final ScheduledExecutorService executor = ThreadPoolManager.getScheduledPool(SCHEDULER_THREAD_POOL);

    @Override
    public ScheduledCompletableFuture<Instant> after(Duration duration) {
        final Instant start = Instant.now();
        return after(() -> start, duration);
    }

    @Override
    public <T> ScheduledCompletableFuture<T> after(Callable<T> callable, Duration duration) {
        return afterInternal(new ScheduledCompletableFutureOnce<>(null, duration), callable);
    }

    private <T> ScheduledCompletableFutureOnce<T> afterInternal(ScheduledCompletableFutureOnce<T> deferred,
            Callable<T> callable) {
        final long duration = Math.max(100,
                deferred.getScheduledTime().minus(currentTimeMillis(), ChronoUnit.MILLIS).toInstant().toEpochMilli());
        final ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                final long timeLeft = deferred.getDelay(TimeUnit.MILLISECONDS);

                if (timeLeft > ALLOWED_DEVIATION_MILLISECONDS) {
                    logger.trace("Scheduled task is re-scheduled because the scheduler ran {} milliseconds to early.",
                            timeLeft);
                    afterInternal(deferred, callable);
                } else {
                    logger.trace("Scheduled task is run now.");
                    deferred.complete(callable.call());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.warn("Scheduled job '{}' failed and stopped",
                        Objects.requireNonNullElse(deferred.identifier, "<unknown>"), e);
                deferred.completeExceptionally(e);
            }
        }, duration, TimeUnit.MILLISECONDS);
        if (logger.isTraceEnabled()) {
            logger.trace("Scheduled a task to run in {} seconds.", TimeUnit.MILLISECONDS.toSeconds(duration));
        }
        deferred.exceptionally(e -> {
            logger.trace("Scheduled task stopped with exception ", e);
            if (e instanceof CancellationException) {
                future.cancel(true);
            }
            return null;
        });
        return deferred;
    }

    @Override
    public <T> ScheduledCompletableFuture<T> before(CompletableFuture<T> promise, Duration timeout) {
        final AtomicBoolean done = new AtomicBoolean();
        final Consumer<Runnable> runOnce = runnable -> {
            if (!done.getAndSet(true)) {
                runnable.run();
            }
        };
        final ScheduledCompletableFutureOnce<T> wrappedPromise = new ScheduledCompletableFutureOnce<>(null, timeout);
        Callable<T> callable = () -> {
            wrappedPromise.completeExceptionally(new TimeoutException());
            return null;
        };
        final ScheduledCompletableFutureOnce<T> afterPromise = afterInternal(wrappedPromise, callable);
        wrappedPromise.exceptionally(e -> {
            if (e instanceof CancellationException) {
                // Also cancel the scheduled timer if returned completable future is cancelled.
                afterPromise.cancel(true);
            }
            return null;
        });

        promise.thenAccept(p -> runOnce.accept(() -> wrappedPromise.complete(p))) //
                .exceptionally(ex -> {
                    runOnce.accept(() -> wrappedPromise.completeExceptionally(ex));
                    return null;
                });
        return wrappedPromise;
    }

    @Override
    public ScheduledCompletableFuture<Instant> at(Instant instant) {
        return at(() -> instant, instant);
    }

    @Override
    public <T> ScheduledCompletableFuture<T> at(Callable<T> callable, Instant instant) {
        return atInternal(
                new ScheduledCompletableFutureOnce<>(null, ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())),
                callable);
    }

    private <T> ScheduledCompletableFuture<T> atInternal(ScheduledCompletableFutureOnce<T> deferred,
            Callable<T> callable) {
        return afterInternal(deferred, callable);
    }

    @Override
    public <T> ScheduledCompletableFuture<T> schedule(SchedulerRunnable runnable, TemporalAdjuster temporalAdjuster) {
        return schedule(runnable, null, temporalAdjuster);
    }

    @Override
    public <T> ScheduledCompletableFuture<T> schedule(SchedulerRunnable runnable, @Nullable String identifier,
            TemporalAdjuster temporalAdjuster) {
        final ScheduledCompletableFutureRecurring<T> schedule = new ScheduledCompletableFutureRecurring<>(identifier,
                ZonedDateTime.now());
        schedule(schedule, runnable, identifier, temporalAdjuster);
        return schedule;
    }

    private <T> void schedule(ScheduledCompletableFutureRecurring<T> recurringSchedule, SchedulerRunnable runnable,
            @Nullable String identifier, TemporalAdjuster temporalAdjuster) {
        final Temporal newTime = recurringSchedule.getScheduledTime().with(temporalAdjuster);
        final ScheduledCompletableFutureOnce<T> deferred = new ScheduledCompletableFutureOnce<>(identifier,
                ZonedDateTime.from(newTime));

        deferred.thenAccept(v -> {
            if (temporalAdjuster instanceof SchedulerTemporalAdjuster) {
                final SchedulerTemporalAdjuster schedulerTemporalAdjuster = (SchedulerTemporalAdjuster) temporalAdjuster;

                if (!schedulerTemporalAdjuster.isDone(newTime)) {
                    schedule(recurringSchedule, runnable, identifier, temporalAdjuster);
                    return;
                }
            }
            recurringSchedule.complete(v);
        });
        recurringSchedule.setScheduledPromise(deferred);
        atInternal(deferred, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * {@link ScheduledCompletableFuture} that is intended to keep track of jobs that only run recurring.
     * Calling get() on this class will only return if the job is stopped or if the related scheduler
     * determines the job is done.
     *
     * @param <T> Data the job returns when finished
     */
    private static class ScheduledCompletableFutureRecurring<T> extends ScheduledCompletableFutureOnce<T> {
        private @Nullable volatile ScheduledCompletableFuture<T> scheduledPromise;
        private @Nullable String identifier;

        public ScheduledCompletableFutureRecurring(@Nullable String identifier, ZonedDateTime scheduledTime) {
            super(identifier, scheduledTime);
            exceptionally(e -> {
                synchronized (this) {
                    if (e instanceof CancellationException) {
                        if (scheduledPromise != null) {
                            scheduledPromise.cancel(true);
                        }
                    }
                }
                return null;
            });
        }

        void setScheduledPromise(ScheduledCompletableFuture<T> future) {
            synchronized (this) {
                if (isCancelled()) {
                    // if already cancelled stop the new future directly.
                    future.cancel(true);
                } else {
                    scheduledPromise = future;
                    scheduledPromise.getPromise().exceptionally(ex -> {
                        // if an error occurs in the scheduled job propagate to parent
                        ScheduledCompletableFutureRecurring.this.completeExceptionally(ex);
                        return null;
                    });
                }
            }
        }

        @Override
        public long getDelay(@Nullable TimeUnit timeUnit) {
            return scheduledPromise != null ? scheduledPromise.getDelay(timeUnit) : 0;
        }

        @Override
        public ZonedDateTime getScheduledTime() {
            return scheduledPromise != null ? scheduledPromise.getScheduledTime() : super.getScheduledTime();
        }
    }

    /**
     * {@link ScheduledCompletableFuture} that is intended to keep track of jobs that only run once.
     *
     * @param <T> Data the job returns when finished.
     */
    private static class ScheduledCompletableFutureOnce<T> extends CompletableFuture<T>
            implements ScheduledCompletableFuture<T> {
        private ZonedDateTime scheduledTime;
        private @Nullable String identifier;

        public ScheduledCompletableFutureOnce(@Nullable String identifier, Duration duration) {
            this(identifier, ZonedDateTime.now().plusNanos(duration.toNanos()));
        }

        public ScheduledCompletableFutureOnce(@Nullable String identifier, ZonedDateTime scheduledTime) {
            this.identifier = identifier;
            this.scheduledTime = scheduledTime;
        }

        @Override
        public CompletableFuture<T> getPromise() {
            return this;
        }

        @Override
        public long getDelay(@Nullable TimeUnit timeUnit) {
            ZonedDateTime scheduledTime = this.scheduledTime;
            if (timeUnit == null) {
                return 0;
            }
            long remaining = scheduledTime.toInstant().toEpochMilli() - System.currentTimeMillis();

            return timeUnit.convert(remaining, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@Nullable Delayed timeUnit) {
            return timeUnit == null ? -1
                    : Long.compare(getDelay(TimeUnit.MILLISECONDS), timeUnit.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public ZonedDateTime getScheduledTime() {
            return scheduledTime;
        }
    }

    /**
     * Wraps the system call to get the current time to be able to manipulate it in a unit test.
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
