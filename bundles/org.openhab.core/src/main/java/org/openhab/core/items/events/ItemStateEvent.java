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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;

/**
 * {@link ItemStateEvent}s can be used to deliver item status updates through the openHAB event bus.
 * State events must be created with the {@link ItemEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public class ItemStateEvent extends ItemEvent {

    /**
     * The item state event type.
     */
    public static final String TYPE = ItemStateEvent.class.getSimpleName();

    private final State itemState;

    /**
     * Constructs a new item state event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param itemName the item name
     * @param itemState the item state
     * @param source the source, can be null
     */
    protected ItemStateEvent(String topic, String payload, String itemName, State itemState, @Nullable String source) {
        super(topic, payload, itemName, source);
        this.itemState = itemState;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the item state.
     *
     * @return the item state
     */
    public State getItemState() {
        return itemState;
    }

    @Override
    public String toString() {
        return String.format("Item '%s' updated to %s", itemName, itemState);
    }
}
