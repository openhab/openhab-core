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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests for RuleMetric class
 *
 * @author Robert Delbrück - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
class RuleMetricTest {
    @Test
    void testRuleExecution() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        RuleMetric ruleMetric = new RuleMetric(mock(BundleContext.class), List.of(), mock(RuleRegistry.class));
        ruleMetric.bindTo(meterRegistry);
        fireRule(ruleMetric, "any");
        assertMeters(meterRegistry, 1);
        fireRule(ruleMetric, "anything-else");
        assertMeters(meterRegistry, 2);

        // fire again, use the same metric
        fireRule(ruleMetric, "anything-else");
        assertMeters(meterRegistry, 2);
    }

    private static void fireRule(RuleMetric ruleMetric, String ruleName) {
        ruleMetric.receive(new RuleStatusInfoEvent(RULES_TOPIC_PREFIX + ruleName + RULES_TOPIC_SUFFIX,
                RuleStatus.RUNNING.name(), ruleName, mock(RuleStatusInfo.class), ruleName));
        ruleMetric.receive(new RuleStatusInfoEvent(RULES_TOPIC_PREFIX + ruleName + RULES_TOPIC_SUFFIX,
                RuleStatus.IDLE.name(), ruleName, mock(RuleStatusInfo.class), ruleName));
    }

    private static void assertMeters(SimpleMeterRegistry meterRegistry, int size) {
        List<Meter> meters = meterRegistry.getMeters();
        List<Meter> durationMeters = meters.stream().filter(m -> m.getId().getName().equals(METRIC_DURATION_NAME))
                .toList();
        Assertions.assertEquals(size, durationMeters.size());
    }
}
