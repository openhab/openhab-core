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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.events.AbstractEvent;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 * The {@link FirmwareStatusInfoEvent} is sent if the {@link FirmwareStatusInfo} of a {@link Thing} has been changed.
 * It is created by the {@link FirmwareEventFactory}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Removed thing UID from the event
 */
@NonNullByDefault
public final class FirmwareStatusInfoEvent extends AbstractEvent {

    /** Constant for the firmware status info event type. */
    public static final String TYPE = FirmwareStatusInfoEvent.class.getSimpleName();

    private final FirmwareStatusInfo firmwareStatusInfo;

    /**
     * Creates a new {@link FirmwareStatusInfoEvent}.
     *
     * @param topic the topic of the event
     * @param payload the payload of the event
     * @param firmwareStatusInfo the firmware status info to be sent with the event
     */
    protected FirmwareStatusInfoEvent(String topic, String payload, FirmwareStatusInfo firmwareStatusInfo) {
        super(topic, payload, null);
        this.firmwareStatusInfo = firmwareStatusInfo;
    }

    /**
     * Returns the firmware status info.
     *
     * @return the firmware status info
     */
    public FirmwareStatusInfo getFirmwareStatusInfo() {
        return firmwareStatusInfo;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((firmwareStatusInfo == null) ? 0 : firmwareStatusInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FirmwareStatusInfoEvent other = (FirmwareStatusInfoEvent) obj;
        if (firmwareStatusInfo == null) {
            if (other.firmwareStatusInfo != null) {
                return false;
            }
        } else if (!firmwareStatusInfo.equals(other.firmwareStatusInfo)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        FirmwareStatus status = firmwareStatusInfo.getFirmwareStatus();
        ThingUID thingUID = firmwareStatusInfo.getThingUID();

        StringBuilder sb = new StringBuilder(
                String.format("Firmware status of thing %s changed to %s.", thingUID, status.name()));

        if (status == FirmwareStatus.UPDATE_EXECUTABLE) {
            sb.append(String.format("The new updatable firmware version is %s.",
                    firmwareStatusInfo.getUpdatableFirmwareVersion()));
        }

        return sb.toString();
    }

}
