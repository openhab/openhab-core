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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a thread pool executor service, which works as a developer would expect it to work.
 * The default {@link ThreadPoolExecutor} does the following (see
 * <a href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadPoolExecutor.html">the official
 * JavaDoc)</a>:
 * <ul>
 * <li>If fewer than corePoolSize threads are running, the Executor always prefers adding a new thread rather than
 * queuing.</li>
 * <li>If corePoolSize or more threads are running, the Executor always prefers queuing a request rather than adding a
 * new thread.</li>
 * <li>If a request cannot be queued, a new thread is created unless this would exceed maximumPoolSize, in which case,
 * the task will be rejected.</li>
 * </ul>
 *
 * This class in contrast implements the following logic:
 * <ul>
 * <li>corePoolSize is 1, so threads are only created on demand</li>
 * <li>If the number of busy threads is smaller than the threadPoolSize, the Executor always prefers adding (or reusing)
 * a thread rather than queuing it.</li>
 * <li>If threadPoolSize threads are busy, new requests will be put in a FIFO queue and processed as soon as a thread
 * becomes idle.</li>
 * <li>The queue size is unbound, i.e. requests will never be rejected.
 * <li>Threads are terminated after being idle for at least 10 seconds.
 * </ul>
 * Please note that this implementation (with its partially hard-coded settings) is specifically targeted for use
 * on embedded devices without a high throughput. If you intend to use it for mass data processing on a server, you
 * should definitely tweak those settings.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class QueueingThreadPoolExecutor extends ThreadPoolExecutor {

    private final Logger logger = LoggerFactory.getLogger(QueueingThreadPoolExecutor.class);

    /** we will use a core pool size of 1 since we allow to timeout core threads. */
    static final int CORE_THREAD_POOL_SIZE = 1;

    /** Our queue for queueing tasks that wait for a thread to become available */
    private final BlockingQueue<Runnable> taskQueue = new LinkedTransferQueue<>();

    /** The thread for processing the queued tasks */
    private volatile Thread queueThread;
    private final ReadWriteLock queueThreadLock = new ReentrantReadWriteLock(true);

    private final Object semaphore = new Object();

    private final String threadPoolName;

    /**
     * Allows to subclass QueueingThreadPoolExecutor.
     */
    protected QueueingThreadPoolExecutor(String name, int threadPoolSize) {
        this(name, new CommonThreadFactory(name), threadPoolSize,
                new QueueingThreadPoolExecutor.QueueingRejectionHandler());
    }

    private QueueingThreadPoolExecutor(String threadPoolName, ThreadFactory threadFactory, int threadPoolSize,
            RejectedExecutionHandler rejectionHandler) {
        super(CORE_THREAD_POOL_SIZE, threadPoolSize, 10L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory,
                rejectionHandler);

        if (threadPoolName == null || threadPoolName.trim().isEmpty()) {
            throw new IllegalArgumentException("A thread pool name must be provided!");
        }

        this.threadPoolName = threadPoolName;
        allowCoreThreadTimeOut(true);
    }

    /**
     * Creates a new instance of {@link QueueingThreadPoolExecutor}.
     *
     * @param name the name of the thread pool, will be used as a prefix for the name of the threads
     * @param threadPoolSize the maximum size of the pool
     * @return the {@link QueueingThreadPoolExecutor} instance
     */
    public static QueueingThreadPoolExecutor createInstance(String name, int threadPoolSize) {
        return new QueueingThreadPoolExecutor(name, threadPoolSize);
    }

    /**
     * Adds a new task to the queue
     *
     * @param runnable the task to add
     */
    protected void addToQueue(Runnable runnable) {
        try {
            queueThreadLock.readLock().lock();
            taskQueue.add(runnable);

            if (queueThread == null || !queueThread.isAlive()) {
                try {
                    queueThreadLock.readLock().unlock();
                    queueThreadLock.writeLock().lock();
                    // check again to make sure it has not been created by another thread
                    if (queueThread == null || !queueThread.isAlive()) {
                        logger.trace("Thread pool '{}' exhausted, queueing tasks now.", threadPoolName);
                        queueThread = createNewQueueThread();
                        queueThread.start();
                    }
                } finally {
                    queueThreadLock.writeLock().unlock();
                    queueThreadLock.readLock().lock();
                }
            }
        } finally {
            queueThreadLock.readLock().unlock();
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        synchronized (semaphore) {
            semaphore.notify();
        }
    }

    /**
     * This implementation does not allow setting a custom handler.
     *
     * @throws UnsupportedOperationException if called.
     */
    @Override
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockingQueue<Runnable> getQueue() {
        return taskQueue;
    }

    @Override
    public void execute(Runnable command) {
        // make sure that rejected tasks are executed before any new concurrently incoming tasks
        if (taskQueue.isEmpty()) {
            super.execute(command);
        } else {
            if (command == null) {
                throw new IllegalArgumentException("Command can not be null.");
            }

            // ignore incoming tasks when the executor is shutdown
            if (!isShutdown()) {
                addToQueue(command);
            }
        }
    }

    private Thread createNewQueueThread() {
        Thread thread = getThreadFactory().newThread(new Runnable() {

            @Override
            public void run() {
                final QueueingThreadPoolExecutor tpe = QueueingThreadPoolExecutor.this;
                final Consumer<Runnable> parentExecute = QueueingThreadPoolExecutor.super::execute;

                while (true) {
                    // check if some thread from the pool is idle
                    if (tpe.getActiveCount() < tpe.getMaximumPoolSize()) {
                        try {
                            // keep waiting for max 2 seconds if further tasks are pushed to the queue
                            final Runnable runnable = taskQueue.poll(2, TimeUnit.SECONDS);
                            if (runnable != null) {
                                logger.trace("Executing queued task of thread pool '{}'.", threadPoolName);
                                parentExecute.accept(runnable);
                            } else {
                                try {
                                    queueThreadLock.writeLock().lock();
                                    if (taskQueue.isEmpty()) {
                                        // Set the queueThread member to null while holding the writeLock, so we signal
                                        // that the thread will die. Without this approach the thread could be still
                                        // alive and only the thread itself knows that he is dyeing.
                                        queueThread = null;
                                        break;
                                    }
                                } finally {
                                    queueThreadLock.writeLock().unlock();
                                }
                            }
                        } catch (InterruptedException e) {
                        }
                    } else {
                        // let's wait for a thread to become available, but max. 1 second
                        try {
                            synchronized (semaphore) {
                                semaphore.wait(1000);
                            }
                        } catch (InterruptedException e) {
                        }
                    }
                }
                logger.trace("Queue for thread pool '{}' fully processed - terminating queue thread.", threadPoolName);
            }
        });
        thread.setName(threadPoolName + "-queue");
        return thread;
    }

    /**
     * This is the internally used thread factory, which creates non-daemon threads and assigns them a sequentially
     * indexed name.
     */
    private static class CommonThreadFactory implements ThreadFactory {

        protected final ThreadGroup group;
        protected final AtomicInteger threadNumber = new AtomicInteger(1);
        protected final String name;

        public CommonThreadFactory(String name) {
            this.name = name;
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, name + "-" + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }
    }

    /**
     * This is the internally used rejection handler, which - instead of rejecting a task - puts it to the queue of the
     * pool.
     */
    private static class QueueingRejectionHandler extends ThreadPoolExecutor.DiscardPolicy {

        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
            if (!threadPoolExecutor.isShutdown()) {
                QueueingThreadPoolExecutor queueingThreadPoolExecutor = (QueueingThreadPoolExecutor) threadPoolExecutor;
                queueingThreadPoolExecutor.addToQueue(runnable);
            }
        }
    }
}
