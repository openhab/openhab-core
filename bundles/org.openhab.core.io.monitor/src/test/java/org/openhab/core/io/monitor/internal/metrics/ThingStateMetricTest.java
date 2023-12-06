/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.internal.ThingImpl;
import org.osgi.framework.BundleContext;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Tests for ThingStateMetric class
 *
 * @author Scott Hraban - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class ThingStateMetricTest {

    @Test
    public void testThingUidAlwaysUsedToCreateMeter() {
        final String strThingTypeUid = "sonos:Amp";

        final String strThingUid = strThingTypeUid + ":RINCON_347E5C0D150501400";
        ThingUID thingUid = new ThingUID(strThingUid);
        Thing thing = new ThingImpl(new ThingTypeUID(strThingTypeUid), thingUid);

        final String strThingUid2 = strThingTypeUid + ":foo";
        ThingUID thingUid2 = new ThingUID(strThingUid2);

        ThingRegistry thingRegistry = mock(ThingRegistry.class);

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        ThingStateMetric thingStateMetric = new ThingStateMetric(mock(BundleContext.class), thingRegistry, Set.of());

        // Only one meter registered at bind time
        doReturn(List.of(thing)).when(thingRegistry).getAll();
        thingStateMetric.bindTo(meterRegistry);

        List<Meter> meters = meterRegistry.getMeters();
        assertEquals(1, meters.size());
        assertEquals(strThingUid, meters.get(0).getId().getTag("thing"));

        // Still only one meter registered after receiving an event
        ThingStatusInfo thingStatusInfo = new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, null);
        thingStateMetric.receive(ThingEventFactory.createStatusInfoEvent(thingUid, thingStatusInfo));

        meters = meterRegistry.getMeters();
        assertEquals(1, meters.size());
        assertEquals(strThingUid, meters.get(0).getId().getTag("thing"));

        // Now another one is added
        thingStateMetric.receive(ThingEventFactory.createStatusInfoEvent(thingUid2, thingStatusInfo));

        meters = meterRegistry.getMeters();
        assertEquals(2, meters.size());
    }
}
