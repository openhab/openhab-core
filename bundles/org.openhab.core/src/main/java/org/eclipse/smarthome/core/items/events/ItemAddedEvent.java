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
package org.eclipse.smarthome.core.items.events;

import org.eclipse.smarthome.core.items.dto.ItemDTO;

/**
 * An {@link ItemAddedEvent} notifies subscribers that an item has been added.
 * Item added events must be created with the {@link ItemEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public class ItemAddedEvent extends AbstractItemRegistryEvent {

    /**
     * The item added event type.
     */
    public static final String TYPE = ItemAddedEvent.class.getSimpleName();

    /**
     * Constructs a new item added event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param item the item data transfer object
     */
    protected ItemAddedEvent(String topic, String payload, ItemDTO item) {
        super(topic, payload, null, item);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "Item '" + getItem().name + "' has been added.";
    }

}
