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
 * A {@link ItemChannelLinkAddedEvent} notifies subscribers that an item channel link has been added.
 * Events must be created with the {@link LinkEventFactory}.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class ItemChannelLinkAddedEvent extends AbstractItemChannelLinkRegistryEvent {

    /**
     * The link added event type.
     */
    public static final String TYPE = ItemChannelLinkAddedEvent.class.getSimpleName();

    public ItemChannelLinkAddedEvent(String topic, String payload, ItemChannelLinkDTO link) {
        super(topic, payload, link);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        ItemChannelLinkDTO link = getLink();
        return "Link '" + link.itemName + "-" + link.channelUID + "' has been added.";
    }
}
