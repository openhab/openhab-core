/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.firmware;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.firmware.ProgressStep;
import org.eclipse.smarthome.test.java.JavaTest;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Dimitar Ivanov - Initial contribution
 *
 */
public class FirmwareEventFactoryTest extends JavaTest {

    static final ThingTypeUID THING_TYPE_UID1 = new ThingTypeUID("binding", "simpleID");
    ThingUID thingUID = new ThingUID(THING_TYPE_UID1, "idSample");

    private FirmwareEventFactory eventFactory;

    @Before
    public void setUp() {
        eventFactory = new FirmwareEventFactory();
    }

    @Test
    public void testSupportedEventTypes() {

        Set<String> supportedEventTypes = eventFactory.getSupportedEventTypes();
        assertThat(supportedEventTypes, containsInAnyOrder(FirmwareStatusInfoEvent.TYPE,
                FirmwareUpdateProgressInfoEvent.TYPE, FirmwareUpdateResultInfoEvent.TYPE));

    }

    @Test
    public void testCreateEventForUnknownType() throws Exception {
        Event event = eventFactory.createEventByType("unknownType", "topic", "somePayload", "Source");
        assertThat(event, is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullStatusInfo() {
        FirmwareEventFactory.createFirmwareStatusInfoEvent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullUpdateProgressInfo() {
        FirmwareEventFactory.createFirmwareUpdateProgressInfoEvent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullUpdateResultInfo() {
        FirmwareEventFactory.createFirmwareUpdateResultInfoEvent(null);
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
                        Arrays.asList(ProgressStep.WAITING, ProgressStep.UPDATING), false, 10);
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
