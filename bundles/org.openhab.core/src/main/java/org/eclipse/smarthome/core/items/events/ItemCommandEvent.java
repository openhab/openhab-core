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
import org.eclipse.smarthome.core.types.Command;

/**
 * {@link ItemCommandEvent}s can be used to deliver commands through the openHAB event bus.
 * Command events must be created with the {@link ItemEventFactory}.
 * 
 * @author Stefan Bußweiler - Initial contribution
 */
public class ItemCommandEvent extends AbstractEvent {

    /**
     * The item command event type.
     */
    public static final String TYPE = ItemCommandEvent.class.getSimpleName();

    private final String itemName;

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
    protected ItemCommandEvent(String topic, String payload, String itemName, Command command, String source) {
        super(topic, payload, source);
        this.itemName = itemName;
        this.command = command;
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
     * Gets the item command.
     * 
     * @return the item command
     */
    public Command getItemCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "Item '" + itemName + "' received command " + command;
    }

}
