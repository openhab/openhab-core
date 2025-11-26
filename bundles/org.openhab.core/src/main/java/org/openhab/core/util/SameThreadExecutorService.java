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
package org.openhab.core.util;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link ExecutorService} implementation that runs all tasks in the calling thread in order to enable deterministic
 * testing.
 * <p>
 * <b>Not for use outside tests</b>
 *
 * @author David Pace - Initial contribution
 * @author Ravi Nadahar - Adapted for more general use
 */
@NonNullByDefault
public class SameThreadExecutorService extends AbstractExecutorService implements ScheduledExecutorService {

    protected volatile boolean terminated;

    @Override
    public void shutdown() {
        terminated = true;
    }

    @Override
    public @NonNullByDefault({}) List<Runnable> shutdownNow() {
        terminated = true;
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        return terminated;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nullable TimeUnit unit) throws InterruptedException {
        return terminated;
    }

    @Override
    public void execute(@Nullable Runnable command) {
        Objects.requireNonNull(command);
        command.run();
    }

    @Override
    public <T> Future<T> submit(@Nullable Callable<T> task) {
        Objects.requireNonNull(task);
        FutureTask<T> future = new FutureTask<T>(task);
        future.run();
        return future;
    }

    @Override
    public Future<?> submit(@Nullable Runnable task) {
        Objects.requireNonNull(task);
        FutureTask<?> future = new FutureTask<>(task, null);
        future.run();
        return future;
    }

    @Override
    public <T> Future<T> submit(@Nullable Runnable task, @Nullable T result) {
        Objects.requireNonNull(task);
        FutureTask<T> future = new FutureTask<>(task, result);
        future.run();
        return future;
    }

    /**
     * <b>Not supported</b> if {@code delay} is non-zero.
     * <p>
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code command} is {@code null}.
     * @throws UnsupportedOperationException if {@code delay} is non-zero.
     */
    @Override
    public ScheduledFuture<?> schedule(@Nullable Runnable command, long delay, @Nullable TimeUnit unit) {
        Objects.requireNonNull(command);
        if (delay == 0L) {
            @NonNullByDefault({})
            FutureTask<Void> future = new FutureTask<>(command, null);
            future.run();
            return new ImpostorScheduledFuture<Void>(future);
        }
        throw new UnsupportedOperationException("Delayed schedule not supported by SameThreadExecutorService");
    }

    /**
     * <b>Not supported</b> if {@code delay} is non-zero.
     * <p>
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code callable} is {@code null}.
     * @throws UnsupportedOperationException if {@code delay} is non-zero.
     */
    @Override
    public <V> ScheduledFuture<V> schedule(@Nullable Callable<V> callable, long delay, @Nullable TimeUnit unit) {
        Objects.requireNonNull(callable);
        if (delay == 0L) {
            FutureTask<V> future = new FutureTask<V>(callable);
            future.run();
            return new ImpostorScheduledFuture<>(future);
        }
        throw new UnsupportedOperationException("Delayed schedule not supported by SameThreadExecutorService");
    }

    /**
     * <b>Will execute immediately and run only once.</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nullable Runnable task, long initialDelay, long period,
            @Nullable TimeUnit unit) {
        Objects.requireNonNull(task);
        @NonNullByDefault({})
        FutureTask<Void> future = new FutureTask<>(task, null);
        future.run();
        return new ImpostorScheduledFuture<Void>(future);
    }

    /**
     * <b>Will execute immediately and run only once.</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nullable Runnable task, long initialDelay, long delay,
            @Nullable TimeUnit unit) {
        Objects.requireNonNull(task);
        @NonNullByDefault({})
        FutureTask<Void> future = new FutureTask<>(task, null);
        future.run();
        return new ImpostorScheduledFuture<Void>(future);
    }

    /**
     * A {@link ScheduledFuture} that wraps a {@link Future} and always reports zero delay.
     *
     * @param <T> the return type.
     *
     * @author Ravi Nadahar - Initial contribution
     */
    protected static class ImpostorScheduledFuture<T> implements ScheduledFuture<T> {

        /** The wrapped {@link Future} */
        protected final Future<T> delegate;

        /**
         * Creates a new instance that wraps the specified {@link Future}.
         *
         * @param future the {@link Future} to wrap.
         */
        public ImpostorScheduledFuture(Future<T> future) {
            this.delegate = future;
        }

        @Override
        public long getDelay(@Nullable TimeUnit unit) {
            return 0L;
        }

        @Override
        @SuppressWarnings("PMD.CompareObjectsWithEquals")
        public int compareTo(@Nullable Delayed other) {
            if (this == other) {
                return 0;
            }
            if (other == null) {
                return -1;
            }
            long diff = 0L - other.getDelay(TimeUnit.NANOSECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return delegate.get();
        }

        @Override
        public T get(long timeout, @Nullable TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout, unit);
        }
    }
}
