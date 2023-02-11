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
package org.openhab.core.thing.firmware;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.Event;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.firmware.ProgressStep;

/**
 *
 * @author Dimitar Ivanov - Initial contribution
 */
@NonNullByDefault
public class FirmwareEventFactoryTest extends JavaTest {

    private static final ThingTypeUID THING_TYPE_UID1 = new ThingTypeUID("binding", "simpleID");
    private final ThingUID thingUID = new ThingUID(THING_TYPE_UID1, "idSample");

    private @NonNullByDefault({}) FirmwareEventFactory eventFactory;

    @BeforeEach
    public void setUp() {
        eventFactory = new FirmwareEventFactory();
    }

    @Test
    public void testSupportedEventTypes() {
        assertThat(eventFactory.getSupportedEventTypes(), containsInAnyOrder(FirmwareStatusInfoEvent.TYPE,
                FirmwareUpdateProgressInfoEvent.TYPE, FirmwareUpdateResultInfoEvent.TYPE));
    }

    @Test
    public void testCreateEventForUnknownType() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> eventFactory.createEventByType("unknownType", "topic", "somePayload", "Source"));
    }

    @Test
    public void testSerializationAndDeserializationFirmwareStatusInfo() throws Exception {
        FirmwareStatusInfo firmwareStatusInfo = FirmwareStatusInfo.createUpdateAvailableInfo(thingUID);

        // The info is serialized into the event payload
        FirmwareStatusInfoEvent firmwareStatusInfoEvent = FirmwareEventFactory
                .createFirmwareStatusInfoEvent(firmwareStatusInfo);

        // Deserialize
        Event event = eventFactory.createEventByType(FirmwareStatusInfoEvent.TYPE, "topic",
                firmwareStatusInfoEvent.getPayload(), null);

        assertThat(event, is(instanceOf(FirmwareStatusInfoEvent.class)));
        assertThat(event.getType(), is(FirmwareStatusInfoEvent.TYPE));
        FirmwareStatusInfo deserializedStatusInfo = ((FirmwareStatusInfoEvent) event).getFirmwareStatusInfo();
        assertThat(firmwareStatusInfo, is(deserializedStatusInfo));
    }

    @Test
    public void testSerializationAndDeserializationFirmwareUpdateProgressInfo() throws Exception {
        FirmwareUpdateProgressInfo firmwareUpdateProgressInfo = FirmwareUpdateProgressInfo
                .createFirmwareUpdateProgressInfo(thingUID, "1.2.3", ProgressStep.UPDATING,
                        List.of(ProgressStep.WAITING, ProgressStep.UPDATING), false, 10);
        FirmwareUpdateProgressInfoEvent progressInfoEvent = FirmwareEventFactory
                .createFirmwareUpdateProgressInfoEvent(firmwareUpdateProgressInfo);

        // Deserialize
        Event event = eventFactory.createEventByType(FirmwareUpdateProgressInfoEvent.TYPE, "topic",
                progressInfoEvent.getPayload(), null);

        assertThat(event, is(instanceOf(FirmwareUpdateProgressInfoEvent.class)));
        FirmwareUpdateProgressInfo deserializedStatusInfo = ((FirmwareUpdateProgressInfoEvent) event).getProgressInfo();
        assertThat(firmwareUpdateProgressInfo, is(deserializedStatusInfo));
    }

    @Test
    public void testSerializationAndDeserializationFirmwareUpdateResultInfo() throws Exception {
        FirmwareUpdateResultInfo firmwareUpdateResultInfo = FirmwareUpdateResultInfo
                .createFirmwareUpdateResultInfo(thingUID, FirmwareUpdateResult.ERROR, "error message");

        FirmwareUpdateResultInfoEvent firmwareUpdateResultInfoEvent = FirmwareEventFactory
                .createFirmwareUpdateResultInfoEvent(firmwareUpdateResultInfo);

        Event event = eventFactory.createEventByType(FirmwareUpdateResultInfoEvent.TYPE, "topic",
                firmwareUpdateResultInfoEvent.getPayload(), "Source");

        assertThat(event, is(instanceOf(FirmwareUpdateResultInfoEvent.class)));
        FirmwareUpdateResultInfo updateResultInfo = ((FirmwareUpdateResultInfoEvent) event)
                .getFirmwareUpdateResultInfo();
        assertThat(firmwareUpdateResultInfo, is(updateResultInfo));
    }
}
