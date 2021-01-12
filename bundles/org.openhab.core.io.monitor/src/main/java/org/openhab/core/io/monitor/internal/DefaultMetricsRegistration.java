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
package org.openhab.core.io.monitor.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.monitor.MeterRegistryProvider;
import org.openhab.core.io.monitor.internal.metrics.BundleStateMetric;
import org.openhab.core.io.monitor.internal.metrics.EventCountMetric;
import org.openhab.core.io.monitor.internal.metrics.JVMMetric;
import org.openhab.core.io.monitor.internal.metrics.OpenhabCoreMeterBinder;
import org.openhab.core.io.monitor.internal.metrics.RuleMetric;
import org.openhab.core.io.monitor.internal.metrics.ThingStateMetric;
import org.openhab.core.io.monitor.internal.metrics.ThreadPoolMetric;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.thing.ThingRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

/**
 * The {@link DefaultMetricsRegistration} class registers all openHAB internal metrics with the global MeterRegistry.
 *
 * @author Robert Bach - Initial contribution
 */
@Component(immediate = true, service = MeterRegistryProvider.class)
@NonNullByDefault
public class DefaultMetricsRegistration implements ReadyService.ReadyTracker, MeterRegistryProvider {

    private final Logger logger = LoggerFactory.getLogger(DefaultMetricsRegistration.class);
    public static final Tag OH_CORE_METRIC_TAG = Tag.of("openhab_core_metric", "true");
    private final BundleContext bundleContext;
    private final Set<OpenhabCoreMeterBinder> meters = new HashSet<>();
    private final CompositeMeterRegistry registry = Metrics.globalRegistry;
    private final ReadyService readyService;
    private final ThingRegistry thingRegistry;

    @Activate
    public DefaultMetricsRegistration(BundleContext bundleContext, final @Reference ReadyService readyService,
            final @Reference ThingRegistry thingRegistry) {
        this.bundleContext = bundleContext;
        this.readyService = readyService;
        this.thingRegistry = thingRegistry;
    }

    @Activate
    protected void activate() {
        logger.trace("Activating DefaultMetricsRegistration...");
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE)
                .withIdentifier(Integer.toString(StartLevelService.STARTLEVEL_RULES)));
    }

    @Deactivate
    public void deactivate() {
        unregisterMeters();
        readyService.unregisterTracker(this);
    }

    private void registerMeters() {
        logger.debug("Registering meters...");
        Set<Tag> tags = Set.of(OH_CORE_METRIC_TAG);
        meters.add(new JVMMetric(tags));
        meters.add(new ThreadPoolMetric(tags));
        meters.add(new BundleStateMetric(bundleContext, tags));
        meters.add(new ThingStateMetric(bundleContext, thingRegistry, tags));
        meters.add(new EventCountMetric(bundleContext, tags));
        meters.add(new RuleMetric(bundleContext, tags));
        meters.add(new ThreadPoolMetric(tags));

        meters.forEach(m -> m.bindTo(registry));
    }

    private void unregisterMeters() {
        this.meters.forEach(OpenhabCoreMeterBinder::unbind);
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        registerMeters();
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        unregisterMeters();
    }

    @Override
    public CompositeMeterRegistry getOHMeterRegistry() {
        return registry;
    }
}
