/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;

/**
 * Tests for {@link ItemChannelLinkRegistry} assignment of the linked
 * channel's default tags to the item.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class ItemChannelLinkAssignDefaultTagsOSGiTest extends JavaOSGiTest {

    private static final String TEST_ITEM_NAME = "testItem";
    private static final String TEST_MODEL_NAME = "testModel.items";
    private static final String ITEMS_MODEL_TYPE = "items";

    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) ModelRepository modelRepository;
    private @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;

    @BeforeEach
    public void setUp() {
        registerVolatileStorageService();

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);
        assertEquals(0, itemRegistry.getAll().size());

        modelRepository = getService(ModelRepository.class);
        assertNotNull(modelRepository);
        assertFalse(modelRepository.getAllModelNamesOfType(ITEMS_MODEL_TYPE).iterator().hasNext());

        thingRegistry = getService(ThingRegistry.class);
        assertNotNull(thingRegistry);

        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        assertNotNull(itemChannelLinkRegistry);
    }

    @Test
    public void assertItemChannelLinkRegistryIsUpdated() {
        String input = "String %s { channel=\"test:test:test:test\" [ boolVal=true, foo=\"bar\" ] }"
                .formatted(TEST_ITEM_NAME);

        modelRepository.addOrRefreshModel(TEST_MODEL_NAME, new ByteArrayInputStream(input.getBytes()));

        Item item = itemRegistry.get(TEST_ITEM_NAME);
        assertNotNull(item);
        assertEquals(1, itemChannelLinkRegistry.getLinks(TEST_ITEM_NAME).size());
        Optional<ItemChannelLink> optional = itemChannelLinkRegistry.getLinks(TEST_ITEM_NAME).stream().findFirst();
        assertTrue(optional.isPresent());
        ItemChannelLink link = optional.get();
        assertEquals(new ChannelUID("test:test:test:test"), link.getLinkedUID());
        Configuration conf = link.getConfiguration();
        assertNotNull(conf);
        assertEquals(true, conf.get("boolVal"));
        assertEquals("bar", conf.get("foo"));
    }

    @Test
    public void assertItemAssignedDefaultTags() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("test", "test");
        ThingUID thingUID = new ThingUID(thingTypeUID, "test");
        Channel channel = ChannelBuilder.create(new ChannelUID(thingUID, "test")).withDefaultTags(Set.of("foo", "bar"))
                .build();
        ThingBuilder thingBuilder = ThingBuilder.create(thingTypeUID, thingUID).withChannel(channel);
        thingRegistry.add(thingBuilder.build());

        String input = "String %s { channel=\"test:test:test:test\" [ %s=true ] }".formatted(TEST_ITEM_NAME,
                ItemChannelLinkRegistry.USE_TAGS);

        modelRepository.addOrRefreshModel(TEST_MODEL_NAME, new ByteArrayInputStream(input.getBytes()));

        Item item = itemRegistry.get(TEST_ITEM_NAME);
        assertNotNull(item);
        assertEquals(2, item.getTags().size());
        assertTrue(item.getTags().contains("foo"));
        assertTrue(item.getTags().contains("bar"));
    }

    @Test
    public void assertItemDidNotAssignDefaultTags() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("test", "test");
        ThingUID thingUID = new ThingUID(thingTypeUID, "test");
        Channel channel = ChannelBuilder.create(new ChannelUID(thingUID, "test")).withDefaultTags(Set.of("foo", "bar"))
                .build();
        ThingBuilder thingBuilder = ThingBuilder.create(thingTypeUID, thingUID).withChannel(channel);
        thingRegistry.add(thingBuilder.build());

        String input = "String %s { channel=\"test:test:test:test\" [ %s=false ] }".formatted(TEST_ITEM_NAME,
                ItemChannelLinkRegistry.USE_TAGS);

        modelRepository.addOrRefreshModel(TEST_MODEL_NAME, new ByteArrayInputStream(input.getBytes()));

        Item item = itemRegistry.get(TEST_ITEM_NAME);
        assertNotNull(item);
        assertEquals(0, item.getTags().size());
    }

    @Test
    public void assertItemDidNotAssignDefaultTags2() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("test", "test");
        ThingUID thingUID = new ThingUID(thingTypeUID, "test");
        Channel channel = ChannelBuilder.create(new ChannelUID(thingUID, "test"))
                .withDefaultTags(Set.of("Switch", "Power")).build();
        ThingBuilder thingBuilder = ThingBuilder.create(thingTypeUID, thingUID).withChannel(channel);
        thingRegistry.add(thingBuilder.build());

        String input = "String %s [Measurement, Temperature, CustomTag] { channel=\"test:test:test:test\" [ %s=true ] }"
                .formatted(TEST_ITEM_NAME, ItemChannelLinkRegistry.USE_TAGS);

        modelRepository.addOrRefreshModel(TEST_MODEL_NAME, new ByteArrayInputStream(input.getBytes()));

        Item item = itemRegistry.get(TEST_ITEM_NAME);
        assertNotNull(item);
        assertEquals(3, item.getTags().size());
        assertTrue(item.getTags().contains("Measurement"));
        assertTrue(item.getTags().contains("Temperature"));
        assertTrue(item.getTags().contains("CustomTag"));
    }

    @Test
    public void assertItemDidNotAssignSecondChannelDefaultTags() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("test", "test");
        ThingUID thingUID = new ThingUID(thingTypeUID, "test");
        Channel channel1 = ChannelBuilder.create(new ChannelUID(thingUID, "test1"))
                .withDefaultTags(Set.of("Switch", "Power")).build();
        Channel channel2 = ChannelBuilder.create(new ChannelUID(thingUID, "test2"))
                .withDefaultTags(Set.of("Measurement", "Temperature")).build();
        ThingBuilder thingBuilder = ThingBuilder.create(thingTypeUID, thingUID).withChannel(channel1)
                .withChannel(channel2);
        thingRegistry.add(thingBuilder.build());

        String input = "String %s { channel=\"test:test:test:test1\" [ %s=true ], channel=\"test:test:test:test2\" [ %s=true ] }"
                .formatted(TEST_ITEM_NAME, ItemChannelLinkRegistry.USE_TAGS, ItemChannelLinkRegistry.USE_TAGS);

        modelRepository.addOrRefreshModel(TEST_MODEL_NAME, new ByteArrayInputStream(input.getBytes()));

        Item item = itemRegistry.get(TEST_ITEM_NAME);
        assertNotNull(item);
        assertEquals(2, item.getTags().size());
        assertTrue(item.getTags().contains("Switch"));
        assertTrue(item.getTags().contains("Power"));
    }

    @Test
    public void assertItemAssignedOwnTags() {
        ThingTypeUID thingTypeUID = new ThingTypeUID("test", "test");
        ThingUID thingUID = new ThingUID(thingTypeUID, "test");
        Channel channel = ChannelBuilder.create(new ChannelUID(thingUID, "test")).withDefaultTags(Set.of("foo", "bar"))
                .build();
        ThingBuilder thingBuilder = ThingBuilder.create(thingTypeUID, thingUID).withChannel(channel);
        thingRegistry.add(thingBuilder.build());

        String input = "String %s [tag1, tag2] { channel=\"test:test:test:test\" [ %s=true ] }"
                .formatted(TEST_ITEM_NAME, ItemChannelLinkRegistry.USE_TAGS);

        modelRepository.addOrRefreshModel(TEST_MODEL_NAME, new ByteArrayInputStream(input.getBytes()));

        Item item = itemRegistry.get(TEST_ITEM_NAME);
        assertNotNull(item);
        assertEquals(2, item.getTags().size());
        assertTrue(item.getTags().contains("tag1"));
        assertTrue(item.getTags().contains("tag2"));
        assertTrue(item.getTags().contains("this should fail"));
    }
}
