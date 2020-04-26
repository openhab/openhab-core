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
package org.openhab.core.items.events;

import org.openhab.core.items.dto.ItemDTO;

/**
 * An {@link ItemUpdatedEvent} notifies subscribers that an item has been updated.
 * Item updated events must be created with the {@link ItemEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public class ItemUpdatedEvent extends AbstractItemRegistryEvent {

    private final ItemDTO oldItem;

    /**
     * The item updated event type.
     */
    public static final String TYPE = ItemUpdatedEvent.class.getSimpleName();

    /**
     * Constructs a new item updated event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param item the item data transfer object
     * @param oldItem the old item data transfer object
     */
    protected ItemUpdatedEvent(String topic, String payload, ItemDTO item, ItemDTO oldItem) {
        super(topic, payload, null, item);
        this.oldItem = oldItem;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the old item.
     * 
     * @return the oldItem
     */
    public ItemDTO getOldItem() {
        return oldItem;
    }

    @Override
    public String toString() {
        return "Item '" + getItem().name + "' has been updated.";
    }
}
