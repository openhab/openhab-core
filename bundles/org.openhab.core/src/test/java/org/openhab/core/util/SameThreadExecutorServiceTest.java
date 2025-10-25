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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SameThreadExecutorService}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class SameThreadExecutorServiceTest {

    @Test
    public void basicTest() {
        try (SameThreadExecutorService executor = new SameThreadExecutorService()) {
            assertFalse(executor.isShutdown());
            assertFalse(executor.isTerminated());
            assertTrue(executor.shutdownNow().isEmpty());
            assertTrue(executor.isShutdown());
            assertTrue(executor.isTerminated());
        }
    }

    @Test
    public void executeTest() throws InterruptedException {
        try (SameThreadExecutorService executor = new SameThreadExecutorService()) {
            assertFalse(executor.isShutdown());
            assertFalse(executor.isTerminated());
            final StringBuilder sb = new StringBuilder();
            executor.execute(() -> {
                sb.append(Thread.currentThread().threadId());
            });
            assertEquals(Long.toString(Thread.currentThread().threadId()), sb.toString());
            executor.shutdown();
            assertTrue(executor.isShutdown());
            assertTrue(executor.isTerminated());
            assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
        }
    }

    @Test
    public void submitTest() throws Exception {
        try (SameThreadExecutorService executor = new SameThreadExecutorService()) {
            assertFalse(executor.isShutdown());
            assertFalse(executor.isTerminated());
            assertFalse(executor.awaitTermination(5L, TimeUnit.SECONDS));
            final StringBuilder sb = new StringBuilder();
            Future<?> future = executor.submit(() -> {
                sb.append(Thread.currentThread().threadId());
            });
            assertEquals(Long.toString(Thread.currentThread().threadId()), sb.toString());
            assertFalse(future.isCancelled());
            assertTrue(future.isDone());

            Future<Long> longFuture = executor.submit(() -> Thread.currentThread().threadId());
            assertFalse(longFuture.isCancelled());
            assertTrue(longFuture.isDone());
            assertEquals(Long.valueOf(Thread.currentThread().threadId()), longFuture.get());

            sb.setLength(0);
            longFuture = executor.submit(() -> {
                sb.append(Thread.currentThread().getName());
            }, Long.valueOf(-99L));
            assertEquals(Thread.currentThread().getName(), sb.toString());
            assertEquals(Long.valueOf(-99L), longFuture.get(5L, TimeUnit.SECONDS));
            assertFalse(longFuture.isCancelled());
            assertTrue(longFuture.isDone());

            executor.shutdown();
            assertTrue(executor.isShutdown());
            assertTrue(executor.isTerminated());
        }
    }

    @Test
    public void scheduleTest() throws Exception {
        try (SameThreadExecutorService executor = new SameThreadExecutorService()) {
            assertFalse(executor.isShutdown());
            assertFalse(executor.isTerminated());
            final StringBuilder sb = new StringBuilder();
            Exception e = assertThrows(UnsupportedOperationException.class, () -> {
                executor.schedule(() -> {
                    sb.append(Thread.currentThread().threadId());
                }, 5L, TimeUnit.SECONDS);
            });
            assertEquals("Delayed schedule not supported by SameThreadExecutorService", e.getMessage());
            assertTrue(sb.isEmpty());

            ScheduledFuture<?> future = executor.schedule(() -> {
                sb.append(Thread.currentThread().threadId());
            }, 0L, null);
            assertEquals(Long.toString(Thread.currentThread().threadId()), sb.toString());
            assertFalse(future.isCancelled());
            assertTrue(future.isDone());

            e = assertThrows(UnsupportedOperationException.class, () -> {
                executor.schedule(() -> Thread.currentThread().threadId(), -3L, TimeUnit.SECONDS);
            });
            assertEquals("Delayed schedule not supported by SameThreadExecutorService", e.getMessage());

            ScheduledFuture<Long> longFuture = executor.schedule(() -> Thread.currentThread().threadId(), 0L, null);
            assertFalse(longFuture.isCancelled());
            assertTrue(longFuture.isDone());
            assertEquals(Long.valueOf(Thread.currentThread().threadId()), longFuture.get(10L, TimeUnit.SECONDS));
            assertEquals(Long.valueOf(Thread.currentThread().threadId()), longFuture.get());
            assertFalse(longFuture.cancel(false));
            assertEquals(0L, longFuture.getDelay(TimeUnit.NANOSECONDS));
            assertEquals(0, longFuture.compareTo(longFuture));
            assertEquals(0, longFuture.compareTo(future));

            sb.setLength(0);
            future = executor.scheduleAtFixedRate(() -> {
                sb.append(Thread.currentThread().threadId());
            }, 0L, 0L, null);
            assertEquals(Long.toString(Thread.currentThread().threadId()), sb.toString());
            assertFalse(future.isCancelled());
            assertTrue(future.isDone());

            future = executor.scheduleWithFixedDelay(() -> {
                sb.append(Thread.currentThread().threadId());
            }, 1L, 4L, TimeUnit.SECONDS);
            assertEquals(
                    Long.toString(Thread.currentThread().threadId()) + Long.toString(Thread.currentThread().threadId()),
                    sb.toString());
            assertFalse(future.isCancelled());
            assertTrue(future.isDone());

            executor.shutdown();
            assertTrue(executor.isShutdown());
            assertTrue(executor.isTerminated());
        }
    }
}
