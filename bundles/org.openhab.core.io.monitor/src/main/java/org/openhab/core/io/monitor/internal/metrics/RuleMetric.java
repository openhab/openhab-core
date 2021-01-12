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
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * The {@link RuleMetric} class implements a gauge metric for rules RUNNING events (per rule)
 *
 * @author Robert Bach - Initial contribution
 */
public class RuleMetric implements OpenhabCoreMeterBinder, EventSubscriber {

    public static final String METRIC_NAME = "openhab.rule.runs";
    public static final String RULES_TOPIC_PREFIX = "openhab/rules/";
    public static final String RULES_TOPIC_SUFFIX = "/state";
    public static final String SUBSCRIPTION_PROPERTY_TOPIC = "event.topics";
    public static final String RULES_TOPIC_FILTER = "openhab/rules/*";
    private final Logger logger = LoggerFactory.getLogger(RuleMetric.class);
    private static final Tag CORE_RULE_METRIC_TAG = Tag.of("metric", "openhab.core.metric.rules");
    private static final String RULE_TAG_NAME = "rule";
    private @Nullable MeterRegistry meterRegistry;
    private final Set<Tag> tags = new HashSet<>();
    private ServiceRegistration<?> eventSubscriberRegistration;
    private BundleContext bundleContext;

    public RuleMetric(BundleContext bundleContext, Collection<Tag> tags) {
        this.tags.addAll(tags);
        this.tags.add(CORE_RULE_METRIC_TAG);
        this.bundleContext = bundleContext;
    }

    @Override
    public void bindTo(@io.micrometer.core.lang.NonNull MeterRegistry meterRegistry) {
        unbind();
        logger.debug("RuleMetric is being bound...");
        this.meterRegistry = meterRegistry;
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(SUBSCRIPTION_PROPERTY_TOPIC, RULES_TOPIC_FILTER);
        this.eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this,
                properties);
    }

    @Override
    public void unbind() {
        if (meterRegistry == null) {
            return;
        }
        for (Meter meter : meterRegistry.getMeters()) {
            if (meter.getId().getTags().contains(CORE_RULE_METRIC_TAG)) {
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
        subscribedEvents.add(RuleStatusInfoEvent.TYPE);
        return subscribedEvents;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        if (meterRegistry == null) {
            logger.trace("Measurement not started. Skipping rule event processing");
            return;
        }
        String topic = event.getTopic();
        String rule = topic.substring(RULES_TOPIC_PREFIX.length(), topic.indexOf(RULES_TOPIC_SUFFIX));
        if (!event.getPayload().contains(RuleStatus.RUNNING.name())) {
            logger.trace("Skipping rule status info with status other than RUNNING {}", event.getPayload());
            return;
        }

        logger.debug("Rule {} RUNNING - updating metric.", rule);
        Set<Tag> tagsWithRule = new HashSet<>(tags);
        tagsWithRule.add(Tag.of(RULE_TAG_NAME, rule));
        meterRegistry.counter(METRIC_NAME, tagsWithRule).increment();
    }
}
