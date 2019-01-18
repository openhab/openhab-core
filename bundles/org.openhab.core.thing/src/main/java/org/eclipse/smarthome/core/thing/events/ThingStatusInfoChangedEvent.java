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
package org.eclipse.smarthome.core.thing.events;

import org.eclipse.smarthome.core.events.AbstractEvent;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;

/**
 * {@link ThingStatusInfoChangedEvent}s will be delivered through the Eclipse SmartHome event bus if the status of a
 * thing has changed. Thing status info objects must be created with the {@link ThingEventFactory}.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class ThingStatusInfoChangedEvent extends AbstractEvent {

    /**
     * The thing status event type.
     */
    public static final String TYPE = ThingStatusInfoChangedEvent.class.getSimpleName();

    private final ThingUID thingUID;

    private final ThingStatusInfo thingStatusInfo;

    private final ThingStatusInfo oldStatusInfo;

    /**
     * Creates a new thing status event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param thingUID the thing UID
     * @param thingStatusInfo the thing status info object
     * @param thingStatusInfo the old thing status info object
     */
    protected ThingStatusInfoChangedEvent(String topic, String payload, ThingUID thingUID,
            ThingStatusInfo newThingStatusInfo, ThingStatusInfo oldThingStatusInfo) {
        super(topic, payload, null);
        this.thingUID = thingUID;
        this.thingStatusInfo = newThingStatusInfo;
        this.oldStatusInfo = oldThingStatusInfo;
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

    /**
     * Gets the old thing status info.
     *
     * @return the old thing status info
     */
    public ThingStatusInfo getOldStatusInfo() {
        return oldStatusInfo;
    }

    @Override
    public String toString() {
        return "'" + thingUID + "' changed from " + oldStatusInfo.toString() + " to " + thingStatusInfo.toString();
    }

}
