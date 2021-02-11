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
package org.openhab.core.io.monitor.internal.metrics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

/**
 * The {@link ThreadPoolMetric} class implements a set of metrics for ThreadManager thread pool stats
 *
 * @author Robert Bach - Initial contribution
 */
public class ThreadPoolMetric implements OpenhabCoreMeterBinder {

    private final Logger logger = LoggerFactory.getLogger(ThreadPoolMetric.class);
    public static final Tag CORE_THREADPOOL_METRIC_TAG = Tag.of("metric", "openhab.core.metric.threadpools");
    private static final String POOLNAME_TAG_NAME = "pool";
    private final Set<Tag> tags = new HashSet<>();
    @Nullable
    private MeterRegistry meterRegistry;

    public ThreadPoolMetric(Collection<Tag> tags) {
        this.tags.addAll(tags);
        this.tags.add(CORE_THREADPOOL_METRIC_TAG);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        unbind();
        logger.debug("ThreadPoolMetric is being bound...");
        this.meterRegistry = registry;
        try {
            ThreadPoolManager.getPoolNames().forEach(this::addPoolMetrics);
        } catch (NoSuchMethodError nsme) {
            logger.info("A newer version of openHAB is required for thread pool metrics to work.");
        }
    }

    private void addPoolMetrics(String poolName) {
        ExecutorService es = ThreadPoolManager.getPool(poolName);
        if (es == null) {
            return;
        }
        Set<Tag> tagsWithPoolname = new HashSet<>(tags);
        tagsWithPoolname.add(Tag.of(POOLNAME_TAG_NAME, poolName));
        new ExecutorServiceMetrics(es, poolName, tagsWithPoolname).bindTo(meterRegistry);
    }

    @Override
    public void unbind() {
        if (meterRegistry == null) {
            return;
        }
        for (Meter meter : meterRegistry.getMeters()) {
            if (meter.getId().getTags().contains(CORE_THREADPOOL_METRIC_TAG)) {
                meterRegistry.remove(meter);
            }
        }
        meterRegistry = null;
    }
}
