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

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

/**
 * The {@link JVMMetric} class implements JVM related metrics like class loading, memory, GC and thread figures
 *
 * @author Robert Bach - Initial contribution
 */
public class JVMMetric implements OpenhabCoreMeterBinder {

    private final Logger logger = LoggerFactory.getLogger(JVMMetric.class);
    private static final Tag CORE_JVM_METRIC_TAG = Tag.of("metric", "openhab.core.metric.jvm");
    private final Set<Tag> tags = new HashSet<>();
    @Nullable
    private MeterRegistry meterRegistry;

    public JVMMetric(Collection<Tag> tags) {
        this.tags.addAll(tags);
        this.tags.add(CORE_JVM_METRIC_TAG);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        unbind();
        logger.debug("JVMMetric is being bound...");
        this.meterRegistry = registry;
        new ClassLoaderMetrics(tags).bindTo(meterRegistry);
        new JvmMemoryMetrics(tags).bindTo(meterRegistry);
        new JvmGcMetrics(tags).bindTo(meterRegistry);
        new ProcessorMetrics(tags).bindTo(meterRegistry);
        new JvmThreadMetrics(tags).bindTo(meterRegistry);
    }

    @Override
    public void unbind() {
        if (meterRegistry == null) {
            return;
        }
        for (Meter meter : meterRegistry.getMeters()) {
            if (meter.getId().getTags().contains(CORE_JVM_METRIC_TAG)) {
                meterRegistry.remove(meter);
            }
        }
        meterRegistry = null;
    }
}
