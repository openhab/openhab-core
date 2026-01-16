/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.common.WrappedScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ScheduledExecutorService that will sequentially perform the tasks like a
 * {@link java.util.concurrent.Executors#newSingleThreadScheduledExecutor} backed by a thread pool.
 * This is a drop in replacement to a ScheduledExecutorService with one thread to avoid a lot of threads created, idling
 * most of the time and wasting memory on low-end devices.
 *
 * The mechanism to block the ScheduledExecutorService to run tasks concurrently is based on a chain of
 * {@link CompletableFuture}s.
 * Each instance has a reference to the last CompletableFuture and will call handleAsync to add a new task.
 *
 * @author Jörg Sautter - Initial contribution
 */
@NonNullByDefault
final class PoolBasedSequentialScheduledExecutorService implements ScheduledExecutorService {

    static class BasePoolExecutor extends WrappedScheduledExecutorService {

        protected final Logger logger = LoggerFactory.getLogger(BasePoolExecutor.class);

        private final String threadPoolName;
        private final AtomicInteger pending;
        private volatile int minimumPoolSize;

        public BasePoolExecutor(String threadPoolName, int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);

            this.threadPoolName = threadPoolName;
            // set to one does ensure at least one thread more than tasks running
            this.pending = new AtomicInteger(1);
        }

        public synchronized void resizePool(int mandatoryPoolSize) {
            int corePoolSize = getCorePoolSize();

            if (minimumPoolSize > mandatoryPoolSize) {
                mandatoryPoolSize = minimumPoolSize;
            }

            if (mandatoryPoolSize > corePoolSize) {
                // two more than needed, they will time out if there is no work for them im time
                setMaximumPoolSize(mandatoryPoolSize + 2);
                setCorePoolSize(mandatoryPoolSize);
            } else if (mandatoryPoolSize < corePoolSize) {
                setCorePoolSize(mandatoryPoolSize);
                // ensure we drop not needed threads, this is only needed under higher load when none of the
                // started threads have a chance to timeout
                setMaximumPoolSize(mandatoryPoolSize + 2);
            }
        }

        public int getMinimumPoolSize() {
            return minimumPoolSize;
        }

        public void setMinimumPoolSize(int minimumPoolSize) {
            this.minimumPoolSize = minimumPoolSize;

            resizePool(getCorePoolSize());
        }

        @Override
        public void shutdown() {
            logger.warn("shutdown() invoked on a shared thread pool '{}'. This is a bug, please submit a bug report",
                    threadPoolName, new IllegalStateException());
        }

        @Override
        @NonNullByDefault({})
        public List<Runnable> shutdownNow() {
            logger.warn("shutdownNow() invoked on a shared thread pool '{}'. This is a bug, please submit a bug report",
                    threadPoolName, new IllegalStateException());
            return List.of();
        }
    }

    private final WorkQueueEntry empty;
    private final BasePoolExecutor pool;
    private final List<RunnableFuture<?>> scheduled;
    private final ScheduledFuture<?> cleaner;
    private @Nullable WorkQueueEntry tail;

    public PoolBasedSequentialScheduledExecutorService(BasePoolExecutor pool) {
        this.pool = pool;

        // prepare the WorkQueueEntry we are using when no tasks are pending
        RunnableCompletableFuture<?> future = new RunnableCompletableFuture<>();
        future.complete(null);
        empty = new WorkQueueEntry(null, null, future);

        // tracks scheduled tasks alive
        this.scheduled = new ArrayList<>();

        tail = empty;

        // clean up to ensure we do not keep references to old tasks
        cleaner = this.scheduleWithFixedDelay(() -> {
            synchronized (this) {
                scheduled.removeIf((sf) -> sf.isCancelled());

                if (tail == null) {
                    // the service is shutdown
                    return;
                }

                WorkQueueEntry entry = tail;

                while (entry.prev != null) {
                    if (entry.prev.future.isDone()) {
                        entry.prev = null;
                        break;
                    }
                    entry = entry.prev;
                }

                if (tail != empty && tail.future.isDone()) {
                    // replace the tail with empty to ensure we do not prevent GC
                    tail = empty;
                }
            }
        },
                // avoid cleaners of promptly created instances to run at the same time
                (System.nanoTime() % 13), 8, TimeUnit.SECONDS);
    }

    @Override
    public ScheduledFuture<?> schedule(@Nullable Runnable command, long delay, @Nullable TimeUnit unit) {
        return schedule((origin) -> pool.schedule(() -> {
            // we might block the thread here, in worst case new threads are spawned
            submitToWorkQueue(origin.join(), command, true).join();
        }, delay, unit));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nullable Callable<V> callable, long delay, @Nullable TimeUnit unit) {
        return schedule((origin) -> pool.schedule(() -> {
            // we might block the thread here, in worst case new threads are spawned
            return submitToWorkQueue(origin.join(), callable, true).join();
        }, delay, unit));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nullable Runnable command, long initialDelay, long period,
            @Nullable TimeUnit unit) {
        return schedule((origin) -> pool.scheduleAtFixedRate(() -> {
            CompletableFuture<?> submitted;

            try {
                submitted = submitToWorkQueue(origin.join(), command, true);
            } catch (RejectedExecutionException ex) {
                // the pool has been shutdown, scheduled tasks should cancel
                return;
            }

            // we might block the thread here, in worst case new threads are spawned
            submitted.join();
        }, initialDelay, period, unit));
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nullable Runnable command, long initialDelay, long delay,
            @Nullable TimeUnit unit) {
        return schedule((origin) -> pool.scheduleWithFixedDelay(() -> {
            CompletableFuture<?> submitted;

            try {
                submitted = submitToWorkQueue(origin.join(), command, true);
            } catch (RejectedExecutionException ex) {
                // the pool has been shutdown, scheduled tasks should cancel
                return;
            }

            // we might block the thread here, in worst case new threads are spawned
            submitted.join();
        }, initialDelay, delay, unit));
    }

    private <V> ScheduledFuture<V> schedule(
            Function<CompletableFuture<RunnableFuture<?>>, ScheduledFuture<V>> doSchedule) {
        synchronized (this) {
            if (tail == null) {
                throw new RejectedExecutionException("this scheduled executor has been shutdown before");
            }

            CompletableFuture<RunnableFuture<?>> origin = new CompletableFuture<>();
            ScheduledFuture<V> future = doSchedule.apply(origin);

            scheduled.add((RunnableFuture<?>) future);
            origin.complete((RunnableFuture<?>) future);

            return future;
        }
    }

    @Override
    public void shutdown() {
        synchronized (this) {
            if (tail == null) {
                return;
            }

            cleaner.cancel(false);
            scheduled.removeIf((sf) -> {
                sf.cancel(false);
                return true;
            });
            tail = null;
        }
    }

    @Override
    @NonNullByDefault({})
    public List<Runnable> shutdownNow() {
        synchronized (this) {
            if (tail == null) {
                return List.of();
            }

            // ensures we do not leak the internal cleaner as Runnable
            cleaner.cancel(false);

            Set<@Nullable Runnable> runnables = Collections.newSetFromMap(new IdentityHashMap<>());
            WorkQueueEntry entry = tail;
            scheduled.removeIf((sf) -> {
                if (sf.cancel(false)) {
                    runnables.add(sf);
                }
                return true;
            });
            tail = null;

            while (entry != null) {
                if (!entry.future.cancel(false)) {
                    break;
                }

                if (entry.origin != null) {
                    // entry has been submitted by a .schedule call
                    runnables.add(entry.origin);
                } else {
                    // entry has been submitted by a .submit call
                    runnables.add(entry.future);
                }
                entry = entry.prev;
            }

            return List.copyOf(runnables);
        }
    }

    @Override
    public boolean isShutdown() {
        synchronized (this) {
            return pool == null;
        }
    }

    @Override
    public boolean isTerminated() {
        synchronized (this) {
            return pool == null && tail.future.isDone();
        }
    }

    @Override
    public boolean awaitTermination(long timeout, @Nullable TimeUnit unit) throws InterruptedException {
        long timeoutAt = System.currentTimeMillis() + unit.toMillis(timeout);

        while (!isTerminated()) {
            if (System.currentTimeMillis() > timeoutAt) {
                return false;
            }

            Thread.onSpinWait();
        }

        return true;
    }

    @Override
    public <T> Future<T> submit(@Nullable Callable<T> task) {
        return submitToWorkQueue(null, task, false);
    }

    private CompletableFuture<?> submitToWorkQueue(RunnableFuture<?> origin, @Nullable Runnable task, boolean inPool) {
        Callable<?> callable = () -> {
            task.run();

            return null;
        };

        return submitToWorkQueue(origin, callable, inPool);
    }

    private <T> CompletableFuture<T> submitToWorkQueue(@Nullable RunnableFuture<?> origin, @Nullable Callable<T> task,
            boolean inPool) {
        BiFunction<? super Object, Throwable, T> action = (result, error) -> {
            // ignore result & error, they are from the previous task
            try {
                return task.call();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                // a small hack to throw the Exception unchecked
                throw PoolBasedSequentialScheduledExecutorService.unchecked(ex);
            } finally {
                pool.pending.decrementAndGet();
            }
        };

        RunnableCompletableFuture<T> cf;
        boolean runNow;

        synchronized (this) {
            if (tail == null) {
                throw new RejectedExecutionException("this scheduled executor has been shutdown before");
            }

            var mandatoryPoolSize = pool.pending.incrementAndGet();
            pool.resizePool(mandatoryPoolSize);

            // avoid waiting for one pool thread to finish inside a pool thread
            runNow = inPool && tail.future.isDone();

            if (runNow) {
                cf = new RunnableCompletableFuture<>(task);
                tail = new WorkQueueEntry(null, origin, cf);
            } else {
                cf = tail.future.handleAsync(action, pool);
                cf.setCallable(task);
                tail = new WorkQueueEntry(tail, origin, cf);
            }
        }

        if (runNow) {
            // ensure we do not wait for one pool thread to finish inside another pool thread
            try {
                cf.run();
            } finally {
                pool.pending.decrementAndGet();
            }
        }

        return cf;
    }

    private static <E extends RuntimeException> E unchecked(Exception ex) throws E {
        throw (E) ex;
    }

    @Override
    public <T> Future<T> submit(@Nullable Runnable task, T result) {
        return submitToWorkQueue(null, () -> {
            task.run();

            return result;
        }, false);
    }

    @Override
    public Future<?> submit(@Nullable Runnable task) {
        return submit(task, (Void) null);
    }

    @Override
    @NonNullByDefault({})
    public <T> List<Future<T>> invokeAll(@Nullable Collection<? extends @Nullable Callable<T>> tasks)
            throws InterruptedException {
        List<Future<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            futures.add(submit(task));
        }

        // wait for all futures to complete
        for (Future<T> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                // ignore, we are just waiting here for the futures to complete
            }
        }

        return futures;
    }

    @Override
    @NonNullByDefault({})
    public <T> List<Future<T>> invokeAll(@Nullable Collection<? extends @Nullable Callable<T>> tasks, long timeout,
            TimeUnit unit) throws InterruptedException {
        List<Future<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            futures.add(submitToWorkQueue(null, task, false).orTimeout(timeout, unit));
        }

        // wait for all futures to complete
        for (Future<T> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                // ignore, we are just waiting here for the futures to complete
            }
        }

        return futures;
    }

    @Override
    public <T> T invokeAny(@Nullable Collection<? extends @Nullable Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        try {
            return invokeAny(tasks, Long.MAX_VALUE);
        } catch (TimeoutException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public <T> T invokeAny(@Nullable Collection<? extends @Nullable Callable<T>> tasks, long timeout,
            @Nullable TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long timeoutAt = System.currentTimeMillis() + unit.toMillis(timeout);

        return invokeAny(tasks, timeoutAt);
    }

    private <T> T invokeAny(@Nullable Collection<? extends @Nullable Callable<T>> tasks, long timeoutAt)
            throws InterruptedException, ExecutionException, TimeoutException {
        List<CompletableFuture<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            futures.add(submitToWorkQueue(null, task, false));
        }

        // wait for any future to complete
        while (timeoutAt >= System.currentTimeMillis()) {
            boolean allDone = true;

            for (CompletableFuture<T> future : futures) {
                if (future.isDone()) {
                    if (!future.isCompletedExceptionally()) {
                        // stop the others
                        for (CompletableFuture<T> tooLate : futures) {
                            if (tooLate != future) {
                                tooLate.cancel(true);
                            }
                        }

                        return future.join();
                    }
                } else {
                    allDone = false;
                }
            }

            if (allDone) {
                ExecutionException exe = new ExecutionException("all tasks failed", null);

                for (CompletableFuture<T> future : futures) {
                    try {
                        future.get();
                        throw new AssertionError("all tasks should be failed");
                    } catch (ExecutionException ex) {
                        exe.addSuppressed(ex);
                    }
                }

                throw exe;
            }

            Thread.onSpinWait();
        }

        for (CompletableFuture<T> tooLate : futures) {
            tooLate.cancel(true);
        }

        throw new TimeoutException("none of the tasks did complete in time");
    }

    @Override
    public void execute(Runnable command) {
        submit(command);
    }

    static class WorkQueueEntry {
        private @Nullable WorkQueueEntry prev;
        private @Nullable RunnableFuture<?> origin;
        private final RunnableCompletableFuture<?> future;

        public WorkQueueEntry(@Nullable WorkQueueEntry prev, @Nullable RunnableFuture<?> origin,
                RunnableCompletableFuture<?> future) {
            this.prev = prev;
            this.origin = origin;
            this.future = future;
        }
    }

    static class RunnableCompletableFuture<V> extends CompletableFuture<V> implements RunnableFuture<V> {
        private @Nullable Callable<V> callable;

        public RunnableCompletableFuture() {
            this.callable = null;
        }

        public RunnableCompletableFuture(@Nullable Callable<V> callable) {
            this.callable = callable;
        }

        public void setCallable(@Nullable Callable<V> callable) {
            this.callable = callable;
        }

        @Override
        public <U> RunnableCompletableFuture<U> newIncompleteFuture() {
            return new RunnableCompletableFuture<>();
        }

        @Override
        public <U> RunnableCompletableFuture<U> handleAsync(BiFunction<? super V, Throwable, ? extends U> fn,
                Executor executor) {
            return (RunnableCompletableFuture<U>) super.handleAsync(fn, executor);
        }

        @Override
        public void run() {
            if (this.isDone()) {
                // a FutureTask does also return here without exception
                return;
            }

            try {
                this.complete(callable.call());
            } catch (Error | Exception t) {
                this.completeExceptionally(t);
            }
        }
    }
}
