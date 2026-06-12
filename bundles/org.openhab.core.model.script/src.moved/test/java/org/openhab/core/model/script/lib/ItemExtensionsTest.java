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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

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
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;

/**
 * Tests for {@link ItemExtensions}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ItemExtensionsTest {

    private static final String ITEM_NAME = "TestItem";
    private static final String NAMESPACE = "test";
    private static final String VALUE = "myValue";
    private static final String CHANNEL_UID = "binding:thing:1:channel";

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @Mock @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisher;
    private @Mock @NonNullByDefault({}) ModelRepository modelRepository;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @Mock @NonNullByDefault({}) RuleRegistry ruleRegistry;
    private @Mock @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @Mock @NonNullByDefault({}) TimeZoneProvider timeZoneProvider;
    private @Mock @NonNullByDefault({}) LocaleProvider localeProvider;
    private @Mock @NonNullByDefault({}) Scheduler scheduler;
    private @Mock @NonNullByDefault({}) Item item;
    private @Mock @NonNullByDefault({}) Item item2;
    private @Mock @NonNullByDefault({}) Metadata metadata;
    private @Mock @NonNullByDefault({}) ItemChannelLink link;
    private @Mock @NonNullByDefault({}) Thing thing;

    @BeforeEach
    public void setUp() throws Exception {
        when(item.getName()).thenReturn(ITEM_NAME);
        when(metadataRegistry.get(new MetadataKey(NAMESPACE, ITEM_NAME))).thenReturn(metadata);
        when(metadataRegistry.remove(new MetadataKey(NAMESPACE, ITEM_NAME))).thenReturn(metadata);
        doReturn(metadata).when(metadataRegistry).add(any(Metadata.class));
        when(metadataRegistry.update(any(Metadata.class))).thenReturn(metadata);
        when(itemChannelLinkRegistry.getLinks(ITEM_NAME)).thenReturn(Set.of(link));
        when(itemChannelLinkRegistry.getBoundChannels(ITEM_NAME))
                .thenReturn(Set.of(new ChannelUID("binding:thing:1:channel")));
        when(itemChannelLinkRegistry.getBoundThings(ITEM_NAME)).thenReturn(Set.of(thing));
        when(itemChannelLinkRegistry.isLinked(ITEM_NAME)).thenReturn(true);
        when(itemChannelLinkRegistry.isLinked(ITEM_NAME, new ChannelUID(CHANNEL_UID))).thenReturn(true);
        when(itemChannelLinkRegistry.get(ITEM_NAME + " -> " + CHANNEL_UID)).thenReturn(link);
        when(itemChannelLinkRegistry.remove(ITEM_NAME + " -> " + CHANNEL_UID)).thenReturn(link);
        when(itemChannelLinkRegistry.add(any(ItemChannelLink.class))).thenReturn(link);
        when(itemChannelLinkRegistry.update(any(ItemChannelLink.class))).thenReturn(link);
        when(itemChannelLinkRegistry.removeLinksForItem(ITEM_NAME)).thenReturn(2);

        new ScriptServiceUtil(itemRegistry, thingRegistry, eventPublisher, modelRepository, metadataRegistry,
                ruleRegistry, itemChannelLinkRegistry, timeZoneProvider, localeProvider, scheduler);
    }

    @AfterEach
    public void tearDown() throws Exception {
        nullScriptServiceUtilInstance();
    }

    @Test
    public void testAddMetadata() {
        assertDoesNotThrow(() -> ItemExtensions.addMetadata(item, NAMESPACE, VALUE));
        assertDoesNotThrow(() -> ItemExtensions.addMetadata(item, NAMESPACE, VALUE, "arg1", "val1"));
        assertDoesNotThrow(() -> ItemExtensions.addMetadata(item, NAMESPACE, VALUE, Map.of("arg1", "val1")));
    }

    @Test
    public void testGetMetadata() {
        assertThat(ItemExtensions.getMetadata(item, NAMESPACE), is(metadata));
    }

    @Test
    public void testRemoveMetadata() {
        assertThat(ItemExtensions.removeMetadata(item, NAMESPACE), is(metadata));
    }

    @Test
    public void testUpdateMetadata() {
        assertThat(ItemExtensions.updateMetadata(item, NAMESPACE, VALUE), is(metadata));
        assertThat(ItemExtensions.updateMetadata(item, NAMESPACE, VALUE, "arg1", "val1"), is(metadata));
        assertThat(ItemExtensions.updateMetadata(item, NAMESPACE, VALUE, Map.of("arg1", "val1")), is(metadata));
    }

    @Test
    public void testChannelLinkMethods() {
        assertThat(ItemExtensions.getChannelLinks(item), is(Set.of(link)));
        assertThat(ItemExtensions.getBoundChannels(item), is(Set.of(new ChannelUID(CHANNEL_UID))));
        assertThat(ItemExtensions.getBoundThings(item), is(Set.of(thing)));
        assertThat(ItemExtensions.isLinked(item), is(true));
        assertThat(ItemExtensions.isLinked(item, CHANNEL_UID), is(true));
        assertThat(ItemExtensions.isLinked(item, new ChannelUID(CHANNEL_UID)), is(true));
        assertThat(ItemExtensions.getChannelLink(item, new ChannelUID(CHANNEL_UID)), is(link));
        assertThat(ItemExtensions.getChannelLink(item, CHANNEL_UID), is(link));
    }

    @Test
    public void testLinkLifecycle() {
        assertThat(ItemExtensions.removeChannelLink(item, new ChannelUID(CHANNEL_UID)), is(link));
        assertThat(ItemExtensions.removeChannelLink(item, CHANNEL_UID), is(link));
        assertThat(ItemExtensions.removeChannelLinks(item), is(2));
        assertThat(ItemExtensions.addChannelLink(item, CHANNEL_UID), is(link));
        assertThat(ItemExtensions.addChannelLink(item, CHANNEL_UID, "arg1", "val1"), is(link));
        assertThat(ItemExtensions.addChannelLink(item, CHANNEL_UID, Map.of("arg1", "val1")), is(link));
        assertThat(ItemExtensions.replaceChannelLink(item, CHANNEL_UID), is(link));
        assertThat(ItemExtensions.replaceChannelLink(item, CHANNEL_UID, "arg1", "val1"), is(link));
        assertThat(ItemExtensions.replaceChannelLink(item, CHANNEL_UID, Map.of("arg1", "val1")), is(link));
    }

    private void nullScriptServiceUtilInstance() throws Exception {
        Field field = ScriptServiceUtil.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }
}
