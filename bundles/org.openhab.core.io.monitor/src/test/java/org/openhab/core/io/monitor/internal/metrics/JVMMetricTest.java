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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests the lifecycle of {@link JVMMetric}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
class JVMMetricTest {

    @Test
    void closesGcMetricsOnRebindAndUnbind() {
        SimpleMeterRegistry firstRegistry = new SimpleMeterRegistry();
        SimpleMeterRegistry secondRegistry = new SimpleMeterRegistry();
        try (MockedConstruction<JvmGcMetrics> construction = mockConstruction(JvmGcMetrics.class)) {
            JVMMetric metric = new JVMMetric(List.of());
            try {
                metric.bindTo(firstRegistry);
                JvmGcMetrics firstMetrics = construction.constructed().getFirst();
                verify(firstMetrics).bindTo(firstRegistry);
                verify(firstMetrics, never()).close();

                metric.bindTo(secondRegistry);
                verify(firstMetrics).close();
                assertTrue(firstRegistry.getMeters().isEmpty());
                verify(construction.constructed().getLast()).bindTo(secondRegistry);
            } finally {
                metric.unbind();
            }
            metric.unbind();
            verify(construction.constructed().getLast()).close();
            assertTrue(secondRegistry.getMeters().isEmpty());
        } finally {
            firstRegistry.close();
            secondRegistry.close();
        }
    }
}
