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
package org.openhab.core.thing.internal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ChannelTypeRegistry;

/**
 * @author Simon Kaufmann - Initial contribution
 */
public class ChannelItemProviderTest {

    private static final ChannelUID CHANNEL_UID = new ChannelUID("test:test:test:test");
    private static final Channel CHANNEL = ChannelBuilder.create(CHANNEL_UID, CoreItemFactory.NUMBER).build();

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test:test");
    private static final Thing THING = ThingBuilder.create(THING_TYPE_UID, "test").withChannel(CHANNEL).build();

    private static final String ITEM_NAME = "test";
    private static final NumberItem ITEM = new NumberItem(ITEM_NAME);
    private static final ItemChannelLink LINK = new ItemChannelLink(ITEM_NAME, CHANNEL_UID);

    @Mock
    private ItemRegistry itemRegistry;
    @Mock
    private ThingRegistry thingRegistry;
    @Mock
    private ItemFactory itemFactory;
    @Mock
    private ProviderChangeListener<Item> listener;
    @Mock
    private LocaleProvider localeProvider;
    @Mock
    private ItemChannelLinkRegistry linkRegistry;

    private ChannelItemProvider provider;

    @Before
    public void setup() {
        initMocks(this);

        provider = createProvider();

        Map<String, Object> props = new HashMap<>();
        props.put("enabled", "true");
        props.put("initialDelay", "false");
        provider.activate(props);

        when(thingRegistry.getChannel(same(CHANNEL_UID))).thenReturn(CHANNEL);
        when(itemFactory.createItem(CoreItemFactory.NUMBER, ITEM_NAME)).thenReturn(ITEM);
        when(localeProvider.getLocale()).thenReturn(Locale.ENGLISH);
    }

    @Test
    public void testItemCreationFromThingNotThere() {
        resetAndPrepareListener();

        provider.thingRegistryListener.added(THING);
        verify(listener, only()).added(same(provider), same(ITEM));
    }

    @Test
    public void testItemCreationFromThingAlreadyExists() {
        when(itemRegistry.get(eq(ITEM_NAME))).thenReturn(ITEM);

        resetAndPrepareListener();

        provider.thingRegistryListener.added(THING);
        verify(listener, never()).added(same(provider), same(ITEM));
    }

    @Test
    public void testItemRemovalFromThingLinkRemoved() {
        provider.linkRegistryListener.added(LINK);

        resetAndPrepareListener();

        provider.thingRegistryListener.removed(THING);
        verify(listener, never()).added(same(provider), same(ITEM));
        verify(listener, only()).removed(same(provider), same(ITEM));
    }

    @Test
    public void testItemCreationFromLinkNotThere() {
        provider.linkRegistryListener.added(LINK);
        verify(listener, only()).added(same(provider), same(ITEM));
    }

    @Test
    public void testItemCreationFromLinkAlreadyExists() {
        when(itemRegistry.get(eq(ITEM_NAME))).thenReturn(ITEM);

        provider.linkRegistryListener.added(LINK);
        verify(listener, never()).added(same(provider), same(ITEM));
    }

    @Test
    public void testItemRemovalFromLinkLinkRemoved() {
        provider.linkRegistryListener.added(LINK);

        resetAndPrepareListener();

        provider.linkRegistryListener.removed(LINK);
        verify(listener, never()).added(same(provider), same(ITEM));
        verify(listener, only()).removed(same(provider), same(ITEM));
    }

    @Test
    public void testItemRemovalItemFromOtherProvider() {
        provider.linkRegistryListener.added(LINK);

        resetAndPrepareListener();

        provider.itemRegistryListener.beforeAdding(new NumberItem(ITEM_NAME));
        verify(listener, only()).removed(same(provider), same(ITEM));
        verify(listener, never()).added(same(provider), same(ITEM));
    }

    @Test
    public void testDisableBeforeDelayedInitialization() throws Exception {
        provider = createProvider();
        reset(linkRegistry);

        // Set the initialization delay to 40ms so we don't have to wait 2000ms to do the assertion
        Field field = ChannelItemProvider.class.getDeclaredField("INITIALIZATION_DELAY_NANOS");
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(provider, TimeUnit.MILLISECONDS.toNanos(40));

        Map<String, Object> props = new HashMap<>();
        props.put("enabled", "true");
        provider.activate(props);

        provider.linkRegistryListener.added(LINK);
        verify(listener, never()).added(same(provider), same(ITEM));
        verify(linkRegistry, never()).getAll();

        props = new HashMap<>();
        props.put("enabled", "false");
        provider.modified(props);

        Thread.sleep(100);

        provider.linkRegistryListener.added(LINK);
        verify(listener, never()).added(same(provider), same(ITEM));
        verify(linkRegistry, never()).getAll();
    }

    @SuppressWarnings("unchecked")
    private void resetAndPrepareListener() {
        reset(listener);
        doAnswer(invocation -> {
            // this is crucial as it mimics the real ItemRegistry's behavior
            provider.itemRegistryListener.afterRemoving((Item) invocation.getArguments()[1]);
            return null;
        }).when(listener).removed(same(provider), any(Item.class));
        doAnswer(invocation -> {
            // this is crucial as it mimics the real ItemRegistry's behavior
            provider.itemRegistryListener.beforeAdding((Item) invocation.getArguments()[1]);
            return null;
        }).when(listener).added(same(provider), any(Item.class));
        when(linkRegistry.getBoundChannels(eq(ITEM_NAME))).thenReturn(Collections.singleton(CHANNEL_UID));
        when(linkRegistry.getLinks(eq(CHANNEL_UID))).thenReturn(Collections.singleton(LINK));
    }

    private ChannelItemProvider createProvider() {
        ChannelItemProvider provider = new ChannelItemProvider(localeProvider, mock(ChannelTypeRegistry.class),
                thingRegistry, itemRegistry, linkRegistry);
        provider.addItemFactory(itemFactory);
        provider.addProviderChangeListener(listener);
        return provider;
    }

}
