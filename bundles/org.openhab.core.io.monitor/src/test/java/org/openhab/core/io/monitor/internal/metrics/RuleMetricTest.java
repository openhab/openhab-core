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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.openhab.core.io.monitor.internal.metrics.RuleMetric.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusDetail;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.osgi.framework.BundleContext;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests for RuleMetric class
 *
 * @author Robert Delbrück - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
class RuleMetricTest {

    public static final String RULE1 = "any";
    public static final String RULE2 = "anything-else";

    @Test
    void testRuleExecution() {
        MockClock clock = new MockClock();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);

        RuleMetric ruleMetric = new RuleMetric(mock(BundleContext.class), List.of(), mock(RuleRegistry.class));
        ruleMetric.bindTo(meterRegistry);
        try {
            fireRule(ruleMetric, RULE1, Duration.ofSeconds(1));
            assertDurationMeters(meterRegistry, 1);
            assertDurationMeter(meterRegistry, RULE1, 1, 1);
            assertCounterMeter(meterRegistry, RULE1, 1);

            fireRule(ruleMetric, RULE2, Duration.ofSeconds(2));
            assertDurationMeters(meterRegistry, 2);
            assertDurationMeter(meterRegistry, RULE2, 1, 2);
            assertCounterMeter(meterRegistry, RULE2, 1);

            // fire again, use the same metric
            fireRule(ruleMetric, RULE2, Duration.ofSeconds(3));
            assertDurationMeters(meterRegistry, 2);
            assertDurationMeter(meterRegistry, RULE2, 2, 5);
            assertCounterMeter(meterRegistry, RULE2, 2);
        } finally {
            ruleMetric.unbind();
        }
    }

    @Test
    void testLongRunningRuleExecution() {
        MockClock clock = new MockClock();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);

        RuleMetric ruleMetric = new RuleMetric(mock(BundleContext.class), List.of(), mock(RuleRegistry.class));
        ruleMetric.bindTo(meterRegistry);

        try {
            // Rule runs for 10 minutes
            fireRule(ruleMetric, RULE1, Duration.ofMinutes(10));

            assertDurationMeters(meterRegistry, 1);
            assertDurationMeter(meterRegistry, RULE1, 1, 600);
            assertCounterMeter(meterRegistry, RULE1, 1);
        } finally {
            ruleMetric.unbind();
        }
    }

    @Test
    void testNoDurationMeterWhenIdleEventCarriesNoDuration() {
        MockClock clock = new MockClock();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);

        RuleMetric ruleMetric = new RuleMetric(mock(BundleContext.class), List.of(), mock(RuleRegistry.class));
        ruleMetric.bindTo(meterRegistry);

        try {
            String topic = RULES_TOPIC_PREFIX + RULE1 + RULES_TOPIC_SUFFIX;
            // Rule starts
            ruleMetric.receive(new RuleStatusInfoEvent(topic, "", null, new RuleStatusInfo(RuleStatus.RUNNING), RULE1));

            // IDLE event without a reported duration (e.g. not emitted by the rule engine's own execution tracking)
            ruleMetric.receive(new RuleStatusInfoEvent(topic, "", null, new RuleStatusInfo(RuleStatus.IDLE), RULE1));

            // No duration meter should be recorded
            assertDurationMeters(meterRegistry, 0);
            // Counter was incremented when RUNNING
            assertCounterMeter(meterRegistry, RULE1, 1);
        } finally {
            ruleMetric.unbind();
        }
    }

    @Test
    void testRuleMetricTagsWithRuleName() {
        MockClock clock = new MockClock();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);

        RuleRegistry ruleRegistry = mock(RuleRegistry.class);
        Rule rule = mock(Rule.class);
        when(rule.getName()).thenReturn("Friendly Rule Name");
        when(ruleRegistry.get(RULE1)).thenReturn(rule);

        RuleMetric ruleMetric = new RuleMetric(mock(BundleContext.class), List.of(), ruleRegistry);
        ruleMetric.bindTo(meterRegistry);

        try {
            fireRule(ruleMetric, RULE1, Duration.ofSeconds(1));

            Timer timer = getDurationMeters(meterRegistry).getFirst();
            assertEquals(RULE1, timer.getId().getTag("rule"));
            assertEquals("Friendly Rule Name", timer.getId().getTag("rulename"));

            Counter counter = getCounterMeters(meterRegistry).getFirst();
            assertEquals(RULE1, counter.getId().getTag("rule"));
            assertEquals("Friendly Rule Name", counter.getId().getTag("rulename"));
        } finally {
            ruleMetric.unbind();
        }
    }

    @Test
    void testUnbindClearsMeters() {
        MockClock clock = new MockClock();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);

        RuleMetric ruleMetric = new RuleMetric(mock(BundleContext.class), List.of(), mock(RuleRegistry.class));
        ruleMetric.bindTo(meterRegistry);

        fireRule(ruleMetric, RULE1, Duration.ofSeconds(1));
        assertEquals(1, getDurationMeters(meterRegistry).size());
        assertEquals(1, getCounterMeters(meterRegistry).size());

        ruleMetric.unbind();

        assertEquals(0, getDurationMeters(meterRegistry).size());
        assertEquals(0, getCounterMeters(meterRegistry).size());
    }

    private static void fireRule(RuleMetric ruleMetric, String ruleName, Duration duration) {
        String topic = RULES_TOPIC_PREFIX + ruleName + RULES_TOPIC_SUFFIX;
        ruleMetric.receive(new RuleStatusInfoEvent(topic, "", null, new RuleStatusInfo(RuleStatus.RUNNING), ruleName));
        ruleMetric.receive(new RuleStatusInfoEvent(topic, "", null,
                new RuleStatusInfo(RuleStatus.IDLE, RuleStatusDetail.NONE, null, duration.toMillis()), ruleName));
    }

    private static void assertDurationMeter(SimpleMeterRegistry meterRegistry, String ruleName, int count,
            int totalTimeSeconds) {
        var meter = getDurationMeters(meterRegistry).stream().filter(m -> ruleName.equals(m.getId().getTag("rule")))
                .findFirst().orElseThrow();
        assertEquals(count, meter.count());
        assertTrue(meter.totalTime(TimeUnit.SECONDS) >= totalTimeSeconds);
    }

    private static void assertCounterMeter(SimpleMeterRegistry meterRegistry, String ruleName, double count) {
        var counter = getCounterMeters(meterRegistry).stream().filter(c -> ruleName.equals(c.getId().getTag("rule")))
                .findFirst().orElseThrow();
        assertEquals(count, counter.count());
    }

    private static void assertDurationMeters(SimpleMeterRegistry meterRegistry, int size) {
        List<Timer> durationMeters = getDurationMeters(meterRegistry);
        assertEquals(size, durationMeters.size());
    }

    private static List<Timer> getDurationMeters(SimpleMeterRegistry meterRegistry) {
        List<Meter> meters = meterRegistry.getMeters();
        return meters.stream().filter(m -> m.getId().getName().equals(METRIC_DURATION_NAME)).map(m -> (Timer) m)
                .toList();
    }

    private static List<Counter> getCounterMeters(SimpleMeterRegistry meterRegistry) {
        List<Meter> meters = meterRegistry.getMeters();
        return meters.stream().filter(m -> m.getId().getName().equals(METRIC_NAME)).map(m -> (Counter) m).toList();
    }
}
