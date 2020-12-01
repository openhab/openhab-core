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
import org.openhab.core.types.Command;

/**
 * {@link ItemCommandEvent}s can be used to deliver commands through the openHAB event bus.
 * Command events must be created with the {@link ItemEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public class ItemCommandEvent extends ItemEvent {

    /**
     * The item command event type.
     */
    public static final String TYPE = ItemCommandEvent.class.getSimpleName();

    private final Command command;

    /**
     * Constructs a new item command event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param itemName the item name
     * @param command the command
     * @param source the source, can be null
     */
    protected ItemCommandEvent(String topic, String payload, String itemName, Command command,
            @Nullable String source) {
        super(topic, payload, itemName, source);
        this.command = command;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the item command.
     *
     * @return the item command
     */
    public Command getItemCommand() {
        return command;
    }

    @Override
    public String toString() {
        return String.format("Item '%s' received command %s", itemName, command);
    }
}
