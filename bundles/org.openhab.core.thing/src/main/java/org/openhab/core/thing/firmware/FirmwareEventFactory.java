/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openhab.core.events.AbstractEventFactory;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link FirmwareEventFactory} is registered as an OSGi service and is responsible to create firmware events. It
 * supports the following event types:
 * <ul>
 * <li>{@link FirmwareStatusInfoEvent#TYPE}</li>
 * <li>{@link FirmwareUpdateProgressInfoEvent#TYPE}</li>
 * <li>{@link FirmwareUpdateResultInfoEvent#TYPE}</li>
 * </ul>
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Consolidated all the event information into the FirmwareStatusInfoEvent
 */
@Component(immediate = true, service = EventFactory.class)
public final class FirmwareEventFactory extends AbstractEventFactory {

    static final String THING_UID_TOPIC_KEY = "{thingUID}";

    static final String FIRMWARE_STATUS_TOPIC = "smarthome/things/{thingUID}/firmware/status";
    static final String FIRMWARE_UPDATE_PROGRESS_TOPIC = "smarthome/things/{thingUID}/firmware/update/progress";
    static final String FIRMWARE_UPDATE_RESULT_TOPIC = "smarthome/things/{thingUID}/firmware/update/result";

    /**
     * Creates a new firmware event factory.
     */
    public FirmwareEventFactory() {
        super(Stream.of(FirmwareStatusInfoEvent.TYPE, FirmwareUpdateProgressInfoEvent.TYPE,
                FirmwareUpdateResultInfoEvent.TYPE).collect(Collectors.toSet()));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, String source) throws Exception {
        if (FirmwareStatusInfoEvent.TYPE.equals(eventType)) {
            return createFirmwareStatusInfoEvent(topic, payload);
        } else if (FirmwareUpdateProgressInfoEvent.TYPE.equals(eventType)) {
            return createFirmwareUpdateProgressInfoEvent(topic, payload);
        } else if (FirmwareUpdateResultInfoEvent.TYPE.equals(eventType)) {
            return createFirmwareUpdateResultInfoEvent(topic, payload);
        }
        throw new IllegalArgumentException("The event type '" + eventType + "' is not supported by this factory.");
    }

    /**
     * Creates a new {@link FirmwareStatusInfoEvent}.
     *
     * @param firmwareStatusInfo the firmware status information (must not be null)
     * @param thingUID the thing UID for which the new firmware status info is to be sent (must not be null)
     * @return the corresponding firmware status info event
     * @throws IllegalArgumentException if given firmware status info is null
     */
    public static FirmwareStatusInfoEvent createFirmwareStatusInfoEvent(FirmwareStatusInfo firmwareStatusInfo) {
        checkNotNull(firmwareStatusInfo, "firmwareStatusInfo");

        String topic = FIRMWARE_STATUS_TOPIC.replace(THING_UID_TOPIC_KEY,
                firmwareStatusInfo.getThingUID().getAsString());
        String payload = serializePayload(firmwareStatusInfo);

        return new FirmwareStatusInfoEvent(topic, payload, firmwareStatusInfo);
    }

    /**
     * Creates a new {@link FirmwareUpdateProgressInfoEvent}.
     *
     * @param progressInfo the progress information of the firmware update process (must not be null)
     * @param thingUID the thing UID for which the progress info is to be sent (must not be null)
     * @return the corresponding progress info event
     * @throws IllegalArgumentException if given progress info is null
     */
    public static FirmwareUpdateProgressInfoEvent createFirmwareUpdateProgressInfoEvent(
            FirmwareUpdateProgressInfo progressInfo) {
        checkNotNull(progressInfo, "progressInfo");

        String topic = FIRMWARE_UPDATE_PROGRESS_TOPIC.replace(THING_UID_TOPIC_KEY,
                progressInfo.getThingUID().getAsString());
        String payload = serializePayload(progressInfo);

        return new FirmwareUpdateProgressInfoEvent(topic, payload, progressInfo);
    }

    /**
     * Creates a new {@link FirmwareUpdateResultInfoEvent}.
     *
     * @param firmwareUpdateResultInfo the firmware update result information (must not be null)
     * @param thingUID the thing UID for which the result information is to be sent (must not be null)
     * @return the corresponding firmware update result info event
     * @throws IllegalArgumentException if given firmware update result info event is null
     */
    public static FirmwareUpdateResultInfoEvent createFirmwareUpdateResultInfoEvent(
            FirmwareUpdateResultInfo firmwareUpdateResultInfo) {
        checkNotNull(firmwareUpdateResultInfo, "firmwareUpdateResultInfo");

        String topic = FIRMWARE_UPDATE_RESULT_TOPIC.replace(THING_UID_TOPIC_KEY,
                firmwareUpdateResultInfo.getThingUID().getAsString());
        String payload = serializePayload(firmwareUpdateResultInfo);

        return new FirmwareUpdateResultInfoEvent(topic, payload, firmwareUpdateResultInfo);
    }

    private static FirmwareStatusInfoEvent createFirmwareStatusInfoEvent(String topic, String payload) {
        FirmwareStatusInfo firmwareStatusInfo = deserializePayload(payload, FirmwareStatusInfo.class);
        return new FirmwareStatusInfoEvent(topic, payload, firmwareStatusInfo);
    }

    private static FirmwareUpdateProgressInfoEvent createFirmwareUpdateProgressInfoEvent(String topic, String payload) {
        FirmwareUpdateProgressInfo firmwareUpdateProgressInfo = deserializePayload(payload,
                FirmwareUpdateProgressInfo.class);
        return new FirmwareUpdateProgressInfoEvent(topic, payload, firmwareUpdateProgressInfo);
    }

    private static FirmwareUpdateResultInfoEvent createFirmwareUpdateResultInfoEvent(String topic, String payload) {
        FirmwareUpdateResultInfo firmwareUpdateResultInfo = deserializePayload(payload, FirmwareUpdateResultInfo.class);
        return new FirmwareUpdateResultInfoEvent(topic, payload, firmwareUpdateResultInfo);
    }

}
