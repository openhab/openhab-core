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
 * {@link ThingStatusInfoEvent}s will be delivered through the openHAB event bus if the status of a thing has
 * been updated. Thing status info objects must be created with the {@link ThingEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
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
        return "'" + thingUID + "' updated: " + thingStatusInfo.toString();
    }

}
