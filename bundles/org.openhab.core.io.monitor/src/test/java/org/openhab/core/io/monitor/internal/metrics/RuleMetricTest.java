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

import static org.mockito.Mockito.mock;
import static org.openhab.core.io.monitor.internal.metrics.RuleMetric.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.osgi.framework.BundleContext;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests for RuleMetric class
 *
 * @author Robert Delbrück - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
class RuleMetricTest {

    public static final String RULE1 = "any";
    public static final String RULE2 = "anything-else";

    @Test
    void testRuleExecution() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        RuleMetric ruleMetric = new RuleMetric(mock(BundleContext.class), List.of(), mock(RuleRegistry.class));
        ruleMetric.bindTo(meterRegistry);
        fireRule(ruleMetric, RULE1);
        assertMeters(meterRegistry, 1);
        assertMeter(meterRegistry, RULE1, 1, 1);

        fireRule(ruleMetric, RULE2);
        assertMeters(meterRegistry, 2);
        assertMeter(meterRegistry, RULE2, 1, 1);

        // fire again, use the same metric
        fireRule(ruleMetric, RULE2);
        assertMeters(meterRegistry, 2);
        assertMeter(meterRegistry, RULE2, 2, 2);
    }

    private static void fireRule(RuleMetric ruleMetric, String ruleName) {
        ruleMetric.receive(new RuleStatusInfoEvent(RULES_TOPIC_PREFIX + ruleName + RULES_TOPIC_SUFFIX,
                RuleStatus.RUNNING.name(), ruleName, mock(RuleStatusInfo.class), ruleName));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        ruleMetric.receive(new RuleStatusInfoEvent(RULES_TOPIC_PREFIX + ruleName + RULES_TOPIC_SUFFIX,
                RuleStatus.IDLE.name(), ruleName, mock(RuleStatusInfo.class), ruleName));
    }

    private static void assertMeter(SimpleMeterRegistry meterRegistry, String ruleName, int count, int totalTime) {
        var meter = getMeters(meterRegistry).stream().filter(m -> m.getId().getTag("rule").equals(ruleName))
                .map(m -> (Timer) m).findFirst().orElseThrow();
        Assertions.assertEquals(count, meter.count());
        Assertions.assertTrue(meter.totalTime(TimeUnit.SECONDS) >= totalTime);
    }

    private static void assertMeters(SimpleMeterRegistry meterRegistry, int size) {
        List<Meter> durationMeters = getMeters(meterRegistry);
        Assertions.assertEquals(size, durationMeters.size());
    }

    private static @NonNull List<Meter> getMeters(SimpleMeterRegistry meterRegistry) {
        List<Meter> meters = meterRegistry.getMeters();
        return meters.stream().filter(m -> m.getId().getName().equals(METRIC_DURATION_NAME)).toList();
    }
}
