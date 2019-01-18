/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.common;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * The ThreadPoolManagerTest tests functionality of the ThreadPoolManager class.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Kaufmann - migrated from Groovy to Java
 */
public class ThreadPoolManagerTest {

    @Test
    public void testGetScheduledPool() {
        ThreadPoolExecutor result = (ThreadPoolExecutor) ThreadPoolManager.getScheduledPool("test1");

        assertThat(result, instanceOf(ScheduledExecutorService.class));

        assertTrue(result.allowsCoreThreadTimeOut());
        assertEquals(ThreadPoolManager.THREAD_TIMEOUT, result.getKeepAliveTime(TimeUnit.SECONDS));
        assertEquals(ThreadPoolManager.DEFAULT_THREAD_POOL_SIZE, result.getCorePoolSize());
    }

    @Test
    public void testGetCachedPool() {
        ExecutorService result = ThreadPoolManager.getPool("test2");

        assertThat(result, instanceOf(ExecutorService.class));

        ThreadPoolExecutor tpe = (ThreadPoolExecutor) result;

        assertTrue(tpe.allowsCoreThreadTimeOut());
        assertEquals(ThreadPoolManager.THREAD_TIMEOUT, tpe.getKeepAliveTime(TimeUnit.SECONDS));
        assertEquals(ThreadPoolManager.DEFAULT_THREAD_POOL_SIZE, tpe.getMaximumPoolSize());
    }

    @Test
    public void testGetConfiguredScheduledPool() {
        ThreadPoolManager tpm = new ThreadPoolManager();
        tpm.modified(Collections.singletonMap("test3", "5"));
        ThreadPoolExecutor result = (ThreadPoolExecutor) ThreadPoolManager.getScheduledPool("test3");

        assertThat(result, instanceOf(ScheduledExecutorService.class));
        assertEquals(5, result.getCorePoolSize());
    }

    @Test
    public void testGetConfiguredCachedPool() {
        ThreadPoolManager tpm = new ThreadPoolManager();
        tpm.modified(Collections.singletonMap("test4", "4"));
        ThreadPoolExecutor result = (ThreadPoolExecutor) ThreadPoolManager.getPool("test4");

        assertEquals(4, result.getMaximumPoolSize());
    }

    @Test
    public void testReconfiguringScheduledPool() {
        ThreadPoolExecutor result = (ThreadPoolExecutor) ThreadPoolManager.getScheduledPool("test5");
        assertEquals(ThreadPoolManager.DEFAULT_THREAD_POOL_SIZE, result.getCorePoolSize());

        ThreadPoolManager tpm = new ThreadPoolManager();
        tpm.modified(Collections.singletonMap("test5", "11"));

        assertEquals(11, result.getCorePoolSize());
    }

    @Test
    public void testReconfiguringCachedPool() {
        ThreadPoolExecutor result = (ThreadPoolExecutor) ThreadPoolManager.getPool("test6");
        assertEquals(ThreadPoolManager.DEFAULT_THREAD_POOL_SIZE, result.getMaximumPoolSize());

        ThreadPoolManager tpm = new ThreadPoolManager();
        tpm.modified(Collections.singletonMap("test6", "7"));

        assertEquals(7, result.getMaximumPoolSize());

        tpm.modified(Collections.singletonMap("test6", "3"));
        assertEquals(3, result.getMaximumPoolSize());
    }
}
