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
package org.eclipse.smarthome.core.thing.link.events;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.events.AbstractEventFactory;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFactory;
import org.eclipse.smarthome.core.thing.link.AbstractLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;
import org.osgi.service.component.annotations.Component;

/**
 * This is an {@link EventFactory} for creating link events. The following event types are supported by this
 * factory:
 *
 * {@link ItemChannelLinkAddedEvent#TYPE}, {@link ItemChannelLinkRemovedEvent#TYPE}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - Removed Thing link events
 */
@Component(immediate = true, service = EventFactory.class)
public class LinkEventFactory extends AbstractEventFactory {

    private static final String LINK_ADDED_EVENT_TOPIC = "smarthome/links/{linkID}/added";

    private static final String LINK_REMOVED_EVENT_TOPIC = "smarthome/links/{linkID}/removed";

    /**
     * Constructs a new LinkEventFactory.
     */
    public LinkEventFactory() {
        super(Stream.of(ItemChannelLinkAddedEvent.TYPE, ItemChannelLinkRemovedEvent.TYPE).collect(Collectors.toSet()));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, String source) throws Exception {
        Event event = null;
        if (eventType.equals(ItemChannelLinkAddedEvent.TYPE)) {
            event = createItemChannelLinkAddedEvent(topic, payload);
        } else if (eventType.equals(ItemChannelLinkRemovedEvent.TYPE)) {
            event = createItemChannelLinkRemovedEvent(topic, payload);
        }
        return event;
    }

    private Event createItemChannelLinkAddedEvent(String topic, String payload) throws Exception {
        ItemChannelLinkDTO link = deserializePayload(payload, ItemChannelLinkDTO.class);
        return new ItemChannelLinkAddedEvent(topic, payload, link);
    }

    private Event createItemChannelLinkRemovedEvent(String topic, String payload) throws Exception {
        ItemChannelLinkDTO link = deserializePayload(payload, ItemChannelLinkDTO.class);
        return new ItemChannelLinkRemovedEvent(topic, payload, link);
    }

    /**
     * Creates an item channel link added event.
     *
     * @param itemChannelLink item channel link
     * @return the created item channel link added event
     * @throws IllegalArgumentException if item channel link is null
     */
    public static ItemChannelLinkAddedEvent createItemChannelLinkAddedEvent(ItemChannelLink itemChannelLink) {
        checkNotNull(itemChannelLink, "itemChannelLink");
        String topic = buildTopic(LINK_ADDED_EVENT_TOPIC, itemChannelLink);
        ItemChannelLinkDTO itemChannelLinkDTO = map(itemChannelLink);
        String payload = serializePayload(itemChannelLinkDTO);
        return new ItemChannelLinkAddedEvent(topic, payload, itemChannelLinkDTO);
    }

    /**
     * Creates an item channel link removed event.
     *
     * @param itemChannelLink item channel link
     * @return the created item channel link removed event
     * @throws IllegalArgumentException if item channel link is null
     */
    public static ItemChannelLinkRemovedEvent createItemChannelLinkRemovedEvent(ItemChannelLink itemChannelLink) {
        checkNotNull(itemChannelLink, "itemChannelLink");
        String topic = buildTopic(LINK_REMOVED_EVENT_TOPIC, itemChannelLink);
        ItemChannelLinkDTO itemChannelLinkDTO = map(itemChannelLink);
        String payload = serializePayload(itemChannelLinkDTO);
        return new ItemChannelLinkRemovedEvent(topic, payload, itemChannelLinkDTO);
    }

    private static String buildTopic(String topic, AbstractLink itemChannelLink) {
        String targetEntity = itemChannelLink.getItemName() + "-" + itemChannelLink.getLinkedUID().toString();
        return topic.replace("{linkID}", targetEntity);
    }

    private static ItemChannelLinkDTO map(ItemChannelLink itemChannelLink) {
        return new ItemChannelLinkDTO(itemChannelLink.getItemName(), itemChannelLink.getLinkedUID().toString(),
                itemChannelLink.getConfiguration().getProperties());
    }

}
