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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.OnOffType;
import org.osgi.framework.BundleContext;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests for EventCountMetric class
 *
 * @author Robert Delbrück - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
class EventCountMetricTest {

    @Test
    void testEventIncrementsMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        EventCountMetric eventCountMetric = new EventCountMetric(mock(BundleContext.class), List.of());
        eventCountMetric.bindTo(meterRegistry);

        try {
            eventCountMetric.receive(ItemEventFactory.createStateEvent("myItem", OnOffType.ON));
            eventCountMetric.receive(ItemEventFactory.createStateEvent("myItem", OnOffType.ON));

            Counter counter = meterRegistry.getMeters().stream()
                    .filter(m -> m.getId().getName().equals(EventCountMetric.METRIC_NAME)).map(m -> (Counter) m)
                    .findFirst().orElseThrow();
            assertEquals(2, counter.count());
        } finally {
            eventCountMetric.unbind();
        }
    }
}
