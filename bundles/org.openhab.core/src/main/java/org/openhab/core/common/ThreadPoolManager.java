/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openhab.core.internal.common.WrappedScheduledExecutorService;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class provides a general mechanism to create thread pools. In general, no code of openHAB
 * should deal with its own pools, but rather use this class.
 * The created thread pools have named threads, so that it is easy to find them in the debugger. Additionally, it is
 * possible to configure the pool sizes through the configuration admin service, so that solutions have the chance to
 * tweak the pool sizes according to their needs.
 *
 * <p>
 * The configuration can be done as
 * <br/>
 * {@code org.openhab.core.threadpool:<poolName>=<poolSize>}
 * <br/>
 * All threads will time out after {@link THREAD_TIMEOUT}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(configurationPid = ThreadPoolManager.CONFIGURATION_PID)
public class ThreadPoolManager {

    public static final String CONFIGURATION_PID = "org.openhab.threadpool";

    /**
     * The common thread pool is reserved for occasional, light weight tasks that run quickly, and
     * use little resources to execute. Tasks that do not fit into this category should setup
     * their own dedicated pool or permanent thread.
     */
    public static final String THREAD_POOL_NAME_COMMON = "common";

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolManager.class);

    protected static final int DEFAULT_THREAD_POOL_SIZE = 5;

    protected static final long THREAD_TIMEOUT = 65L;
    protected static final long THREAD_MONITOR_SLEEP = 60000;

    protected static Map<String, ExecutorService> pools = new WeakHashMap<>();

    private static Map<String, Integer> configs = new ConcurrentHashMap<>();

    private static final Set<String> OSGI_PROPERTY_NAMES = Set.of(Constants.SERVICE_PID,
            ComponentConstants.COMPONENT_ID, ComponentConstants.COMPONENT_NAME, "osgi.ds.satisfying.condition.target");

    protected void activate(Map<String, Object> properties) {
        modified(properties);
    }

    protected void modified(Map<String, Object> properties) {
        for (Entry<String, Object> entry : properties.entrySet()) {
            if (OSGI_PROPERTY_NAMES.contains(entry.getKey())) {
                continue;
            }
            String poolName = entry.getKey();
            Object config = entry.getValue();
            if (config == null) {
                configs.remove(poolName);
            }
            if (config instanceof String) {
                try {
                    Integer poolSize = Integer.valueOf((String) config);
                    configs.put(poolName, poolSize);
                    ThreadPoolExecutor pool = (ThreadPoolExecutor) pools.get(poolName);
                    if (pool instanceof ScheduledThreadPoolExecutor) {
                        pool.setCorePoolSize(poolSize);
                        LOGGER.debug("Updated scheduled thread pool '{}' to size {}", poolName, poolSize);
                    } else if (pool instanceof QueueingThreadPoolExecutor) {
                        pool.setMaximumPoolSize(poolSize);
                        LOGGER.debug("Updated queuing thread pool '{}' to size {}", poolName, poolSize);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("Ignoring invalid configuration for pool '{}': {} - value must be an integer", poolName,
                            config);
                    continue;
                }
            }
        }
    }

    /**
     * Returns an instance of a scheduled thread pool service. If it is the first request for the given pool name, the
     * instance is newly created.
     *
     * @param poolName a short name used to identify the pool, e.g. "discovery"
     * @return an instance to use
     */
    public static ScheduledExecutorService getScheduledPool(String poolName) {
        ExecutorService pool = pools.get(poolName);
        if (pool == null) {
            synchronized (pools) {
                // do a double check if it is still null or if another thread might have created it meanwhile
                pool = pools.get(poolName);
                if (pool == null) {
                    int cfg = getConfig(poolName);
                    pool = new WrappedScheduledExecutorService(cfg,
                            new NamedThreadFactory(poolName, true, Thread.NORM_PRIORITY));
                    ((ThreadPoolExecutor) pool).setKeepAliveTime(THREAD_TIMEOUT, TimeUnit.SECONDS);
                    ((ThreadPoolExecutor) pool).allowCoreThreadTimeOut(true);
                    ((ScheduledThreadPoolExecutor) pool).setRemoveOnCancelPolicy(true);
                    pools.put(poolName, pool);
                    LOGGER.debug("Created scheduled thread pool '{}' of size {}", poolName, cfg);
                }
            }
        }
        if (pool instanceof ScheduledExecutorService) {
            return new UnstoppableScheduledExecutorService(poolName, (ScheduledExecutorService) pool);
        } else {
            throw new IllegalArgumentException("Pool " + poolName + " is not a scheduled pool!");
        }
    }

    /**
     * Returns an instance of a cached thread pool service. If it is the first request for the given pool name, the
     * instance is newly created.
     *
     * @param poolName a short name used to identify the pool, e.g. "discovery"
     * @return an instance to use
     */
    public static ExecutorService getPool(String poolName) {
        ExecutorService pool = pools.get(poolName);
        if (pool == null) {
            synchronized (pools) {
                // do a double check if it is still null or if another thread might have created it meanwhile
                pool = pools.get(poolName);
                if (pool == null) {
                    int cfg = getConfig(poolName);
                    pool = QueueingThreadPoolExecutor.createInstance(poolName, cfg);
                    ((ThreadPoolExecutor) pool).setKeepAliveTime(THREAD_TIMEOUT, TimeUnit.SECONDS);
                    ((ThreadPoolExecutor) pool).allowCoreThreadTimeOut(true);
                    pools.put(poolName, pool);
                    LOGGER.debug("Created thread pool '{}' with size {}", poolName, cfg);
                }
            }
        }
        return new UnstoppableExecutorService<>(poolName, pool);
    }

    static ThreadPoolExecutor getPoolUnwrapped(String poolName) {
        UnstoppableExecutorService<?> ret = (UnstoppableExecutorService<?>) getPool(poolName);
        return (ThreadPoolExecutor) ret.getDelegate();
    }

    static ThreadPoolExecutor getScheduledPoolUnwrapped(String poolName) {
        UnstoppableExecutorService<?> ret = (UnstoppableScheduledExecutorService) getScheduledPool(poolName);
        return (ThreadPoolExecutor) ret.getDelegate();
    }

    protected static int getConfig(String poolName) {
        Integer cfg = configs.get(poolName);
        return cfg != null ? cfg : DEFAULT_THREAD_POOL_SIZE;
    }

    public static Set<String> getPoolNames() {
        return new HashSet<>(pools.keySet());
    }

    static class UnstoppableExecutorService<T extends ExecutorService> implements ExecutorService {

        protected final Logger logger = LoggerFactory.getLogger(getClass());
        protected final T delegate;
        protected final String threadPoolName;

        private UnstoppableExecutorService(String threadPoolName, T delegate) {
            this.threadPoolName = threadPoolName;
            this.delegate = delegate;
        }

        @Override
        public void shutdown() {
            logger.warn("shutdown() invoked on a shared thread pool '{}'. This is a bug, please submit a bug report",
                    threadPoolName, new IllegalStateException());
        }

        @Override
        public List<Runnable> shutdownNow() {
            logger.warn("shutdownNow() invoked on a shared thread pool '{}'. This is a bug, please submit a bug report",
                    threadPoolName, new IllegalStateException());
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(command);
        }

        T getDelegate() {
            return delegate;
        }
    }

    static class UnstoppableScheduledExecutorService extends UnstoppableExecutorService<ScheduledExecutorService>
            implements ScheduledExecutorService {

        private UnstoppableScheduledExecutorService(String threadPoolName, ScheduledExecutorService delegate) {
            super(threadPoolName, delegate);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return delegate.schedule(command, delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return delegate.schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                TimeUnit unit) {
            return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
    }
}
