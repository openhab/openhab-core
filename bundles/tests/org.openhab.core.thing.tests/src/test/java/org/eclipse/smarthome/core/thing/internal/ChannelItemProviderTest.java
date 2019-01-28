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
package org.eclipse.smarthome.core.thing.internal;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemFactory;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
public class ChannelItemProviderTest {

    private static final String ITEM_NAME = "test";
    private static final ChannelUID CHANNEL_UID = new ChannelUID("test:test:test:test");
    private static final NumberItem ITEM = new NumberItem(ITEM_NAME);

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
    public void setup() throws Exception {
        initMocks(this);

        provider = createProvider();

        Map<String, Object> props = new HashMap<>();
        props.put("enabled", "true");
        props.put("initialDelay", "false");
        provider.activate(props);

        when(thingRegistry.getChannel(same(CHANNEL_UID)))
                .thenReturn(ChannelBuilder.create(CHANNEL_UID, "Number").build());
        when(itemFactory.createItem("Number", ITEM_NAME)).thenReturn(ITEM);
        when(localeProvider.getLocale()).thenReturn(Locale.ENGLISH);
    }

    @Test
    public void testItemCreation_notThere() throws Exception {
        provider.linkRegistryListener.added(new ItemChannelLink(ITEM_NAME, CHANNEL_UID));
        verify(listener, only()).added(same(provider), same(ITEM));
    }

    @Test
    public void testItemCreation_alreadyExists() throws Exception {
        when(itemRegistry.get(eq(ITEM_NAME))).thenReturn(ITEM);

        provider.linkRegistryListener.added(new ItemChannelLink(ITEM_NAME, CHANNEL_UID));
        verify(listener, never()).added(same(provider), same(ITEM));
    }

    @Test
    public void testItemRemoval_linkRemoved() throws Exception {
        provider.linkRegistryListener.added(new ItemChannelLink(ITEM_NAME, CHANNEL_UID));

        resetAndPrepareListener();

        provider.linkRegistryListener.removed(new ItemChannelLink(ITEM_NAME, CHANNEL_UID));
        verify(listener, never()).added(same(provider), same(ITEM));
        verify(listener, only()).removed(same(provider), same(ITEM));
    }

    @Test
    public void testItemRemoval_itemFromOtherProvider() throws Exception {
        provider.linkRegistryListener.added(new ItemChannelLink(ITEM_NAME, CHANNEL_UID));

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

        provider.linkRegistryListener.added(new ItemChannelLink(ITEM_NAME, CHANNEL_UID));
        verify(listener, never()).added(same(provider), same(ITEM));
        verify(linkRegistry, never()).getAll();

        props = new HashMap<>();
        props.put("enabled", "false");
        provider.modified(props);

        Thread.sleep(100);

        provider.linkRegistryListener.added(new ItemChannelLink(ITEM_NAME, CHANNEL_UID));
        verify(listener, never()).added(same(provider), same(ITEM));
        verify(linkRegistry, never()).getAll();
    }

    @SuppressWarnings("unchecked")
    private void resetAndPrepareListener() {
        reset(listener);
        doAnswer(invocation -> {
            // this is crucial as it mimicks the real ItemRegistry's behavior
            provider.itemRegistryListener.afterRemoving((Item) invocation.getArguments()[1]);
            return null;
        }).when(listener).removed(same(provider), any(Item.class));
        doAnswer(invocation -> {
            // this is crucial as it mimicks the real ItemRegistry's behavior
            provider.itemRegistryListener.beforeAdding((Item) invocation.getArguments()[1]);
            return null;
        }).when(listener).added(same(provider), any(Item.class));
        when(linkRegistry.getBoundChannels(eq(ITEM_NAME))).thenReturn(Collections.singleton(CHANNEL_UID));
        when(linkRegistry.getLinks(eq(CHANNEL_UID)))
                .thenReturn(Collections.singleton(new ItemChannelLink(ITEM_NAME, CHANNEL_UID)));
    }

    private ChannelItemProvider createProvider() {
        ChannelItemProvider provider = new ChannelItemProvider();
        provider.setItemRegistry(itemRegistry);
        provider.setThingRegistry(thingRegistry);
        provider.setItemChannelLinkRegistry(linkRegistry);
        provider.addItemFactory(itemFactory);
        provider.setLocaleProvider(localeProvider);
        provider.addProviderChangeListener(listener);
        provider.setChannelTypeRegistry(mock(ChannelTypeRegistry.class));

        return provider;
    }

}
