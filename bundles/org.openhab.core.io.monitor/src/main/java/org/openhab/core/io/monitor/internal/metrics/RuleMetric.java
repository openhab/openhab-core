/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.openhab.core.cache.ExpiringCacheMap;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventSubscriber;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.*;

/**
 * The {@link RuleMetric} class implements a gauge metric for rules RUNNING events (per rule)
 *
 * @author Robert Bach - Initial contribution
 */
@NonNullByDefault
public class RuleMetric implements OpenhabCoreMeterBinder, EventSubscriber {

    public static final String METRIC_NAME = "openhab.rule.runs";
    public static final String METRIC_DURATION_NAME = "openhab.rule.duration";
    public static final String RULES_TOPIC_PREFIX = "openhab/rules/";
    public static final String RULES_TOPIC_SUFFIX = "/state";
    private final Logger logger = LoggerFactory.getLogger(RuleMetric.class);
    private static final Tag CORE_RULE_METRIC_TAG = Tag.of("metric", "openhab.core.metric.rules");
    private static final String RULE_ID_TAG_NAME = "rule";
    private static final String RULE_NAME_TAG_NAME = "rulename";
    private @Nullable MeterRegistry meterRegistry;
    private final Set<Tag> tags = new HashSet<>();
    private @Nullable ServiceRegistration<?> eventSubscriberRegistration;
    private BundleContext bundleContext;
    private RuleRegistry ruleRegistry;
    private final ExpiringCacheMap<String, Timer.Sample> cache = new ExpiringCacheMap<>(Duration.ofMinutes(5));

    public RuleMetric(BundleContext bundleContext, Collection<Tag> tags, RuleRegistry ruleRegistry) {
        this.tags.addAll(tags);
        this.tags.add(CORE_RULE_METRIC_TAG);
        this.bundleContext = bundleContext;
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public void bindTo(@NonNullByDefault({}) MeterRegistry meterRegistry) {
        unbind();
        logger.debug("RuleMetric is being bound...");
        this.meterRegistry = meterRegistry;
        eventSubscriberRegistration = this.bundleContext.registerService(EventSubscriber.class.getName(), this, null);
    }

    @Override
    public void unbind() {
        MeterRegistry meterRegistry = this.meterRegistry;
        if (meterRegistry == null) {
            return;
        }
        for (Meter meter : meterRegistry.getMeters()) {
            if (meter.getId().getTags().contains(CORE_RULE_METRIC_TAG)) {
                meterRegistry.remove(meter);
            }
        }
        this.meterRegistry = null;

        ServiceRegistration<?> eventSubscriberRegistration = this.eventSubscriberRegistration;
        if (eventSubscriberRegistration != null) {
            eventSubscriberRegistration.unregister();
            this.eventSubscriberRegistration = null;
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(RuleStatusInfoEvent.TYPE);
    }

    @Override
    public void receive(Event event) {
        MeterRegistry meterRegistry = this.meterRegistry;
        if (meterRegistry == null) {
            logger.trace("Measurement not started. Skipping rule event processing");
            return;
        }

        String topic = event.getTopic();
        String ruleId = topic.substring(RULES_TOPIC_PREFIX.length(), topic.lastIndexOf(RULES_TOPIC_SUFFIX));
        String ruleStatus = event.getPayload();

        Set<Tag> tagsWithRule = createTags(ruleId);

        if (ruleStatus.contains(RuleStatus.RUNNING.name())) {
            logger.debug("Rule {} RUNNING - updating metric.", ruleId);
            Counter.builder(METRIC_NAME).description("Execution count of the rules").tags(tagsWithRule)
                    .register(this.meterRegistry).increment();
            cache.put(topic, () -> Timer.start(meterRegistry));
        } else if (ruleStatus.contains(RuleStatus.IDLE.name())) {
            Timer.Sample sample = cache.get(topic);
            if (sample != null) {
                Timer timer = Timer.builder(METRIC_DURATION_NAME).description("Execution duration of the rules")
                        .tags(tagsWithRule).register(meterRegistry);
                long duration = sample.stop(timer);
                logger.debug("Rule {} Finished - updating duration metric ({}ns).", ruleId, duration);
            } else {
                logger.trace("Rule {} Finished - but running state missed.", ruleId);
            }
        } else {
            logger.trace("Skipping rule status info with status {}", ruleStatus);
        }
    }

    private @NonNull Set<Tag> createTags(String ruleId) {
        Set<Tag> tagsWithRule = new HashSet<>(tags);
        tagsWithRule.add(Tag.of(RULE_ID_TAG_NAME, ruleId));
        String ruleName = getRuleName(ruleId);
        if (ruleName != null) {
            tagsWithRule.add(Tag.of(RULE_NAME_TAG_NAME, ruleName));
        }
        return tagsWithRule;
    }

    private @Nullable String getRuleName(String ruleId) {
        Rule rule = ruleRegistry.get(ruleId);
        return rule == null ? null : rule.getName();
    }
}
