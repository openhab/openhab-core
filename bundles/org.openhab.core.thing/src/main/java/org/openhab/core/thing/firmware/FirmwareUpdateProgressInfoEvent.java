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
 * The {@link FirmwareUpdateProgressInfoEvent} is sent if there is a new progress step for a firmware update. It is
 * created by the {@link FirmwareEventFactory}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Removed thing UID from the event
 */
@NonNullByDefault
public final class FirmwareUpdateProgressInfoEvent extends AbstractEvent {

    /** Constant for the firmware update progress info event type. */
    public static final String TYPE = FirmwareUpdateProgressInfoEvent.class.getSimpleName();

    private final FirmwareUpdateProgressInfo progressInfo;

    /**
     * Creates a new {@link FirmwareUpdateProgressInfoEvent}.
     *
     * @param topic the topic of the event
     * @param payload the payload of the event
     * @param progressInfo the progress info to be sent with the event
     */
    protected FirmwareUpdateProgressInfoEvent(String topic, String payload, FirmwareUpdateProgressInfo progressInfo) {
        super(topic, payload, null);
        this.progressInfo = progressInfo;
    }

    /**
     * Returns the {@link FirmwareUpdateProgressInfo}.
     *
     * @return the firmware update progress info
     */
    public FirmwareUpdateProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((progressInfo == null) ? 0 : progressInfo.hashCode());
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
        FirmwareUpdateProgressInfoEvent other = (FirmwareUpdateProgressInfoEvent) obj;
        if (progressInfo == null) {
            if (other.progressInfo != null) {
                return false;
            }
        } else if (!progressInfo.equals(other.progressInfo)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String stepName = progressInfo.getProgressStep() == null ? null : progressInfo.getProgressStep().name();
        return String.format("The firmware update progress for thing %s changed. Step: %s Progress: %d.",
                progressInfo.getThingUID(), stepName, progressInfo.getProgress());
    }

}
