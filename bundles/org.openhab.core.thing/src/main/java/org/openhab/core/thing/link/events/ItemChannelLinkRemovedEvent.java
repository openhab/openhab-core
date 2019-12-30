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
package org.openhab.core.thing.link.events;

import org.openhab.core.thing.link.dto.ItemChannelLinkDTO;

/**
 * A {@link ItemChannelLinkRemovedEvent} notifies subscribers that an item channel link has been removed.
 * Events must be created with the {@link LinkEventFactory}.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class ItemChannelLinkRemovedEvent extends AbstractItemChannelLinkRegistryEvent {

    /**
     * The link removed event type.
     */
    public static final String TYPE = ItemChannelLinkRemovedEvent.class.getSimpleName();

    public ItemChannelLinkRemovedEvent(String topic, String payload, ItemChannelLinkDTO link) {
        super(topic, payload, link);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        ItemChannelLinkDTO link = getLink();
        return "Link '" + link.itemName + " => " + link.channelUID + "' has been removed.";
    }

}
