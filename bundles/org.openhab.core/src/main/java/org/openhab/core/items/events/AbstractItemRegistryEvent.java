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
package org.openhab.core.items.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.dto.ItemDTO;

/**
 * Abstract implementation of an item registry event which will be posted by the {@link ItemRegistry} for added, removed
 * and updated items.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractItemRegistryEvent extends AbstractEvent {

    private final ItemDTO item;

    /**
     * Must be called in subclass constructor to create a new item registry event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param source the source
     * @param item the item data transfer object
     */
    protected AbstractItemRegistryEvent(String topic, String payload, @Nullable String source, ItemDTO item) {
        super(topic, payload, source);
        this.item = item;
    }

    /**
     * Gets the item.
     *
     * @return the item
     */
    public ItemDTO getItem() {
        return item;
    }
}
