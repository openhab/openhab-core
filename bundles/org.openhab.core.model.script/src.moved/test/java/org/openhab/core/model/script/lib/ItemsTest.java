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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;

/**
 * Tests for {@link Items}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ItemsTest {

    private static final String ITEM_NAME = "TestItem";
    private static final String UNKNOWN_ITEM = "UnknownItem";
    private static final String TAG = "Tag1";
    private static final String TYPE = "Switch";
    private static final String NAMESPACE = "test";
    private static final String VALUE = "myValue";

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

    @BeforeEach
    public void setUp() throws Exception {
        when(itemRegistry.get(ITEM_NAME)).thenReturn(item);
        when(itemRegistry.get(UNKNOWN_ITEM)).thenReturn(null);
        when(itemRegistry.getAll()).thenReturn(List.of(item, item2));
        when(itemRegistry.getItems(ITEM_NAME)).thenReturn(List.of(item));
        when(itemRegistry.getItemsByTag(TAG)).thenReturn(List.of(item2));
        when(itemRegistry.getItemsOfType(TYPE)).thenReturn(List.of(item, item2));
        when(itemRegistry.getItemsByTagAndType(TYPE, TAG)).thenReturn(List.of(item2));
        when(metadataRegistry.get(new MetadataKey(NAMESPACE, ITEM_NAME))).thenReturn(metadata);
        when(metadataRegistry.update(new Metadata(new MetadataKey(NAMESPACE, ITEM_NAME), "foo", null)))
                .thenReturn(metadata);
        when(metadataRegistry.remove(new MetadataKey(NAMESPACE, ITEM_NAME))).thenReturn(metadata);
        doReturn(metadata).when(metadataRegistry).add(any(Metadata.class));

        new ScriptServiceUtil(itemRegistry, thingRegistry, eventPublisher, modelRepository, metadataRegistry,
                ruleRegistry, itemChannelLinkRegistry, timeZoneProvider, localeProvider, scheduler);
    }

    @AfterEach
    public void tearDown() throws Exception {
        nullScriptServiceUtilInstance();
    }

    @Test
    public void testExists() {
        assertThat(Items.exists(ITEM_NAME), is(true));
        assertThat(Items.exists(UNKNOWN_ITEM), is(false));
    }

    @Test
    public void testGet() {
        assertThat(Items.get(ITEM_NAME), is(item));
        assertNull(Items.get(UNKNOWN_ITEM));
    }

    @Test
    public void testGetAll() {
        Collection<Item> result = Items.getAll();
        assertThat(result, is(List.of(item, item2)));
    }

    @Test
    public void testGetByPattern() {
        Collection<Item> result = Items.getByPattern(ITEM_NAME);
        assertThat(result, is(List.of(item)));
    }

    @Test
    public void testGetByTag() {
        Collection<Item> result = Items.getByTag(TAG);
        assertThat(result, is(List.of(item2)));
    }

    @Test
    public void testGetOfType() {
        Collection<Item> result = Items.getOfType(TYPE);
        assertThat(result, is(List.of(item, item2)));
    }

    @Test
    public void testGetByTagAndType() {
        Collection<Item> result = Items.getByTagAndType(TYPE, TAG);
        assertThat(result, is(List.of(item2)));
    }

    @Test
    public void testGetMetadata() {
        assertThat(Items.getMetadata(ITEM_NAME, NAMESPACE), is(metadata));

        assertThrows(IllegalArgumentException.class, () -> Items.getMetadata(null, NAMESPACE));
        assertThrows(IllegalArgumentException.class, () -> Items.getMetadata(ITEM_NAME, null));
    }

    @Test
    public void testAddMetadata() {
        assertDoesNotThrow(() -> Items.addMetadata(ITEM_NAME, NAMESPACE, VALUE));
        assertDoesNotThrow(() -> Items.addMetadata(ITEM_NAME, NAMESPACE, VALUE, "arg1", "val1"));
        assertDoesNotThrow(() -> Items.addMetadata(ITEM_NAME, NAMESPACE, VALUE, new Object[0]));
        verify(metadataRegistry, times(3)).add(any(Metadata.class));

        assertThrows(IllegalArgumentException.class,
                () -> Items.addMetadata(ITEM_NAME, NAMESPACE, forceNull(), Map.of("arg1", "val1")));
        assertThrows(IllegalArgumentException.class,
                () -> Items.addMetadata(ITEM_NAME, NAMESPACE, VALUE, Boolean.TRUE, "val1"));
        assertThrows(IllegalArgumentException.class,
                () -> Items.addMetadata(ITEM_NAME, NAMESPACE, VALUE, "arg1", "val1", "arg2"));
        assertThrows(IllegalArgumentException.class,
                () -> Items.addMetadata(ITEM_NAME, NAMESPACE, VALUE, "arg1", forceNull()));
        assertThrows(IllegalArgumentException.class, () -> Items.addMetadata(forceNull(), NAMESPACE, VALUE));
        assertThrows(IllegalArgumentException.class, () -> Items.addMetadata(ITEM_NAME, forceNull(), VALUE));
    }

    @Test
    public void testRemoveMetadata() {
        assertThat(Items.removeMetadata(ITEM_NAME, NAMESPACE), is(metadata));
        assertThrows(IllegalArgumentException.class, () -> Items.removeMetadata(null, NAMESPACE));
        assertThrows(IllegalArgumentException.class, () -> Items.removeMetadata(ITEM_NAME, null));
    }

    @Test
    public void testUpdateMetadata() {
        assertThat(Items.updateMetadata(ITEM_NAME, NAMESPACE, VALUE), is(metadata));
        assertThat(Items.updateMetadata(ITEM_NAME, NAMESPACE, VALUE, "arg1", "val1"), is(metadata));
        assertThat(Items.updateMetadata(ITEM_NAME, NAMESPACE, VALUE, new Object[0]), is(metadata));
        assertThat(Items.updateMetadata(ITEM_NAME, NAMESPACE, VALUE, Map.of("arg1", "val1", "arg2", "val2")),
                is(metadata));

        assertThrows(IllegalArgumentException.class, () -> Items.updateMetadata(forceNull(), NAMESPACE, VALUE));
        assertThrows(IllegalArgumentException.class, () -> Items.updateMetadata(ITEM_NAME, forceNull(), VALUE));
        String nullValue = forceNull();
        assertThrows(IllegalArgumentException.class, () -> Items.updateMetadata(ITEM_NAME, NAMESPACE, nullValue));
    }

    private void nullScriptServiceUtilInstance() throws Exception {
        Field field = ScriptServiceUtil.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    @SuppressWarnings("null")
    private <T> T forceNull() {
        return null;
    }
}
