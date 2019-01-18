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

import org.eclipse.smarthome.core.thing.dto.ThingDTO;

/**
 * A {@link ThingUpdatedEvent} notifies subscribers that a thing has been updated.
 * Thing updated events must be created with the {@link ThingEventFactory}. 
 * 
 * @author Stefan Bußweiler - Initial contribution
 */
public class ThingUpdatedEvent extends AbstractThingRegistryEvent {

    /**
     * The thing updated event type.
     */
    public static final String TYPE = ThingUpdatedEvent.class.getSimpleName();

    private final ThingDTO oldThing;

    /**
     * Constructs a new thing updated event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param thing the thing data transfer object
     * @param oldThing the old thing data transfer object
     */
    protected ThingUpdatedEvent(String topic, String payload, ThingDTO thing, ThingDTO oldThing) {
        super(topic, payload, null, thing);
        this.oldThing = oldThing;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the old thing.
     * 
     * @return the oldThing
     */
    public ThingDTO getOldThing() {
        return oldThing;
    }

    @Override
    public String toString() {
        return "Thing '" + getThing().UID + "' has been updated.";
    }

}
