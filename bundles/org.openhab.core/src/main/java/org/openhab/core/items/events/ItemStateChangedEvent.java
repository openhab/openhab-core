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
 * {@link ItemStateChangedEvent}s can be used to deliver item state changes through the openHAB event bus. In contrast
 * to the {@link ItemStateEvent} the {@link ItemStateChangedEvent} is only sent if the state changed. State events must
 * be created with the {@link ItemEventFactory}.
 *
 * @author Dennis Nobel - Initial contribution
 */
@NonNullByDefault
public class ItemStateChangedEvent extends ItemEvent {

    /**
     * The item state changed event type.
     */
    public static final String TYPE = ItemStateChangedEvent.class.getSimpleName();

    protected final State itemState;

    protected final State oldItemState;

    protected final @Nullable ZonedDateTime lastStateUpdate;

    protected final @Nullable ZonedDateTime lastStateChange;

    /**
     * Constructs a new item state changed event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param itemName the item name
     * @param newItemState the new item state
     * @param oldItemState the old item state
     * @param lastStateUpdate the last state update
     * @param lastStateChange the last state change
     */
    protected ItemStateChangedEvent(String topic, String payload, String itemName, State newItemState,
            State oldItemState, @Nullable ZonedDateTime lastStateUpdate, @Nullable ZonedDateTime lastStateChange) {
        super(topic, payload, itemName, null);
        this.itemState = newItemState;
        this.oldItemState = oldItemState;
        this.lastStateUpdate = lastStateUpdate;
        this.lastStateChange = lastStateChange;
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
     * Gets the old item state.
     *
     * @return the old item state
     */
    public State getOldItemState() {
        return oldItemState;
    }

    /**
     * Gets the timestamp of the previous state update that occurred prior to this event.
     *
     * @return the last state update
     */
    public @Nullable ZonedDateTime getLastStateUpdate() {
        return lastStateUpdate;
    }

    /**
     * Gets the timestamp of the previous state change that occurred prior to this event.
     *
     * @return the last state change
     */
    public @Nullable ZonedDateTime getLastStateChange() {
        return lastStateChange;
    }

    @Override
    public String toString() {
        return String.format("Item '%s' changed from %s to %s", itemName, oldItemState, itemState);
    }
}
