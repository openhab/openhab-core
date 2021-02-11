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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * The {@link BundleStateMetric} class implements a set of gauge metrics for the OSGI bundles states
 *
 * @author Robert Bach - Initial contribution
 */
public class BundleStateMetric implements OpenhabCoreMeterBinder, BundleListener {
    private final Logger logger = LoggerFactory.getLogger(BundleStateMetric.class);
    public static final String METRIC_NAME = "openhab.bundle.state";
    private static final String BUNDLE_TAG_NAME = "bundle";
    private final Meter.Id commonMeterId;
    private final Map<Meter.Id, AtomicInteger> registeredMeters = new HashMap<>();
    @Nullable
    private MeterRegistry meterRegistry = null;
    private final BundleContext bundleContext;

    public BundleStateMetric(BundleContext bundleContext, Collection<Tag> tags) {
        commonMeterId = new Meter.Id(METRIC_NAME, Tags.of(tags), "state", "openHAB OSGi bundles state",
                Meter.Type.GAUGE);
        this.bundleContext = bundleContext;
    }

    @Override
    public void bindTo(@io.micrometer.core.lang.NonNull MeterRegistry meterRegistry) {
        unbind();
        logger.debug("BundleStateMetric is being bound...");
        this.meterRegistry = meterRegistry;
        Stream.of(bundleContext.getBundles()).forEach(bundle -> {
            createOrUpdateMetricForBundleState(bundle.getSymbolicName(), bundle.getState());
        });
        bundleContext.addBundleListener(this);
    }

    @Override
    public void bundleChanged(BundleEvent bundleEvent) {
        if (meterRegistry == null) {
            return;
        }
        String bundleName = bundleEvent.getBundle().getSymbolicName();
        int state = bundleEvent.getBundle().getState();
        createOrUpdateMetricForBundleState(bundleName, state);
    }

    private void createOrUpdateMetricForBundleState(String bundleName, int state) {
        Meter.Id uniqueId = commonMeterId.withTag(Tag.of(BUNDLE_TAG_NAME, bundleName));
        AtomicInteger bundleStateHolder = registeredMeters.get(uniqueId);
        if (bundleStateHolder == null) {
            bundleStateHolder = new AtomicInteger();
            Gauge.builder(uniqueId.getName(), bundleStateHolder, AtomicInteger::get).baseUnit(uniqueId.getBaseUnit())
                    .description(uniqueId.getDescription()).tags(uniqueId.getTags()).register(meterRegistry);
            registeredMeters.put(uniqueId, bundleStateHolder);
        }
        bundleStateHolder.set(state);
    }

    @Override
    public void unbind() {
        if (meterRegistry == null) {
            return;
        }
        bundleContext.removeBundleListener(this);
        registeredMeters.keySet().forEach(meterRegistry::remove);
        registeredMeters.clear();
        meterRegistry = null;
    }
}
