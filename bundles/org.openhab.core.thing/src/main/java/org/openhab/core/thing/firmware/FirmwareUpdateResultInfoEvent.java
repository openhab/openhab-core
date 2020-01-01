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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;

/**
 * The {@link FirmwareUpdateResultInfoEvent} is sent if the firmware update has been finished. It is created by the
 * {@link FirmwareEventFactory}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Removed thing UID from the event
 */
@NonNullByDefault
public final class FirmwareUpdateResultInfoEvent extends AbstractEvent {

    /** Constant for the firmware update result info event type. */
    public static final String TYPE = FirmwareUpdateResultInfoEvent.class.getSimpleName();

    private final FirmwareUpdateResultInfo firmwareUpdateResultInfo;

    /**
     * Creates a new {@link FirmwareUpdateResultInfoEvent}.
     *
     * @param topic the topic of the event
     * @param payload the payload of the event
     * @param firmwareUpdateResultInfo the firmware update result info to be sent as event
     */
    protected FirmwareUpdateResultInfoEvent(String topic, String payload,
            FirmwareUpdateResultInfo firmwareUpdateResultInfo) {
        super(topic, payload, null);
        this.firmwareUpdateResultInfo = firmwareUpdateResultInfo;
    }

    /**
     * Returns the firmware update result info.
     *
     * @return the firmware update result info
     */
    public FirmwareUpdateResultInfo getFirmwareUpdateResultInfo() {
        return firmwareUpdateResultInfo;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((firmwareUpdateResultInfo == null) ? 0 : firmwareUpdateResultInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FirmwareUpdateResultInfoEvent other = (FirmwareUpdateResultInfoEvent) obj;
        if (firmwareUpdateResultInfo == null) {
            if (other.firmwareUpdateResultInfo != null) {
                return false;
            }
        } else if (!firmwareUpdateResultInfo.equals(other.firmwareUpdateResultInfo)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        FirmwareUpdateResult result = firmwareUpdateResultInfo.getResult();

        StringBuilder sb = new StringBuilder(String.format("The result of the firmware update for thing %s is %s.",
                firmwareUpdateResultInfo.getThingUID(), result.name()));

        if (result == FirmwareUpdateResult.ERROR) {
            sb.append(String.format(" The error message is %s.", firmwareUpdateResultInfo.getErrorMessage()));
        }

        return sb.toString();
    }

}
