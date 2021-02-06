/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    protected void activate(Map<String, Object> properties) {
        modified(properties);
    }

    protected void modified(Map<String, Object> properties) {
        for (Entry<String, Object> entry : properties.entrySet()) {
            if (Constants.SERVICE_PID.equals(entry.getKey()) || ComponentConstants.COMPONENT_ID.equals(entry.getKey())
                    || ComponentConstants.COMPONENT_NAME.equals(entry.getKey())) {
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
            return (ScheduledExecutorService) pool;
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
        return pool;
    }

    public static int getConfig(String poolName) {
        Integer cfg = configs.get(poolName);
        return (cfg != null) ? cfg : DEFAULT_THREAD_POOL_SIZE;
    }

    public static Set<String> getPoolNames() {
        return pools.keySet();
    }
}
