/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Test;
import org.openhab.core.events.Event;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.dto.ItemChannelLinkDTO;

import com.google.gson.Gson;

/**
 * {@link LinkEventFactoryTests} tests the {@link LinkEventFactory}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class LinkEventFactoryTest {
    private static final Gson JSONCONVERTER = new Gson();

    private final LinkEventFactory factory = new LinkEventFactory();

    private static final ItemChannelLink LINK = new ItemChannelLink("item", new ChannelUID("a:b:c:d"));
    private static final ItemChannelLinkDTO LINK_DTO = new ItemChannelLinkDTO(LINK.getItemName(),
            LINK.getLinkedUID().toString(), LINK.getConfiguration().getProperties());

    private static final String LINK_EVENT_PAYLOAD = JSONCONVERTER.toJson(LINK_DTO);
    private static final String LINK_ADDED_EVENT_TOPIC = LinkEventFactory.LINK_ADDED_EVENT_TOPIC.replace("{linkID}",
            LINK.getItemName() + "-" + LINK.getLinkedUID().toString());
    private static final String LINK_REMOVED_EVENT_TOPIC = LinkEventFactory.LINK_REMOVED_EVENT_TOPIC.replace("{linkID}",
            LINK.getItemName() + "-" + LINK.getLinkedUID().toString());

    @Test
    public void testCreateItemChannelLinkAddedEvent() {
        ItemChannelLinkAddedEvent event = LinkEventFactory.createItemChannelLinkAddedEvent(LINK);

        assertEquals(ItemChannelLinkAddedEvent.TYPE, event.getType());
        assertEquals(LINK_ADDED_EVENT_TOPIC, event.getTopic());
        assertEquals(LINK_EVENT_PAYLOAD, event.getPayload());
    }

    @Test
    public void testCreateEventItemChannelLinkAddedEvent() throws Exception {
        Event event = factory.createEvent(ItemChannelLinkAddedEvent.TYPE, LINK_ADDED_EVENT_TOPIC, LINK_EVENT_PAYLOAD,
                null);

        assertThat(event, is(instanceOf(ItemChannelLinkAddedEvent.class)));
        ItemChannelLinkAddedEvent triggeredEvent = (ItemChannelLinkAddedEvent) event;
        assertEquals(ItemChannelLinkAddedEvent.TYPE, triggeredEvent.getType());
        assertEquals(LINK_ADDED_EVENT_TOPIC, triggeredEvent.getTopic());
        assertEquals(LINK_EVENT_PAYLOAD, triggeredEvent.getPayload());
    }

    @Test
    public void testCreateItemChannelLinkRemovedEvent() {
        ItemChannelLinkRemovedEvent event = LinkEventFactory.createItemChannelLinkRemovedEvent(LINK);

        assertEquals(ItemChannelLinkRemovedEvent.TYPE, event.getType());
        assertEquals(LINK_REMOVED_EVENT_TOPIC, event.getTopic());
        assertEquals(LINK_EVENT_PAYLOAD, event.getPayload());
    }

    @Test
    public void testCreateEventItemChannelLinkRemovedEvent() throws Exception {
        Event event = factory.createEvent(ItemChannelLinkRemovedEvent.TYPE, LINK_REMOVED_EVENT_TOPIC,
                LINK_EVENT_PAYLOAD, null);

        assertThat(event, is(instanceOf(ItemChannelLinkRemovedEvent.class)));
        ItemChannelLinkRemovedEvent triggeredEvent = (ItemChannelLinkRemovedEvent) event;
        assertEquals(ItemChannelLinkRemovedEvent.TYPE, triggeredEvent.getType());
        assertEquals(LINK_REMOVED_EVENT_TOPIC, triggeredEvent.getTopic());
        assertEquals(LINK_EVENT_PAYLOAD, triggeredEvent.getPayload());
    }
}
