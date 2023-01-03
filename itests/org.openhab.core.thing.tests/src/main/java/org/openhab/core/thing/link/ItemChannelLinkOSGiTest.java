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
package org.openhab.core.thing.link;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.ColorItem;
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
@NonNullByDefault
public class ItemChannelLinkOSGiTest extends JavaOSGiTest {

    private static final String BULK_BASE_THING_UID = "binding:type:thing";
    private static final String BULK_BASE_ITEM_NAME = "item";
    private static final int BULK_ITEM_COUNT = 3;
    private static final int BULK_THING_COUNT = 3;
    private static final int BULK_CHANNEL_COUNT = 3;

    private static final String ITEM = "item";
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("binding:thing");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "thing");
    private static final ChannelUID CHANNEL_UID = new ChannelUID(THING_UID, "channel");
    private static final ItemChannelLink ITEM_CHANNEL_LINK = new ItemChannelLink(ITEM, CHANNEL_UID);

    private @NonNullByDefault({}) ManagedItemChannelLinkProvider managedItemChannelLinkProvider;
    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    private @NonNullByDefault({}) ManagedItemProvider managedItemProvider;

    @BeforeEach
    public void setup() {
        registerVolatileStorageService();
        managedThingProvider = getService(ManagedThingProvider.class);
        managedThingProvider.add(ThingBuilder.create(THING_TYPE_UID, THING_UID)
                .withChannel(ChannelBuilder.create(CHANNEL_UID, CoreItemFactory.COLOR).build()).build());
        managedItemProvider = getService(ManagedItemProvider.class);

        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        managedItemChannelLinkProvider = getService(ManagedItemChannelLinkProvider.class);
        assertNotNull(managedItemChannelLinkProvider);
    }

    @AfterEach
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

        waitForAssert(() -> assertEquals(1, itemChannelLinkRegistry.getAll().size()));
        assertEquals(1, managedItemChannelLinkProvider.getAll().size());

        managedItemChannelLinkProvider.remove(ITEM_CHANNEL_LINK.getUID());

        assertEquals(0, itemChannelLinkRegistry.getAll().size());
        assertEquals(0, managedItemChannelLinkProvider.getAll().size());
    }

    @Test
    public void assertThatIsLinkedReturnsTrue() {
        managedItemChannelLinkProvider.add(ITEM_CHANNEL_LINK);
        waitForAssert(() -> assertTrue(itemChannelLinkRegistry.isLinked(ITEM, CHANNEL_UID)));
    }

    @Test
    public void assertThatIsLinkedReturnsFalse() {
        assertFalse(itemChannelLinkRegistry.isLinked(ITEM, CHANNEL_UID));
    }

    @Test
    public void assertThatGetBoundChannelsReturnsChannel() {
        managedItemChannelLinkProvider.add(ITEM_CHANNEL_LINK);
        waitForAssert(() -> {
            Set<ChannelUID> boundChannels = itemChannelLinkRegistry.getBoundChannels(ITEM);
            assertEquals(1, boundChannels.size());
            assertTrue(boundChannels.contains(ITEM_CHANNEL_LINK.getLinkedUID()));
        });
    }

    @Test
    public void assertThatGetBoundChannelsReturnsEmptySet() {
        Set<ChannelUID> boundThings = itemChannelLinkRegistry.getBoundChannels("notExistingItem");
        assertTrue(boundThings.isEmpty());
    }

    @Test
    public void assertThatGetBoundThingsReturnsThing() {
        managedItemChannelLinkProvider.add(ITEM_CHANNEL_LINK);
        waitForAssert(() -> {
            Set<Thing> boundThings = itemChannelLinkRegistry.getBoundThings(ITEM);
            assertEquals(1, boundThings.size());
            assertEquals(CHANNEL_UID.getThingUID(), boundThings.stream().findFirst().get().getUID());
        });
    }

    @Test
    public void assertThatGetBoundThingsReturnsEmptySet() {
        Set<Thing> boundThings = itemChannelLinkRegistry.getBoundThings("notExistingItem");
        assertTrue(boundThings.isEmpty());
    }

    @Test
    public void assertThatAllLinksForItemCanBeDeleted() {
        fillRegistryForBulkTests();

        String itemToRemove = BULK_BASE_ITEM_NAME + "_0_1_1";
        int removed = itemChannelLinkRegistry.removeLinksForItem(itemToRemove);
        assertThat(removed, is(1));

        assertThat(itemChannelLinkRegistry.stream().map(ItemChannelLink::getItemName).collect(Collectors.toList()),
                not(hasItem(itemToRemove)));
        assertThat(itemChannelLinkRegistry.getAll(),
                hasSize(BULK_ITEM_COUNT * BULK_THING_COUNT * BULK_CHANNEL_COUNT - 1));
    }

    @Test
    public void assertThatAllLinksForThingCanBeDeleted() {
        fillRegistryForBulkTests();

        ThingUID thingToRemove = new ThingUID(BULK_BASE_THING_UID + "_0_0");
        int removed = itemChannelLinkRegistry.removeLinksForThing(thingToRemove);
        assertThat(removed, is(BULK_CHANNEL_COUNT));

        assertThat(itemChannelLinkRegistry.stream().map(ItemChannelLink::getLinkedUID).map(ChannelUID::getThingUID)
                .collect(Collectors.toList()), not(hasItem(thingToRemove)));
        assertThat(itemChannelLinkRegistry.getAll(),
                hasSize((BULK_ITEM_COUNT * BULK_THING_COUNT - 1) * BULK_CHANNEL_COUNT));
    }

    @Test
    public void assertThatCompressOnlyRemovesInvalidLinks() {
        fillRegistryForBulkTests();

        int expected = BULK_ITEM_COUNT * BULK_THING_COUNT * BULK_CHANNEL_COUNT;

        int removed = itemChannelLinkRegistry.purge();
        assertThat(removed, is(0));
        assertThat(itemChannelLinkRegistry.getAll(), hasSize(expected));

        managedItemProvider.remove(BULK_BASE_ITEM_NAME + "_0_0_0");
        removed = itemChannelLinkRegistry.purge();
        expected -= removed;
        assertThat(removed, is(1));
        assertThat(itemChannelLinkRegistry.getAll(), hasSize(expected));

        managedThingProvider.remove(new ThingUID(BULK_BASE_THING_UID + "_1_0"));
        removed = itemChannelLinkRegistry.purge();
        expected -= removed;
        assertThat(removed, is(BULK_CHANNEL_COUNT));
        assertThat(itemChannelLinkRegistry.getAll(), hasSize(expected));

        managedItemProvider.remove(BULK_BASE_ITEM_NAME + "_2_0_0");
        managedThingProvider.remove(new ThingUID(BULK_BASE_THING_UID + "_2_0"));
        removed = itemChannelLinkRegistry.purge();
        expected -= removed;
        assertThat(removed, is(BULK_CHANNEL_COUNT));
        assertThat(itemChannelLinkRegistry.getAll(), hasSize(expected));
    }

    private void fillRegistryForBulkTests() {
        // clear all old links and things
        managedItemChannelLinkProvider.getAll().forEach(it -> managedItemChannelLinkProvider.remove(it.getUID()));
        managedThingProvider.getAll().forEach(it -> managedThingProvider.remove(it.getUID()));

        for (int i = 0; i < BULK_ITEM_COUNT; i++) {
            for (int j = 0; j < BULK_THING_COUNT; j++) {
                ThingUID thingUID = new ThingUID(BULK_BASE_THING_UID + "_" + i + "_" + j);
                ThingBuilder thingBuilder = ThingBuilder.create(THING_TYPE_UID, thingUID);
                List<ItemChannelLink> links = new ArrayList<>();
                for (int k = 0; k < BULK_CHANNEL_COUNT; k++) {
                    String itemName = BULK_BASE_ITEM_NAME + "_" + i + "_" + j + "_" + k;
                    managedItemProvider.add(new ColorItem(itemName));

                    ChannelUID channelUID = new ChannelUID(thingUID, "channel" + k);
                    thingBuilder.withChannel(ChannelBuilder.create(channelUID, CoreItemFactory.COLOR).build());
                    links.add(new ItemChannelLink(itemName, channelUID));
                }
                managedThingProvider.add(thingBuilder.build());
                links.forEach(managedItemChannelLinkProvider::add);
            }
        }

        waitForAssert(() -> assertThat(itemChannelLinkRegistry.getAll(),
                hasSize(BULK_ITEM_COUNT * BULK_THING_COUNT * BULK_CHANNEL_COUNT)));
        assertThat(managedThingProvider.getAll(), hasSize(BULK_ITEM_COUNT * BULK_THING_COUNT));
    }
}
