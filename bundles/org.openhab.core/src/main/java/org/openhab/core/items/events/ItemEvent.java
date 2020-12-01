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
import org.openhab.core.events.AbstractEvent;

/**
 * {@link ItemEvent} is an abstract super class for all command and state item events.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public abstract class ItemEvent extends AbstractEvent {

    protected final String itemName;

    /**
     * Constructs a new item state event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param itemName the item name
     * @param source the source, can be null
     */
    public ItemEvent(String topic, String payload, String itemName, @Nullable String source) {
        super(topic, payload, source);
        this.itemName = itemName;
    }

    /**
     * Gets the item name.
     *
     * @return the item name
     */
    public String getItemName() {
        return itemName;
    }
}
