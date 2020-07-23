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
package org.openhab.core.common;

import static org.junit.Assert.*;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for the {@link QueueingThreadPoolExecutor} class, abbreviated here
 * QueueingTPE.
 *
 * @author Jochen Hiller - Initial contribution
 */
public class QueueingThreadPoolExecutorTest {

    /**
     * we know that the QueuingTPE uses a core pool timeout of 10 seconds. Will
     * be needed to check if all threads are down after this timeout.
     */
    private static final int CORE_POOL_TIMEOUT = 10000;

    /**
     * We can enable logging for all test cases.
     */
    @Before
    public void setUp() {
        // enable to see logging. See below how to include slf4j-simple
        // enableLogging();
        disableLogging();
    }

    /**
     * Creates QueueingTPE instances. By default there will be NO thread
     * created, check it.
     */
    @Test
    public void testCreateInstance() {
        String poolName = "testCreateInstance";
        QueueingThreadPoolExecutor.createInstance(poolName, 1);
        QueueingThreadPoolExecutor.createInstance(poolName, 2);
        QueueingThreadPoolExecutor.createInstance(poolName, 5);
        QueueingThreadPoolExecutor.createInstance(poolName, 10);
        QueueingThreadPoolExecutor.createInstance(poolName, 1000);
        QueueingThreadPoolExecutor.createInstance(poolName, 10000);
        QueueingThreadPoolExecutor.createInstance(poolName, 100000);
        QueueingThreadPoolExecutor.createInstance(poolName, 1000000);
        // no threads created
        assertFalse(areThreadsFromPoolRunning(poolName));
    }

    /**
     * Tests what happens when poolName == null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateInstanceInvalidArgsPoolNameNull() throws InterruptedException {
        QueueingThreadPoolExecutor.createInstance(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInstanceInvalidArgsPoolSize0() {
        QueueingThreadPoolExecutor.createInstance("test", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInstanceInvalidArgsPoolSizeMinus1() {
        QueueingThreadPoolExecutor.createInstance("test", -1);
    }

    /**
     * This test tests behavior of QueueingTPE for a pool of core=1, max=2 when
     * no tasks have been scheduled. Same assumptions as above.
     */
    @Test
    public void testQueuingTPEPoolSize2() throws InterruptedException {
        String poolName = "testQueuingTPEPoolSize2";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);

        assertEquals(pool.getActiveCount(), 0);
        assertTrue(pool.allowsCoreThreadTimeOut());
        assertEquals(pool.getCompletedTaskCount(), 0);
        assertEquals(pool.getCorePoolSize(), 1);
        assertEquals(pool.getMaximumPoolSize(), 2);
        assertEquals(pool.getLargestPoolSize(), 0);
        assertEquals(pool.getQueue().size(), 0);

        // now expect that no threads have been created
        assertFalse(areThreadsFromPoolRunning(poolName));

        // no need to wait after shutdown as no threads created
        pool.shutdown();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPoolWithBlankPoolName() throws InterruptedException {
        basicTestForPoolName(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPoolWithEmptyPoolName() throws InterruptedException {
        basicTestForPoolName("");
    }

    /**
     * Basic tests of a pool with a given name. Checks thread creation and
     * cleanup.
     */
    protected void basicTestForPoolName(String poolName) throws InterruptedException {
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
        pool.execute(createRunnable100ms());
        pool.execute(createRunnable100ms());
        assertTrue(isPoolThreadActive(poolName, 1));
        assertTrue(isPoolThreadActive(poolName, 2));

        // no queue thread

        // needs to wait CORE_POOL_TIMEOUT + x until all threads are down again
        pool.shutdown();
        Thread.sleep(CORE_POOL_TIMEOUT + 1000);
        assertFalse(areThreadsFromPoolRunning(poolName));
    }

    /**
     * Test basic thread creation, including thread settings (name, prio,
     * daemon).
     */
    protected void basicTestPoolSize2ThreadSettings(String poolName) throws InterruptedException {
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);

        // pool 2 tasks, threads must exist
        pool.execute(createRunnable10s());
        assertEquals(pool.getActiveCount(), 1);
        assertTrue(isPoolThreadActive(poolName, 1));
        Thread t1 = getThread(poolName + "-1");
        assertFalse(t1.isDaemon());
        // thread will be NORM prio or max prio of this thread group, which can
        // < than NORM
        int prio1 = Math.min(t1.getThreadGroup().getMaxPriority(), Thread.NORM_PRIORITY);
        assertEquals(t1.getPriority(), prio1);

        pool.execute(createRunnable10s());
        assertEquals(pool.getActiveCount(), 2);
        assertTrue(isPoolThreadActive(poolName, 2));
        Thread t2 = getThread(poolName + "-2");
        assertFalse(t2.isDaemon());
        // thread will be NORM prio or max prio of this thread group, which can
        // < than NORM
        int prio2 = Math.min(t2.getThreadGroup().getMaxPriority(), Thread.NORM_PRIORITY);
        assertEquals(t2.getPriority(), prio2);

        // 2 more tasks, will be queued, no threads
        pool.execute(createRunnable1s());
        // as pool size is 2, no more active threads, will stay at 2
        assertEquals(pool.getActiveCount(), 2);
        assertFalse(isPoolThreadActive(poolName, 3));
        assertEquals(pool.getQueue().size(), 1);

        pool.execute(createRunnable1s());
        assertEquals(pool.getActiveCount(), 2);
        assertFalse(isPoolThreadActive(poolName, 4));
        assertEquals(pool.getQueue().size(), 2);

        // 0 are yet executed
        assertEquals(pool.getCompletedTaskCount(), 0);

        // needs to wait CORE_POOL_TIMEOUT + 2sec-queue-thread + x until all
        // threads are down again
        pool.shutdown();
        Thread.sleep(CORE_POOL_TIMEOUT + 3000);
        assertFalse(areThreadsFromPoolRunning(poolName));
    }

    /**
     * Tests what happens when wrong rejected execution handler will be used.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testSetInvalidRejectionHandler() throws InterruptedException {
        String poolName = "testShutdownNoEntriesIntoQueueAnymore";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
    }

    // helper methods

    private void disableLogging() {
        // disable logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
        System.clearProperty("org.slf4j.simpleLogger.logFile");
        System.clearProperty("org.slf4j.simpleLogger.showDateTime");
    }

    private boolean isPoolThreadActive(String poolName, int id) {
        return getThread(poolName + "-" + id) != null;
    }

    /**
     * Search for thread with given name.
     *
     * @return found thread or null
     */
    private Thread getThread(String threadName) {
        // get top level thread group
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g.getParent() != null) {
            g = g.getParent();
        }
        // make buffer 10 entries bigger
        Thread[] l = new Thread[g.activeCount() + 10];
        int n = g.enumerate(l);
        for (int i = 0; i < n; i++) {
            // enable printout to see threads
            // System.out.println("getThread:" + l[i]);
            if (l[i].getName().equals(threadName)) {
                return l[i];
            }
        }
        return null;
    }

    private boolean areThreadsFromPoolRunning(String poolName) {
        // get top level thread group
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g.getParent() != null) {
            g = g.getParent();
        }
        boolean foundThreads = false;
        // make buffer 10 entries bigger
        Thread[] l = new Thread[g.activeCount() + 10];
        int n = g.enumerate(l);
        for (int i = 0; i < n; i++) {
            // we can only test if name is at least one character,
            // otherwise there will be threads found (handles poolName="")
            if (poolName.length() > 0) {
                if (l[i].getName().startsWith(poolName)) {
                    // enable printout to see threads
                    // System.out.println("areThreadsFromPoolRunning: " +
                    // l[i].toString());
                    foundThreads = true;
                }
            }
        }
        return foundThreads;
    }

    // Runnables for testing

    private Runnable createRunnable100ms() {
        return new Runnable100ms();
    }

    private Runnable createRunnable1s() {
        return new Runnable1s();
    }

    private Runnable createRunnable10s() {
        return new Runnable10s();
    }

    private abstract static class AbstractRunnable implements Runnable {
        private static AtomicInteger runs = new AtomicInteger(0);

        protected final Logger logger = LoggerFactory.getLogger(AbstractRunnable.class);

        protected void sleep(int milliseconds) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                // ignore
                logger.info("interrupted");
            }
        }

        @Override
        public void run() {
            logger.info("run");
            runs.incrementAndGet();
        }
    }

    private static class Runnable100ms extends AbstractRunnable {
        @Override
        public void run() {
            super.run();
            sleep(100);
        }
    }

    private static class Runnable1s extends AbstractRunnable {
        @Override
        public void run() {
            super.run();
            sleep(1 * 1000);
        }
    }

    private static class Runnable10s extends AbstractRunnable {
        @Override
        public void run() {
            super.run();
            sleep(10 * 1000);
        }
    }
}
