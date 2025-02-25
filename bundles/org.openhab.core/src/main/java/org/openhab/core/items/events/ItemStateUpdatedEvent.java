/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;

/**
 * {@link ItemStateUpdatedEvent}s can be used to report item status updates through the openHAB event bus.
 * State update events must be created with the {@link ItemEventFactory}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ItemStateUpdatedEvent extends ItemEvent {

    /**
     * The item state event type.
     */
    public static final String TYPE = ItemStateUpdatedEvent.class.getSimpleName();

    protected final State itemState;
    protected final @Nullable ZonedDateTime lastStateUpdate;

    /**
     * Constructs a new item state event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param itemName the item name
     * @param itemState the item state
     * @param lastStateUpdate the last state update
     * @param source the source, can be null
     */
    protected ItemStateUpdatedEvent(String topic, String payload, String itemName, State itemState,
            @Nullable ZonedDateTime lastStateUpdate, @Nullable String source) {
        super(topic, payload, itemName, source);
        this.itemState = itemState;
        this.lastStateUpdate = lastStateUpdate;
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

    /**
     * Gets the timestamp of the previous state update that occurred prior to this event.
     *
     * @return the last state update
     */
    public @Nullable ZonedDateTime getLastStateUpdate() {
        return lastStateUpdate;
    }

    @Override
    public String toString() {
        return String.format("Item '%s' updated to %s", itemName, itemState);
    }
}
