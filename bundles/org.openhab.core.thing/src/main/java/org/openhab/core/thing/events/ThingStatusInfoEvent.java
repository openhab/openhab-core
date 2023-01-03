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
package org.openhab.core.thing.events;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;

/**
 * {@link ThingStatusInfoEvent}s will be delivered through the openHAB event bus if the status of a thing has
 * been updated. Thing status info objects must be created with the {@link ThingEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public class ThingStatusInfoEvent extends AbstractEvent {

    /**
     * The thing status event type.
     */
    public static final String TYPE = ThingStatusInfoEvent.class.getSimpleName();

    private final ThingUID thingUID;

    private final ThingStatusInfo thingStatusInfo;

    /**
     * Creates a new thing status event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param thingUID the thing UID
     * @param thingStatusInfo the thing status info object
     */
    protected ThingStatusInfoEvent(String topic, String payload, ThingUID thingUID, ThingStatusInfo thingStatusInfo) {
        super(topic, payload, null);
        this.thingUID = thingUID;
        this.thingStatusInfo = thingStatusInfo;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the thing UID.
     *
     * @return the thing UID
     */
    public ThingUID getThingUID() {
        return thingUID;
    }

    /**
     * Gets the thing status info.
     *
     * @return the thing status info
     */
    public ThingStatusInfo getStatusInfo() {
        return thingStatusInfo;
    }

    @Override
    public String toString() {
        return String.format("Thing '%s' updated: %s", thingUID, thingStatusInfo);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        ThingStatusInfoEvent that = (ThingStatusInfoEvent) o;
        return thingUID.equals(that.thingUID) && thingStatusInfo.equals(that.thingStatusInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), thingUID, thingStatusInfo);
    }
}
