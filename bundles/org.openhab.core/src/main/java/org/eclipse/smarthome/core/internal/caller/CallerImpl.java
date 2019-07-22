/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.internal.caller;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.caller.Caller;
import org.eclipse.smarthome.core.caller.ExecutionConstraints;
import org.eclipse.smarthome.core.common.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the {@link Caller} interface.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class CallerImpl implements Caller {

    private final Logger logger = LoggerFactory.getLogger(CallerImpl.class);

    private final String id;
    private final ScheduledExecutorService watcher;
    private final ExecutorService executor;
    private final boolean ownExecutor;

    public static NamedThreadFactory newExecutorThreadFactory(final String id) {
        return new NamedThreadFactory(String.format("Caller-%s-Executor", id));
    }

    private static NamedThreadFactory newWatcherThreadFactory(final String id) {
        return new NamedThreadFactory(String.format("Caller-%s-Watcher", id));
    }

    CallerImpl(final String id, final ExecutorService executor, final boolean takeOwnershipOfExecutor) {
        this.id = id;
        this.watcher = Executors.newSingleThreadScheduledExecutor(newWatcherThreadFactory(id));
        this.executor = executor;
        this.ownExecutor = takeOwnershipOfExecutor;
    }

    @Override
    public void close() {
        if (ownExecutor) {
            executor.shutdownNow();
        }
        watcher.shutdownNow();
    }

    private <R> R execute(Supplier<R> func, final ExecutionConstraints constraint) throws Exception {
        final ScheduledFuture<?> timeoutFuture;
        if (constraint.useTimeout()) {
            timeoutFuture = watcher.schedule(constraint.onTimeout(), constraint.getTimeout(), TimeUnit.MILLISECONDS);
        } else {
            timeoutFuture = null;
        }
        try {
            final R rv = func.get();
            logger.trace("Caller \"{}\" succeeded (description: {}, rv: {})", id, rv);
            return rv;
        } catch (final Throwable ex) {
            logger.trace("Caller \"{}\" received exception (description: {})", id, ex);
            throw ex;
        } finally {
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
        }
    }

    private <R> void execAndStore(final CompletableFuture<R> cf, final Supplier<R> func,
            final ExecutionConstraints constraint) {
        try {
            cf.complete(execute(func, constraint));
        } catch (final Throwable ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            cf.completeExceptionally(ex);
        }
    }

    private <R> CompletableFuture<R> execByExecutor(Supplier<R> func, final ExecutionConstraints constraint) {
        final CompletableFuture<R> cf = new CompletableFuture<>();
        executor.submit(() -> {
            execAndStore(cf, func, constraint);
        });
        return cf;
    }

    @Override
    public <R> CompletionStage<R> exec(Supplier<R> func, final ExecutionConstraints constraint) {
        final CompletableFuture<R> cf = new CompletableFuture<>();
        execAndStore(cf, func, constraint);
        return cf;
    }

    @Override
    public <R> CompletionStage<R> execAsync(Supplier<R> func, final ExecutionConstraints constraint) {
        return execByExecutor(func, constraint);
    }

    @Override
    public <R> CompletionStage<R> execSync(Supplier<R> func, final ExecutionConstraints constraint) {
        final CompletableFuture<R> cf = execByExecutor(func, constraint);
        try {
            cf.join();
        } catch (CompletionException | CancellationException ex) {
            logger.trace("Caller \"{}\" failed (description: {})", id, ex);
        }
        return cf;
    }

}
