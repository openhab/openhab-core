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

import org.eclipse.smarthome.core.events.AbstractEvent;
import org.eclipse.smarthome.core.types.State;

/**
 * {@link ItemStateChangedEvent}s can be used to deliver item state changes through the Eclipse SmartHome event bus. In
 * contrast to the {@link ItemStateEvent} the {@link ItemStateChangedEvent} is only sent if the state changed.
 * State events must be created with the {@link ItemEventFactory}.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class ItemStateChangedEvent extends AbstractEvent {

    /**
     * The item state changed event type.
     */
    public static final String TYPE = ItemStateChangedEvent.class.getSimpleName();

    protected final String itemName;

    protected final State itemState;

    protected final State oldItemState;

    /**
     * Constructs a new item state changed event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param itemName the item name
     * @param newItemState the new item state
     * @param oldItemState the old item state
     */
    protected ItemStateChangedEvent(String topic, String payload, String itemName, State newItemState,
            State oldItemState) {
        super(topic, payload, null);
        this.itemName = itemName;
        this.itemState = newItemState;
        this.oldItemState = oldItemState;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the item name.
     *
     * @return the item name
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Gets the item state.
     *
     * @return the item state
     */
    public State getItemState() {
        return itemState;
    }

    /**
     * Gets the old item state.
     *
     * @return the old item state
     */
    public State getOldItemState() {
        return oldItemState;
    }

    @Override
    public String toString() {
        return String.format("%s changed from %s to %s", itemName, oldItemState, itemState);
    }

}
