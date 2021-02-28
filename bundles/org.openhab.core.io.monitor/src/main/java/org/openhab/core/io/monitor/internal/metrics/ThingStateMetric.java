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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.events.ThingStatusInfoEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * The {@link ThingStateMetric} class implements a metric for the openHAB things states.
 *
 * @author Robert Bach - Initial contribution
 */
public class ThingStateMetric implements OpenhabCoreMeterBinder, EventSubscriber {
    private final Logger logger = LoggerFactory.getLogger(ThingStateMetric.class);
    public static final String METRIC_NAME = "openhab.thing.state";
    private static final String THING_TAG_NAME = "thing";
    private static final String THINGSTATUS_TOPIC_PREFIX = "openhab/things/";
    private final ThingRegistry thingRegistry;
    private final Meter.Id commonMeterId;
    private final Map<Meter.Id, AtomicInteger> registeredMeters = new HashMap<>();
    private @Nullable MeterRegistry meterRegistry;
    private @Nullable ServiceRegistration<?> eventSubscriberRegistration;
    private final BundleContext bundleContext;
    private final Gson gson = new Gson();

    public ThingStateMetric(BundleContext bundleContext, ThingRegistry thingRegistry, Collection<Tag> tags) {
        this.bundleContext = bundleContext;
        this.thingRegistry = thingRegistry;
        commonMeterId = new Meter.Id(METRIC_NAME, Tags.of(tags), "state", "openHAB thing state", Meter.Type.GAUGE);
    }

    @Override
    public void bindTo(@io.micrometer.core.lang.NonNull MeterRegistry meterRegistry) {
        unbind();
        logger.debug("ThingStateMetric is being bound...");
        this.meterRegistry = meterRegistry;
        thingRegistry.getAll().forEach(
                thing -> createOrUpdateMetricForBundleState(thing.getUID().getId(), thing.getStatus().ordinal()));
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("event.topics", "openhab/things/*");
        this.eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this,
                properties);
    }

    private void createOrUpdateMetricForBundleState(String thingUid, int thingStatus) {
        Meter.Id uniqueId = commonMeterId.withTag(Tag.of(THING_TAG_NAME, thingUid));
        AtomicInteger thingStateHolder = registeredMeters.get(uniqueId);
        if (thingStateHolder == null) {
            thingStateHolder = new AtomicInteger();
            Gauge.builder(uniqueId.getName(), thingStateHolder, AtomicInteger::get).baseUnit(uniqueId.getBaseUnit())
                    .description(uniqueId.getDescription()).tags(uniqueId.getTags()).register(meterRegistry);
            registeredMeters.put(uniqueId, thingStateHolder);
        }
        thingStateHolder.set(thingStatus);
    }

    @Override
    public void unbind() {
        if (meterRegistry == null) {
            return;
        }
        if (eventSubscriberRegistration != null) {
            eventSubscriberRegistration.unregister();
            eventSubscriberRegistration = null;
        }
        registeredMeters.keySet().forEach(meterRegistry::remove);
        registeredMeters.clear();
        meterRegistry = null;
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ThingStatusInfoEvent.TYPE);
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        logger.trace("Received ThingStatusInfo(Changed)Event...");
        String thingId = event.getTopic().substring(THINGSTATUS_TOPIC_PREFIX.length(),
                event.getTopic().lastIndexOf('/'));
        ThingStatus status = gson.fromJson(event.getPayload(), ThingStatusInfo.class).getStatus();
        createOrUpdateMetricForBundleState(thingId, status.ordinal());
    }
}
