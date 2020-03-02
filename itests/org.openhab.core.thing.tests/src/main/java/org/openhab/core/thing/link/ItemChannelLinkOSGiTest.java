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
package org.openhab.core.thing.link;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Hashtable;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.osgi.service.component.ComponentContext;

/**
 * Tests for {@link ManagedItemChannelLinkProvider}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Christoph Weitkamp - Migrated tests to pure Java
 */
public class ItemChannelLinkOSGiTest extends JavaOSGiTest {

    private static final String ITEM = "item";
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding:thing");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "thing");
    private static final ChannelUID CHANNEL_UID = new ChannelUID(THING_UID, "channel");
    private static final ItemChannelLink ITEM_CHANNEL_LINK = new ItemChannelLink(ITEM, CHANNEL_UID);

    private ManagedItemChannelLinkProvider managedItemChannelLinkProvider;
    private ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ManagedThingProvider managedThingProvider;

    @Before
    public void setup() {
        registerVolatileStorageService();
        managedThingProvider = getService(ManagedThingProvider.class);
        managedThingProvider.add(ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannel(ChannelBuilder.create(CHANNEL_UID, "Color").build()).build());
        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        managedItemChannelLinkProvider = getService(ManagedItemChannelLinkProvider.class);
        assertNotNull(managedItemChannelLinkProvider);
    }

    @After
    public void teardown() {
        managedItemChannelLinkProvider.getAll().forEach(it -> managedItemChannelLinkProvider.remove(it.getUID()));
        managedThingProvider.getAll().forEach(it -> managedThingProvider.remove(it.getUID()));
        ComponentContext componentContext = mock(ComponentContext.class);
        when(componentContext.getProperties()).thenReturn(new Hashtable<>());
    }

    @Test
    public void assertThatItemChannelLinkIsPresentInItemChannelLinkRegistryWhenAddedToManagedItemChannelLinkProvider() {
        assertEquals(0, itemChannelLinkRegistry.getAll().size());
        assertEquals(0, managedItemChannelLinkProvider.getAll().size());

        managedItemChannelLinkProvider.add(ITEM_CHANNEL_LINK);

        assertEquals(1, itemChannelLinkRegistry.getAll().size());
        assertEquals(1, managedItemChannelLinkProvider.getAll().size());

        managedItemChannelLinkProvider.remove(ITEM_CHANNEL_LINK.getUID());

        assertEquals(0, itemChannelLinkRegistry.getAll().size());
        assertEquals(0, managedItemChannelLinkProvider.getAll().size());
    }

    @Test
    public void assertThatIsLinkedReturnsTrue() {
        managedItemChannelLinkProvider.add(ITEM_CHANNEL_LINK);
        assertTrue(itemChannelLinkRegistry.isLinked(ITEM, CHANNEL_UID));
    }

    @Test
    public void assertThatIsLinkedReturnsFalse() {
        assertFalse(itemChannelLinkRegistry.isLinked(ITEM, CHANNEL_UID));
    }

    @Test
    public void assertThatGetBoundChannelsReturnsChannel() {
        managedItemChannelLinkProvider.add(ITEM_CHANNEL_LINK);
        Set<ChannelUID> boundChannels = itemChannelLinkRegistry.getBoundChannels(ITEM);
        assertEquals(1, boundChannels.size());
        assertTrue(boundChannels.contains(ITEM_CHANNEL_LINK.getLinkedUID()));
    }

    @Test
    public void assertThatGetBoundChannelsReturnsEmptySet() {
        Set<ChannelUID> boundThings = itemChannelLinkRegistry.getBoundChannels("notExistingItem");
        assertTrue(boundThings.isEmpty());
    }

    @Test
    public void assertThatGetBoundThingsReturnsThing() {
        managedItemChannelLinkProvider.add(ITEM_CHANNEL_LINK);
        Set<Thing> boundThings = itemChannelLinkRegistry.getBoundThings(ITEM);
        assertEquals(1, boundThings.size());
        assertEquals(CHANNEL_UID.getThingUID(), boundThings.stream().findFirst().get().getUID());
    }

    @Test
    public void assertThatgetBoundThingsReturnsEmptySet() {
        Set<Thing> boundThings = itemChannelLinkRegistry.getBoundThings("notExistingItem");
        assertTrue(boundThings.isEmpty());
    }
}
