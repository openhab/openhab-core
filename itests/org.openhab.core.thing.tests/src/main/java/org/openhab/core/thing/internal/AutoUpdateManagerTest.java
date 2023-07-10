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
package org.openhab.core.thing.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * Tests for {@link AutoUpdateManager}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class AutoUpdateManagerTest extends JavaTest {

    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID("binding:channelType");
    private static final ChannelUID CHANNEL_UID = new ChannelUID("binding:thingtype1:thing1:channel1");
    private static final String ITEM_NAME = "TestItem";

    private @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @NonNullByDefault({}) EventPublisher eventPublisher;
    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) AutoUpdateManager autoUpdateManager;
    private @NonNullByDefault({}) Item item;
    private @NonNullByDefault({}) Thing thing;

    @BeforeEach
    public void setup() {
        channelTypeRegistry = mock(ChannelTypeRegistry.class);
        eventPublisher = mock(EventPublisher.class);
        itemChannelLinkRegistry = mock(ItemChannelLinkRegistry.class);
        assertNotNull(itemChannelLinkRegistry);

        thingRegistry = mock(ThingRegistry.class);
        thing = mock(Thing.class);
        metadataRegistry = mock(MetadataRegistry.class);

        Channel channel = ChannelBuilder.create(CHANNEL_UID).withType(CHANNEL_TYPE_UID).build();

        autoUpdateManager = new AutoUpdateManager(Collections.emptyMap(), channelTypeRegistry, eventPublisher,
                itemChannelLinkRegistry, metadataRegistry, thingRegistry);

        item = mock(Item.class);
        when(item.getName()).thenReturn(ITEM_NAME);
        when(item.getAcceptedDataTypes()).thenReturn(List.of(OnOffType.class));
        when(itemChannelLinkRegistry.getLinks(any(String.class)))
                .thenReturn(Set.of(new ItemChannelLink(ITEM_NAME, CHANNEL_UID)));
        when(thingRegistry.get(any(ThingUID.class))).thenReturn(thing);
        when(thing.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(thing.getHandler()).thenReturn(mock(ThingHandler.class));
        when(thing.getChannel(any(String.class))).thenReturn(channel);
    }

    @Test
    public void testAutoUpdateVetoFromChannelType() {
        when(channelTypeRegistry.getChannelType(any(ChannelTypeUID.class)))
                .thenReturn(ChannelTypeBuilder.state(CHANNEL_TYPE_UID, "label", CoreItemFactory.SWITCH).withAutoUpdatePolicy(AutoUpdatePolicy.VETO).build());

        autoUpdateManager.receiveCommand(ItemEventFactory.createCommandEvent(ITEM_NAME, OnOffType.ON), item);

        // No event should have been sent
        verify(eventPublisher, never()).post(any(Event.class));
    }
}
