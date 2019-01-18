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
 * A {@link ThingRemovedEvent} notifies subscribers that a thing has been removed.
 * Thing removed events must be created with the {@link ThingEventFactory}. 
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public class ThingRemovedEvent extends AbstractThingRegistryEvent {

    /**
     * The thing removed event type.
     */
    public static final String TYPE = ThingRemovedEvent.class.getSimpleName();

    /**
     * Constructs a new thing removed event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param thing the thing data transfer object
     */
    protected ThingRemovedEvent(String topic, String payload, ThingDTO thing) {
        super(topic, payload, null, thing);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Thing '" + getThing().UID + "' has been removed.";
    }

}
