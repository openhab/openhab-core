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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The ThreadPoolManagerTest tests functionality of the ThreadPoolManager class.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Kaufmann - migrated from Groovy to Java
 */
@NonNullByDefault
public class ThreadPoolManagerTest {

    @BeforeAll
    public static void enableSequentialScheduledExecutorService() {
        ThreadPoolManager manager = new ThreadPoolManager();
        manager.activate(Map.of("unit-test", "10"));
    }

    @Test
    public void testSequentialExecutorServiceAssumptions() {
        Callable<Boolean> callable = () -> true;
        Runnable runnable = () -> {
        };

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

        assertTrue(service.submit(runnable) instanceof RunnableFuture);
        assertTrue(service.submit(callable) instanceof RunnableFuture);

        assertTrue(service.schedule(runnable, 1, TimeUnit.SECONDS) instanceof RunnableFuture);
        assertTrue(service.schedule(callable, 1, TimeUnit.SECONDS) instanceof RunnableFuture);

        var fixedRate = service.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);

        try {
            assertTrue(fixedRate instanceof RunnableFuture);
        } finally {
            fixedRate.cancel(false);
        }

        var fixedDelay = service.scheduleWithFixedDelay(runnable, 1, 1, TimeUnit.SECONDS);

        try {
            assertTrue(fixedDelay instanceof RunnableFuture);
        } finally {
            fixedDelay.cancel(false);
        }

        service.shutdownNow();
    }

    @Test
    public void testExecutionIsSequentialInSequentialExecutorService() {
        ScheduledExecutorService service = ThreadPoolManager.getPoolBasedSequentialScheduledExecutorService("unit-test",
                "thread-1");

        AtomicBoolean done = new AtomicBoolean(false);

        service.submit(() -> {
            Thread.sleep(100);

            done.set(true);

            return null;
        });

        assertTrue(((CompletableFuture<Boolean>) service.submit(() -> done.get())).join());
    }

    @Test
    public void testCancelDoesNotStopProcessingInSequentialExecutorService() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ScheduledExecutorService service = ThreadPoolManager.getPoolBasedSequentialScheduledExecutorService("unit-test",
                "thread-2");

        service.submit(() -> {
            Thread.sleep(100);

            return null;
        });

        service.submit(() -> null).cancel(false);

        service.submit(() -> {
            latch.countDown();
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testInstancesDoNotBlockEachOtherInSequentialExecutorService() throws InterruptedException {
        ScheduledExecutorService serviceA = ThreadPoolManager
                .getPoolBasedSequentialScheduledExecutorService("unit-test", "thread-3");
        ScheduledExecutorService serviceB = ThreadPoolManager
                .getPoolBasedSequentialScheduledExecutorService("unit-test", "thread-4");

        serviceA.submit(() -> {
            Thread.sleep(100);

            return null;
        });

        CountDownLatch latch = new CountDownLatch(1);

        serviceB.submit(() -> {
            latch.countDown();
        });

        assertTrue(latch.await(800, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSchedulingWorksInSequentialExecutorService() throws InterruptedException {
        ScheduledExecutorService service = ThreadPoolManager.getPoolBasedSequentialScheduledExecutorService("unit-test",
                "thread-5");
        CountDownLatch latch = new CountDownLatch(1);

        service.schedule(() -> {
            latch.countDown();
        }, 200, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testSchedulingGetsBlockedByRegularTaskInSequentialExecutorService() throws InterruptedException {
        ScheduledExecutorService service = ThreadPoolManager.getPoolBasedSequentialScheduledExecutorService("unit-test",
                "thread-6");
        CountDownLatch latch = new CountDownLatch(1);

        service.submit(() -> {
            Thread.sleep(200);

            return null;
        });

        service.schedule(() -> {
            latch.countDown();
        }, 20, TimeUnit.MILLISECONDS);

        assertFalse(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSchedulingDoesSpawnNewThreads() throws InterruptedException {
        ScheduledExecutorService serviceA = ThreadPoolManager
                .getPoolBasedSequentialScheduledExecutorService("unit-test", "thread-7");
        ScheduledExecutorService serviceB = ThreadPoolManager
                .getPoolBasedSequentialScheduledExecutorService("unit-test", "thread-8");

        for (int j = 0; j < 3; j++) {
            CountDownLatch block = new CountDownLatch(1);
            CountDownLatch check = new CountDownLatch(20);

            for (int i = 0; i < 20; i++) {
                serviceA.schedule(() -> {
                    try {
                        block.await();
                    } catch (InterruptedException e) {
                    }
                    check.countDown();
                }, 1, TimeUnit.MILLISECONDS);
            }

            Thread.sleep(80);

            serviceB.schedule(() -> {
                block.countDown();
            }, 20, TimeUnit.MILLISECONDS);

            assertTrue(check.await(800, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testGetScheduledPool() {
        ThreadPoolExecutor result = ThreadPoolManager.getScheduledPoolUnwrapped("test1");

        assertThat(result, instanceOf(ScheduledExecutorService.class));

        assertTrue(result.allowsCoreThreadTimeOut());
        assertEquals(ThreadPoolManager.THREAD_TIMEOUT, result.getKeepAliveTime(TimeUnit.SECONDS));
        assertEquals(ThreadPoolManager.DEFAULT_THREAD_POOL_SIZE, result.getCorePoolSize());
    }

    @Test
    public void testGetCachedPool() {
        ExecutorService result = ThreadPoolManager.getPoolUnwrapped("test2");

        assertThat(result, instanceOf(ExecutorService.class));

        ThreadPoolExecutor tpe = (ThreadPoolExecutor) result;

        assertTrue(tpe.allowsCoreThreadTimeOut());
        assertEquals(ThreadPoolManager.THREAD_TIMEOUT, tpe.getKeepAliveTime(TimeUnit.SECONDS));
        assertEquals(ThreadPoolManager.DEFAULT_THREAD_POOL_SIZE, tpe.getMaximumPoolSize());
    }

    @Test
    public void testGetConfiguredScheduledPool() {
        ThreadPoolManager tpm = new ThreadPoolManager();
        tpm.modified(Map.of("test3", "5"));
        ThreadPoolExecutor result = ThreadPoolManager.getScheduledPoolUnwrapped("test3");

        assertThat(result, instanceOf(ScheduledExecutorService.class));
        assertEquals(5, result.getCorePoolSize());
    }

    @Test
    public void testGetConfiguredCachedPool() {
        ThreadPoolManager tpm = new ThreadPoolManager();
        tpm.modified(Map.of("test4", "4"));
        ThreadPoolExecutor result = ThreadPoolManager.getPoolUnwrapped("test4");

        assertEquals(4, result.getMaximumPoolSize());
    }

    @Test
    public void testReconfiguringScheduledPool() {
        ThreadPoolExecutor result = ThreadPoolManager.getScheduledPoolUnwrapped("test5");
        assertEquals(ThreadPoolManager.DEFAULT_THREAD_POOL_SIZE, result.getCorePoolSize());

        ThreadPoolManager tpm = new ThreadPoolManager();
        tpm.modified(Map.of("test5", "11"));

        assertEquals(11, result.getCorePoolSize());
    }

    @Test
    public void testReconfiguringCachedPool() {
        ThreadPoolExecutor result = ThreadPoolManager.getPoolUnwrapped("test6");
        assertEquals(ThreadPoolManager.DEFAULT_THREAD_POOL_SIZE, result.getMaximumPoolSize());

        ThreadPoolManager tpm = new ThreadPoolManager();
        tpm.modified(Map.of("test6", "7"));

        assertEquals(7, result.getMaximumPoolSize());

        tpm.modified(Map.of("test6", "3"));
        assertEquals(3, result.getMaximumPoolSize());
    }

    @Test
    public void testGetPoolShutdown() throws InterruptedException {
        checkThreadPoolWorks("Test");
        ThreadPoolManager.getPool("Test").shutdown();
        checkThreadPoolWorks("Test");
        ThreadPoolManager.getPool("Test2").shutdownNow();
        checkThreadPoolWorks("Test2");
    }

    @Test
    public void testGetScheduledPoolShutdown() throws InterruptedException {
        checkScheduledPoolWorks("Test2");
        ThreadPoolManager.getScheduledPool("Test2").shutdown();
        checkScheduledPoolWorks("Test2");
        ThreadPoolManager.getScheduledPool("Test3").shutdownNow();
        checkScheduledPoolWorks("Test3");
    }

    private void checkThreadPoolWorks(String poolName) throws InterruptedException {
        ExecutorService threadPool = ThreadPoolManager.getPool(poolName);
        CountDownLatch cdl = new CountDownLatch(1);
        threadPool.execute(cdl::countDown);
        assertTrue(cdl.await(5, TimeUnit.SECONDS), "Checking if thread pool " + poolName + " works");
        assertFalse(threadPool.isShutdown(), "Checking if thread pool is not shut down");
    }

    private void checkScheduledPoolWorks(String poolName) throws InterruptedException {
        ScheduledExecutorService threadPool = ThreadPoolManager.getScheduledPool(poolName);
        CountDownLatch cdl = new CountDownLatch(1);
        threadPool.schedule(cdl::countDown, 100, TimeUnit.MILLISECONDS);
        assertTrue(cdl.await(5, TimeUnit.SECONDS), "Checking if thread pool " + poolName + " works");
        assertFalse(threadPool.isShutdown(), "Checking if thread pool is not shut down");
    }

    @AfterAll
    public static void disableSequentialScheduledExecutorService() {
        ThreadPoolManager manager = new ThreadPoolManager();
        manager.activate(Map.of("unit-test", "0"));
    }
}
