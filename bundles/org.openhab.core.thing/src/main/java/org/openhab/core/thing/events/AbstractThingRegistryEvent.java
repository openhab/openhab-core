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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.dto.ThingDTO;

/**
 * Abstract implementation of a thing registry event which will be posted by a {@link ThingRegistry} for added, removed
 * and updated items.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractThingRegistryEvent extends AbstractEvent {

    private final ThingDTO thing;

    /**
     * Must be called in subclass constructor to create a new thing registry event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param source the source
     * @param thing the thing data transfer object
     */
    protected AbstractThingRegistryEvent(String topic, String payload, @Nullable String source, ThingDTO thing) {
        super(topic, payload, source);
        this.thing = thing;
    }

    /**
     * Gets the thing data transfer object.
     *
     * @return the thing data transfer object
     */
    public ThingDTO getThing() {
        return thing;
    }
}
