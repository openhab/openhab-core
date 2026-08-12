/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.internal.link;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.link.ManagedItemChannelLinkProvider;
import org.openhab.core.thing.link.dto.ItemChannelLinkDTO;
import org.openhab.core.thing.profiles.ProfileTypeRegistry;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeRegistry;

/**
 * The {@link ItemChannelLinkResourceTest} tests the {@link ItemChannelLinkResource}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ItemChannelLinkResourceTest {

    private static final int EXPECTED_REMOVED_LINKS = 5;

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) ThingRegistry thingRegistryMock;
    private @Mock @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistryMock;
    private @Mock @NonNullByDefault({}) ProfileTypeRegistry profileTypeRegistryMock;
    private @Mock @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistryMock;
    private @Mock @NonNullByDefault({}) ManagedItemChannelLinkProvider managedItemChannelLinkProviderMock;
    private @NonNullByDefault({}) ItemChannelLinkResource itemChannelLinkResource;

    private static class ConfigUtilAccessor extends ConfigUtil {
        static void setEnv(Map<String, String> values) {
            setEnvProvider(values::get);
        }

        static void resetEnv() {
            setEnvProvider(System::getenv);
        }
    }

    @BeforeEach
    public void setup() {
        itemChannelLinkResource = new ItemChannelLinkResource(itemRegistryMock, thingRegistryMock,
                channelTypeRegistryMock, profileTypeRegistryMock, itemChannelLinkRegistryMock,
                managedItemChannelLinkProviderMock);
        ConfigUtilAccessor.setEnv(Map.of("HOSTNAME", "openhab-host"));
        when(itemChannelLinkRegistryMock.removeLinksForItem(any())).thenReturn(EXPECTED_REMOVED_LINKS);
        when(itemChannelLinkRegistryMock.removeLinksForThing(any())).thenReturn(EXPECTED_REMOVED_LINKS);
    }

    @AfterEach
    public void tearDown() {
        ConfigUtilAccessor.resetEnv();
    }

    @Test
    public void testRemoveAllLinksForItem() {
        try (Response response = itemChannelLinkResource.removeAllLinksForObject("testItem")) {
            assertThat(response.getStatus(), is(200));
            Object responseEntity = response.getEntity();
            assertThat(responseEntity, instanceOf(Map.class));
            assertThat(((Map<?, ?>) responseEntity).get("count"), is(EXPECTED_REMOVED_LINKS));
        }

        verify(itemChannelLinkRegistryMock).removeLinksForItem(eq("testItem"));
    }

    @Test
    public void testRemoveAllLinksForThing() {
        try (Response response = itemChannelLinkResource.removeAllLinksForObject("binding:type:thing")) {
            assertThat(response.getStatus(), is(200));
            Object responseEntity = response.getEntity();
            assertThat(responseEntity, instanceOf(Map.class));
            assertThat(((Map<?, ?>) responseEntity).get("count"), is(EXPECTED_REMOVED_LINKS));
        }

        verify(itemChannelLinkRegistryMock).removeLinksForThing(eq(new ThingUID("binding:type:thing")));
    }

    @Test
    public void testLinkUsesRawConfigurationValues() throws ItemNotFoundException {
        String itemName = "testItem";
        ChannelUID channelUID = new ChannelUID("binding:type:thing:channel");
        ItemChannelLinkDTO dto = new ItemChannelLinkDTO(itemName, channelUID.getAsString(),
                Map.of("host", "${ENV:HOSTNAME}"));

        when(itemRegistryMock.getItem(itemName)).thenReturn(mock(Item.class));

        Channel channel = mock(Channel.class);
        when(channel.getKind()).thenReturn(ChannelKind.STATE);
        Thing thing = mock(Thing.class);
        when(thing.getChannel(channelUID)).thenReturn(channel);
        when(thingRegistryMock.get(channelUID.getThingUID())).thenReturn(thing);

        try (Response response = itemChannelLinkResource.link(itemName, channelUID.getAsString(), dto)) {
            assertThat(response.getStatus(), is(200));
        }

        ArgumentCaptor<ItemChannelLink> captor = ArgumentCaptor.forClass(ItemChannelLink.class);
        verify(itemChannelLinkRegistryMock).add(captor.capture());
        assertThat(captor.getValue().getConfiguration().get("host"), is("openhab-host"));
        assertThat(captor.getValue().getConfiguration().getRawProperties().get("host"), is("${ENV:HOSTNAME}"));
    }
}
