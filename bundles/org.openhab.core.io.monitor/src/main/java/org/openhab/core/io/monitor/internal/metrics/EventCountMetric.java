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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemStateEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * The {@link EventCountMetric} class implements a gauge metric for the openHAB events count (per topic)
 * topic.
 *
 * @author Robert Bach - Initial contribution
 */
public class EventCountMetric implements OpenhabCoreMeterBinder, EventSubscriber {

    public static final String METRIC_NAME = "event_count";
    private final Logger logger = LoggerFactory.getLogger(EventCountMetric.class);
    private static final Tag CORE_EVENT_COUNT_METRIC_TAG = Tag.of("metric", "openhab.core.metric.eventcount");
    private static final String TOPIC_TAG_NAME = "topic";
    @Nullable
    private MeterRegistry meterRegistry;
    private final Set<Tag> tags = new HashSet<>();
    private ServiceRegistration<?> eventSubscriberRegistration;
    private BundleContext bundleContext;

    public EventCountMetric(BundleContext bundleContext, Collection<Tag> tags) {
        this.tags.addAll(tags);
        this.tags.add(CORE_EVENT_COUNT_METRIC_TAG);
        this.bundleContext = bundleContext;
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        unbind();
        logger.debug("EventCountMetric is being bound...");
        this.meterRegistry = meterRegistry;
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("event.topics", "openhab/*");
        this.eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this,
                properties);
    }

    @Override
    public void unbind() {
        if (meterRegistry == null) {
            return;
        }
        for (Meter meter : meterRegistry.getMeters()) {
            if (meter.getId().getTags().contains(CORE_EVENT_COUNT_METRIC_TAG)) {
                meterRegistry.remove(meter);
            }
        }
        meterRegistry = null;
        if (this.eventSubscriberRegistration != null) {
            this.eventSubscriberRegistration.unregister();
            this.eventSubscriberRegistration = null;
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        HashSet<String> subscribedEvents = new HashSet<>();
        subscribedEvents.add(ItemCommandEvent.TYPE);
        subscribedEvents.add(ItemStateEvent.TYPE);
        return subscribedEvents;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        if (meterRegistry == null) {
            logger.trace("Measurement not started. Skipping event processing");
            return;
        }
        String topic = event.getTopic();
        logger.debug("Received event on topic {}.", topic);
        Set<Tag> tagsWithTopic = new HashSet<>(tags);
        tagsWithTopic.add(Tag.of(TOPIC_TAG_NAME, topic));
        meterRegistry.counter(METRIC_NAME, tagsWithTopic).increment();
    }
}
