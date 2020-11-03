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
package org.openhab.core.internal.common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the execution of safe-calls.
 *
 * It therefore tracks the executions in order to detect parallel execution and offers some helper methods for the
 * invocation handlers.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class SafeCallManagerImpl implements SafeCallManager {

    private final Logger logger = LoggerFactory.getLogger(SafeCallManagerImpl.class);

    private final Map<Object, Queue<Invocation>> queues = new HashMap<>();
    private final Map<Object, Invocation> activeIdentifiers = new HashMap<>();
    private final Map<Object, Invocation> activeAsyncInvocations = new HashMap<>();

    private final ScheduledExecutorService watcher;
    private final ExecutorService scheduler;
    private boolean enforceSingleThreadPerIdentifier;

    public SafeCallManagerImpl(ScheduledExecutorService watcher, ExecutorService scheduler,
            boolean enforceSingleThreadPerIdentifier) {
        this.watcher = watcher;
        this.scheduler = scheduler;
        this.enforceSingleThreadPerIdentifier = enforceSingleThreadPerIdentifier;
    }

    @Override
    public void recordCallStart(Invocation invocation) {
        synchronized (activeIdentifiers) {
            Invocation otherInvocation = activeIdentifiers.get(invocation.getIdentifier());
            if (enforceSingleThreadPerIdentifier && otherInvocation != null) {
                // another call to the same identifier is (still) running,
                // therefore queue it instead for async execution later on.
                // Inform the caller about the timeout by means of the exception.
                enqueue(invocation);
                throw new DuplicateExecutionException(otherInvocation);
            }
            activeIdentifiers.put(invocation.getIdentifier(), invocation);
        }
        if (invocation.getInvocationHandler() instanceof InvocationHandlerAsync) {
            watch(invocation);
        }
    }

    @Override
    public void recordCallEnd(Invocation invocation) {
        synchronized (activeIdentifiers) {
            activeIdentifiers.remove(invocation.getIdentifier());
        }
        synchronized (activeAsyncInvocations) {
            activeAsyncInvocations.remove(invocation.getIdentifier());
        }
        logger.trace("Finished {}", invocation);
        trigger(invocation.getIdentifier());
    }

    @Override
    public void enqueue(Invocation invocation) {
        synchronized (queues) {
            Queue<Invocation> queue = queues.get(invocation.getIdentifier());
            if (queue == null) {
                queue = new LinkedList<>();
                queues.put(invocation.getIdentifier(), queue);
            }
            queue.add(invocation);
        }
        trigger(invocation.getIdentifier());
    }

    private void trigger(Object identifier) {
        logger.trace("Triggering submissions for '{}'", identifier);
        synchronized (activeIdentifiers) {
            if (enforceSingleThreadPerIdentifier && activeIdentifiers.containsKey(identifier)) {
                logger.trace("Identifier '{}' is already running", identifier);
                return;
            }
        }
        synchronized (activeAsyncInvocations) {
            if (activeAsyncInvocations.containsKey(identifier)) {
                logger.trace("Identifier '{}' is already scheduled for asynchronous execution", identifier);
                return;
            }
            Invocation next = dequeue(identifier);
            if (next != null) {
                logger.trace("Scheduling {} for asynchronous execution", next);
                activeAsyncInvocations.put(identifier, next);
                getScheduler().submit(next);
                logger.trace("Submitted {} for asynchronous execution", next);
            }
        }
    }

    private void handlePotentialTimeout(Invocation invocation) {
        Object identifier = invocation.getIdentifier();
        Invocation activeAsyncInvocation = activeAsyncInvocations.get(identifier);
        if (activeAsyncInvocation == invocation) {
            Invocation activeInvocation = activeIdentifiers.get(identifier);
            if (activeInvocation != null) {
                invocation.getInvocationHandler().handleTimeout(invocation.getMethod(), activeInvocation);
            }
        }
    }

    public @Nullable Invocation dequeue(Object identifier) {
        synchronized (queues) {
            Queue<Invocation> queue = queues.get(identifier);
            if (queue != null) {
                return queue.poll();
            }
        }
        return null;
    }

    @Override
    public @Nullable Invocation getActiveInvocation() {
        synchronized (activeIdentifiers) {
            for (Invocation invocation : activeIdentifiers.values()) {
                if (invocation.getThread() == Thread.currentThread()) {
                    return invocation;
                }
            }
        }
        return null;
    }

    @Override
    public ExecutorService getScheduler() {
        return scheduler;
    }

    private void watch(Invocation invocation) {
        watcher.schedule(() -> {
            handlePotentialTimeout(invocation);
        }, invocation.getTimeout(), TimeUnit.MILLISECONDS);
        logger.trace("Scheduling timeout watcher in {}ms", invocation.getTimeout());
    }

    public void setEnforceSingleThreadPerIdentifier(boolean enforceSingleThreadPerIdentifier) {
        this.enforceSingleThreadPerIdentifier = enforceSingleThreadPerIdentifier;
    }
}
