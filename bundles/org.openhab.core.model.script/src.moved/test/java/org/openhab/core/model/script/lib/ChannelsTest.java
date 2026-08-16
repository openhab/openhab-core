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
package org.openhab.core.model.script.lib;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;

/**
 * Tests for {@link Channels}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ChannelsTest {

    private static final String ITEM_NAME = "TestItem";
    private static final String CHANNEL_UID_STR = "binding:thing:1:channel";
    private static final ChannelUID CHANNEL_UID = new ChannelUID(CHANNEL_UID_STR);

    private @Mock @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisher;
    private @Mock @NonNullByDefault({}) ModelRepository modelRepository;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @Mock @NonNullByDefault({}) RuleRegistry ruleRegistry;
    private @Mock @NonNullByDefault({}) TimeZoneProvider timeZoneProvider;
    private @Mock @NonNullByDefault({}) LocaleProvider localeProvider;
    private @Mock @NonNullByDefault({}) Scheduler scheduler;
    private @Mock @NonNullByDefault({}) Item item;
    private @Mock @NonNullByDefault({}) ItemChannelLink link;
    private @Mock @NonNullByDefault({}) Channel channel;
    private @Mock @NonNullByDefault({}) Thing thing;

    @BeforeEach
    public void setUp() throws Exception {
        when(item.getName()).thenReturn(ITEM_NAME);
        when(link.getItemName()).thenReturn(ITEM_NAME);

        new ScriptServiceUtil(itemRegistry, null, eventPublisher, modelRepository, metadataRegistry, ruleRegistry,
                itemChannelLinkRegistry, timeZoneProvider, localeProvider, scheduler);
    }

    @AfterEach
    public void tearDown() throws Exception {
        nullScriptServiceUtilInstance();
    }

    @Test
    public void testGetLinks() {
        when(itemChannelLinkRegistry.getLinks(ITEM_NAME)).thenReturn(Set.of(link));
        assertThat(Channels.getLinks(ITEM_NAME), is(Set.of(link)));
        assertThat(Channels.getLinks(item), is(Set.of(link)));

        assertThat(Channels.getLinks((String) null), is(Set.of()));
        assertThat(Channels.getLinks(""), is(Set.of()));
        assertThat(Channels.getLinks((Item) null), is(Set.of()));
    }

    @Test
    public void testGetChannelLinksAndLinkedItems() {
        when(itemChannelLinkRegistry.getLinks(CHANNEL_UID)).thenReturn(Set.of(link));
        when(itemChannelLinkRegistry.getLinkedItems(CHANNEL_UID)).thenReturn(Set.of(item));
        when(itemChannelLinkRegistry.getLinkedItemNames(CHANNEL_UID)).thenReturn(Set.of(ITEM_NAME));

        assertThat(Channels.getChannelLinks(CHANNEL_UID_STR), is(Set.of(link)));
        assertThat(Channels.getChannelLinks(CHANNEL_UID), is(Set.of(link)));
        assertThat(Channels.getChannelLinks(""), is(Set.of()));
        assertThat(Channels.getChannelLinks((String) null), is(Set.of()));
        assertThat(Channels.getChannelLinks((ChannelUID) null), is(Set.of()));

        assertThat(Channels.getLinkedItems(CHANNEL_UID_STR), is(Set.of(item)));
        assertThat(Channels.getLinkedItems(""), is(Set.of()));
        assertThat(Channels.getLinkedItems((String) null), is(Set.of()));
        assertThat(Channels.getLinkedItems(CHANNEL_UID), is(Set.of(item)));
        assertThrows(IllegalArgumentException.class, () -> Channels.getLinkedItems((ChannelUID) null));

        assertThat(Channels.getLinkedItemNames(CHANNEL_UID_STR), is(Set.of(ITEM_NAME)));
        assertThat(Channels.getLinkedItemNames(""), is(Set.of()));
        assertThat(Channels.getLinkedItemNames((String) null), is(Set.of()));
        assertThat(Channels.getLinkedItemNames(CHANNEL_UID), is(Set.of(ITEM_NAME)));
        assertThrows(IllegalArgumentException.class, () -> Channels.getLinkedItemNames((ChannelUID) null));
    }

    @Test
    public void testGetBoundChannelsAndThings() {
        when(itemChannelLinkRegistry.getBoundChannels(ITEM_NAME)).thenReturn(Set.of(CHANNEL_UID));
        when(itemChannelLinkRegistry.getBoundThings(ITEM_NAME)).thenReturn(Set.of(thing));

        assertThat(Channels.getBoundChannels(ITEM_NAME), is(Set.of(CHANNEL_UID)));
        assertThat(Channels.getBoundChannels(item), is(Set.of(CHANNEL_UID)));
        assertThat(Channels.getBoundThings(ITEM_NAME), is(Set.of(thing)));
        assertThat(Channels.getBoundThings(item), is(Set.of(thing)));

        assertThat(Channels.getBoundChannels((String) null), is(Set.of()));
        assertThat(Channels.getBoundThings((String) null), is(Set.of()));
        assertThrows(IllegalArgumentException.class, () -> Channels.getBoundChannels((Item) null));
        assertThrows(IllegalArgumentException.class, () -> Channels.getBoundThings((Item) null));
    }

    @Test
    public void testIsLinkedVariants() {
        when(itemChannelLinkRegistry.isLinked(ITEM_NAME)).thenReturn(true);
        when(itemChannelLinkRegistry.isLinked(ITEM_NAME, CHANNEL_UID)).thenReturn(true);
        when(itemChannelLinkRegistry.isLinked(CHANNEL_UID)).thenReturn(true);

        assertTrue(Channels.isLinked(ITEM_NAME));
        assertFalse(Channels.isLinked(""));
        assertFalse(Channels.isLinked((String) null));
        assertTrue(Channels.isLinked(item));
        assertFalse(Channels.isLinked((Item) null));
        assertTrue(Channels.isLinked(ITEM_NAME, CHANNEL_UID_STR));
        assertFalse(Channels.isLinked("", CHANNEL_UID_STR));
        assertFalse(Channels.isLinked((String) null, CHANNEL_UID_STR));
        assertFalse(Channels.isLinked(ITEM_NAME, ""));
        assertFalse(Channels.isLinked(ITEM_NAME, (String) null));
        assertTrue(Channels.isLinked(item, CHANNEL_UID_STR));
        assertTrue(Channels.isLinked(item, CHANNEL_UID));
        assertFalse(Channels.isLinked((Item) null, CHANNEL_UID_STR));
        assertFalse(Channels.isLinked((Item) null, CHANNEL_UID));
        assertFalse(Channels.isLinked(item, ""));
        assertFalse(Channels.isLinked(item, (String) null));
        assertFalse(Channels.isLinked(item, (ChannelUID) null));
        assertTrue(Channels.isChannelLinked(CHANNEL_UID_STR));
        assertTrue(Channels.isChannelLinked(CHANNEL_UID));
        assertFalse(Channels.isChannelLinked(""));
        assertFalse(Channels.isChannelLinked((String) null));
        assertFalse(Channels.isChannelLinked((ChannelUID) null));
    }

    @Test
    public void testGetLink() {
        when(itemChannelLinkRegistry.get(ITEM_NAME + " -> " + CHANNEL_UID_STR)).thenReturn(link);
        assertThat(Channels.getLink(item, CHANNEL_UID_STR), is(link));
        assertThat(Channels.getLink(item, CHANNEL_UID), is(link));
        assertThat(Channels.getLink(ITEM_NAME, CHANNEL_UID_STR), is(link));
        assertNull(Channels.getLink((String) null, CHANNEL_UID_STR));
        assertNull(Channels.getLink(ITEM_NAME, (String) null));

        assertThrows(IllegalArgumentException.class, () -> Channels.getLink((Item) null, CHANNEL_UID));
        assertThrows(IllegalArgumentException.class, () -> Channels.getLink(item, (ChannelUID) null));
        assertThrows(IllegalArgumentException.class, () -> Channels.getLink((Item) null, CHANNEL_UID_STR));
    }

    @Test
    public void testAddItemChannelLink() {
        ItemChannelLink plainLink = new ItemChannelLink(ITEM_NAME, CHANNEL_UID);
        Configuration config = new Configuration(Map.of("arg1", "val1"));
        ItemChannelLink configuredLink = new ItemChannelLink(ITEM_NAME, CHANNEL_UID, config);
        when(itemChannelLinkRegistry.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID))).thenReturn(plainLink);
        when(itemChannelLinkRegistry.add(new ItemChannelLink(ITEM_NAME, CHANNEL_UID, config)))
                .thenReturn(configuredLink);
        assertThat(Channels.addItemChannelLink(item, CHANNEL_UID_STR), is(plainLink));
        assertThat(Channels.addItemChannelLink(item, CHANNEL_UID_STR, "arg1", "val1"), is(configuredLink));
        assertThat(Channels.addItemChannelLink(item, CHANNEL_UID_STR, Map.of("arg1", "val1")), is(configuredLink));
        verify(itemChannelLinkRegistry, times(3)).add(any(ItemChannelLink.class));

        assertThrows(IllegalArgumentException.class, () -> Channels.addItemChannelLink(null, CHANNEL_UID_STR));
        assertThrows(IllegalArgumentException.class, () -> Channels.addItemChannelLink(item, null));
        assertThrows(IllegalArgumentException.class, () -> Channels.addItemChannelLink(item, "foo:bar"));

        assertThrows(IllegalArgumentException.class,
                () -> Channels.addItemChannelLink(null, CHANNEL_UID_STR, Map.of("arg1", "val1")));
        assertThrows(IllegalArgumentException.class,
                () -> Channels.addItemChannelLink(item, null, Map.of("arg1", "val1")));
        assertThrows(IllegalArgumentException.class,
                () -> Channels.addItemChannelLink(item, "foo:bar", Map.of("arg1", "val1")));
    }

    @Test
    public void testReplaceItemChannelLink() {
        ItemChannelLink plainLink = new ItemChannelLink(ITEM_NAME, CHANNEL_UID);
        Configuration config = new Configuration(Map.of("arg1", "val1"));
        ItemChannelLink configuredLink = new ItemChannelLink(ITEM_NAME, CHANNEL_UID, config);
        when(itemChannelLinkRegistry.update(new ItemChannelLink(ITEM_NAME, CHANNEL_UID))).thenReturn(configuredLink);
        when(itemChannelLinkRegistry.update(new ItemChannelLink(ITEM_NAME, CHANNEL_UID, config))).thenReturn(plainLink);
        assertThat(Channels.replaceItemChannelLink(item, CHANNEL_UID_STR), is(configuredLink));
        assertThat(Channels.replaceItemChannelLink(item, CHANNEL_UID_STR, "arg1", "val1"), is(plainLink));
        assertThat(Channels.replaceItemChannelLink(item, CHANNEL_UID_STR, Map.of("arg1", "val1")), is(plainLink));
        verify(itemChannelLinkRegistry, times(3)).update(any(ItemChannelLink.class));

        assertThrows(IllegalArgumentException.class, () -> Channels.replaceItemChannelLink(null, CHANNEL_UID_STR));
        assertThrows(IllegalArgumentException.class, () -> Channels.replaceItemChannelLink(item, null));
        assertThrows(IllegalArgumentException.class, () -> Channels.replaceItemChannelLink(item, "foo:bar"));

        assertThrows(IllegalArgumentException.class,
                () -> Channels.replaceItemChannelLink(null, CHANNEL_UID_STR, Map.of("arg1", "val1")));
        assertThrows(IllegalArgumentException.class,
                () -> Channels.replaceItemChannelLink(item, null, Map.of("arg1", "val1")));
        assertThrows(IllegalArgumentException.class,
                () -> Channels.replaceItemChannelLink(item, "foo:bar", Map.of("arg1", "val1")));
    }

    @Test
    public void testRemoveItemChannelLink() {
        ItemChannelLink plainLink = new ItemChannelLink(ITEM_NAME, CHANNEL_UID);
        when(itemChannelLinkRegistry.remove(ITEM_NAME + " -> " + CHANNEL_UID_STR)).thenReturn(plainLink);
        assertThat(Channels.removeItemChannelLink(item, CHANNEL_UID), is(plainLink));
        assertThat(Channels.removeItemChannelLink(item, CHANNEL_UID_STR), is(plainLink));
        assertThat(Channels.removeItemChannelLink(ITEM_NAME, CHANNEL_UID_STR), is(plainLink));
        assertNull(Channels.removeItemChannelLink((String) null, CHANNEL_UID_STR));
        assertNull(Channels.removeItemChannelLink(ITEM_NAME, (String) null));
        assertNull(Channels.removeItemChannelLink(item, (String) null));
        verify(itemChannelLinkRegistry, times(3)).remove(any(String.class));

        assertThrows(IllegalArgumentException.class, () -> Channels.removeItemChannelLink((Item) null, CHANNEL_UID));
        assertThrows(IllegalArgumentException.class,
                () -> Channels.removeItemChannelLink((Item) null, CHANNEL_UID_STR));
        assertThrows(IllegalArgumentException.class, () -> Channels.removeItemChannelLink(item, (ChannelUID) null));
    }

    @Test
    public void testRemoveLinksForItemAndPurge() {
        when(itemChannelLinkRegistry.removeLinksForItem(ITEM_NAME)).thenReturn(3);
        when(itemChannelLinkRegistry.purge()).thenReturn(5);

        assertThat(Channels.removeLinksForItem(ITEM_NAME), is(3));
        assertThat(Channels.removeLinksForItem(" "), is(0));
        assertThat(Channels.removeLinksForItem((String) null), is(0));
        assertThat(Channels.removeLinksForItem(item), is(3));
        assertThat(Channels.removeLinksForItem((Item) null), is(0));
        assertThat(Channels.removeOrphanedItemChannelLinks(), is(5));
    }

    private void nullScriptServiceUtilInstance() throws Exception {
        Field field = ScriptServiceUtil.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }
}
