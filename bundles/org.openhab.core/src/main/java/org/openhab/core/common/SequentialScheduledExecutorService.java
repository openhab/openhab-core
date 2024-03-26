/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

/**
 * A ScheduledExecutorService that will sequentially perform the tasks like a
 * {@link Executors#newSingleThreadScheduledExecutor} backed by a thread pool.
 * This is a drop in replacement to a ScheduledExecutorService with one thread to avoid a lot of threads created, idling
 * most of the time and wasting memory on low-end devices.
 *
 * The mechanism to block the ScheduledExecutorService to run tasks concurrently is based on a chain of
 * {@link CompletableFuture}s.
 * Each instance has a reference to the last CompletableFuture and will call handleAsync to add a new task.
 *
 * @author Jörg Sautter - Initial contribution
 */
class SequentialScheduledExecutorService implements ScheduledExecutorService {

    private final WorkQueueEntry empty;
    private final ScheduledThreadPoolExecutor pool;
    private final List<RunnableFuture<?>> scheduled;
    private final ScheduledFuture<?> cleaner;
    private WorkQueueEntry tail;

    public SequentialScheduledExecutorService(ScheduledThreadPoolExecutor pool) {
        if (pool.getMaximumPoolSize() != Integer.MAX_VALUE) {
            throw new IllegalArgumentException("the pool must scale unlimited to avoid potential dead locks!");
        }

        this.pool = pool;

        // prepare the WorkQueueEntry we are using when no tasks are pending
        RunnableCompletableFuture<?> future = new RunnableCompletableFuture();
        future.complete(null);
        empty = new WorkQueueEntry(null, null, future);

        // tracks scheduled tasks alive
        this.scheduled = new ArrayList<>();

        tail = empty;

        // clean up to ensure we do not keep references to old tasks
        cleaner = this.scheduleAtFixedRate(() -> {
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
        }, 2, 4, TimeUnit.SECONDS);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        synchronized (this) {
            if (tail == null) {
                throw new RejectedExecutionException("this scheduled executor has been shutdown before");
            }

            CompletableFuture<RunnableFuture<?>> origin = new CompletableFuture<>();
            ScheduledFuture<?> future = pool.schedule(() -> {
                // we block the thread here, in worst case new threads are spawned
                submit0(origin.join(), command).join();
            }, delay, unit);

            scheduled.add((RunnableFuture<?>) future);
            origin.complete((RunnableFuture<?>) future);

            return future;
        }
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        synchronized (this) {
            if (tail == null) {
                throw new RejectedExecutionException("this scheduled executor has been shutdown before");
            }

            CompletableFuture<RunnableFuture<?>> origin = new CompletableFuture<>();
            ScheduledFuture<V> future = pool.schedule(() -> {
                // we block the thread here, in worst case new threads are spawned
                return submit0(origin.join(), callable).join();
            }, delay, unit);

            scheduled.add((RunnableFuture<?>) future);
            origin.complete((RunnableFuture<?>) future);

            return future;
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        synchronized (this) {
            if (tail == null) {
                throw new RejectedExecutionException("this scheduled executor has been shutdown before");
            }

            CompletableFuture<RunnableFuture<?>> origin = new CompletableFuture<>();
            ScheduledFuture<?> future = pool.scheduleAtFixedRate(() -> {
                CompletableFuture<?> submitted;

                try {
                    // we block the thread here, in worst case new threads are spawned
                    submitted = submit0(origin.join(), command);
                } catch (RejectedExecutionException ex) {
                    // the pool has been shutdown, scheduled tasks should cancel
                    return;
                }

                submitted.join();
            }, initialDelay, period, unit);

            scheduled.add((RunnableFuture<?>) future);
            origin.complete((RunnableFuture<?>) future);

            return future;
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        synchronized (this) {
            if (tail == null) {
                throw new RejectedExecutionException("this scheduled executor has been shutdown before");
            }

            CompletableFuture<RunnableFuture<?>> origin = new CompletableFuture<>();
            ScheduledFuture<?> future = pool.scheduleWithFixedDelay(() -> {
                CompletableFuture<?> submitted;

                try {
                    // we block the thread here, in worst case new threads are spawned
                    submitted = submit0(origin.join(), command);
                } catch (RejectedExecutionException ex) {
                    // the pool has been shutdown, scheduled tasks should cancel
                    return;
                }

                submitted.join();
            }, initialDelay, delay, unit);

            scheduled.add((RunnableFuture<?>) future);
            origin.complete((RunnableFuture<?>) future);

            return future;
        }
    }

    @Override
    public void shutdown() {
        synchronized (this) {
            cleaner.cancel(false);
            scheduled.removeIf((sf) -> {
                sf.cancel(false);
                return true;
            });
            tail = null;
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        synchronized (this) {
            if (tail == null) {
                return List.of();
            }

            // ensures we do not leak the internal cleaner as Runnable
            cleaner.cancel(false);

            Set<Runnable> runnables = Collections.newSetFromMap(new IdentityHashMap<>());
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
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
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
    public <T> Future<T> submit(Callable<T> task) {
        return submit0(null, task);
    }

    private CompletableFuture<?> submit0(RunnableFuture<?> origin, Runnable task) {
        Callable<?> callable = () -> {
            task.run();

            return null;
        };

        return submit0(origin, callable);
    }

    private <T> CompletableFuture<T> submit0(RunnableFuture<?> origin, Callable<T> task) {
        BiFunction<? super Object, Throwable, T> action = (result, error) -> {
            // ignore result & error, they are from the previous task
            try {
                return task.call();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                // a small hack to throw the Exception unchecked
                throw SequentialScheduledExecutorService.unchecked(ex);
            }
        };

        synchronized (this) {
            if (tail == null) {
                throw new RejectedExecutionException("this scheduled executor has been shutdown before");
            }

            RunnableCompletableFuture<T> cf = tail.future.handleAsync(action, pool);

            cf.setCallable(task);

            tail = new WorkQueueEntry(tail, origin, cf);

            return cf;
        }
    }

    private static <E extends RuntimeException> E unchecked(Exception ex) throws E {
        throw (E) ex;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return submit0(null, () -> {
            task.run();

            return result;
        });
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, (Void) null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
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
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        List<Future<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            futures.add(submit0(null, task).orTimeout(timeout, unit));
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
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<CompletableFuture<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            futures.add(submit0(null, task));
        }

        // wait for any future to complete
        while (true) {
            boolean allDone = true;

            for (CompletableFuture<T> future : futures) {
                if (future.isDone()) {
                    if (!future.isCompletedExceptionally()) {
                        // stop the others
                        for (CompletableFuture<T> toLate : futures) {
                            if (toLate != future) {
                                toLate.cancel(true);
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
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long timeoutAt = System.currentTimeMillis() + unit.toMillis(timeout);
        List<CompletableFuture<T>> futures = new ArrayList<>();

        for (Callable<T> task : tasks) {
            futures.add(submit0(null, task));
        }

        // wait for any future to complete
        while (timeoutAt >= System.currentTimeMillis()) {
            boolean allDone = true;

            for (CompletableFuture<T> future : futures) {
                if (future.isDone()) {
                    if (!future.isCompletedExceptionally()) {
                        // stop the others
                        for (CompletableFuture<T> toLate : futures) {
                            if (toLate != future) {
                                toLate.cancel(true);
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

        for (CompletableFuture<T> toLate : futures) {
            toLate.cancel(true);
        }

        throw new TimeoutException("none of the tasks did complete in time");
    }

    @Override
    public void execute(Runnable command) {
        submit(command);
    }

    static class WorkQueueEntry {
        private WorkQueueEntry prev;
        private RunnableFuture<?> origin;
        private final RunnableCompletableFuture<?> future;

        public WorkQueueEntry(WorkQueueEntry prev, RunnableFuture<?> origin, RunnableCompletableFuture<?> future) {
            this.prev = prev;
            this.origin = origin;
            this.future = future;
        }
    }

    static class RunnableCompletableFuture<V> extends CompletableFuture<V> implements RunnableFuture<V> {
        private Callable<V> callable;

        public RunnableCompletableFuture() {
        }

        public void setCallable(Callable<V> callable) {
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
