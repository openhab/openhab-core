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
package org.openhab.core.internal.items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.internal.items.ExpireManager.ExpireConfig;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.UnDefType;

/**
 * The {@link ExpireManagerTest} tests the {@link ExpireManager}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
class ExpireManagerTest {

    private static final String ITEMNAME = "Test";
    private static final MetadataKey METADATA_KEY = new MetadataKey(ExpireManager.METADATA_NAMESPACE, ITEMNAME);

    private @NonNullByDefault({}) ExpireManager expireManager;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;

    @BeforeEach
    public void setup() {
        expireManager = new ExpireManager(new HashMap<>(), eventPublisherMock, metadataRegistryMock, itemRegistryMock);
    }

    @Test
    void testDefaultStateExpiry() throws InterruptedException {
        when(metadataRegistryMock.get(METADATA_KEY)).thenReturn(new Metadata(METADATA_KEY, "1s", null));

        Event event = ItemEventFactory.createCommandEvent(ITEMNAME, OnOffType.ON);

        expireManager.receive(event);

        verify(eventPublisherMock, never()).post(any());
        Thread.sleep(2500L);
        verify(eventPublisherMock)
                .post(eq(ItemEventFactory.createStateEvent(ITEMNAME, UnDefType.UNDEF, ExpireManager.EVENT_SOURCE)));
    }

    @Test
    void testStateExpiryWithCustomState() throws InterruptedException, ItemNotFoundException {
        Item testItem = new SwitchItem(ITEMNAME);
        when(itemRegistryMock.getItem(ITEMNAME)).thenReturn(testItem);
        when(metadataRegistryMock.get(METADATA_KEY)).thenReturn(config("1s,state=OFF"));

        Event event = ItemEventFactory.createCommandEvent(ITEMNAME, OnOffType.ON);

        expireManager.receive(event);

        verify(eventPublisherMock, never()).post(any());
        Thread.sleep(2500L);
        verify(eventPublisherMock)
                .post(eq(ItemEventFactory.createStateEvent(ITEMNAME, OnOffType.OFF, ExpireManager.EVENT_SOURCE)));
    }

    @Test
    void testStateExpiryWithCustomCommand() throws InterruptedException, ItemNotFoundException {
        Item testItem = new SwitchItem(ITEMNAME);
        when(itemRegistryMock.getItem(ITEMNAME)).thenReturn(testItem);
        when(metadataRegistryMock.get(METADATA_KEY)).thenReturn(config("1s,command=ON"));

        Event event = ItemEventFactory.createCommandEvent(ITEMNAME, OnOffType.OFF);

        expireManager.receive(event);

        verify(eventPublisherMock, never()).post(any());
        Thread.sleep(2500L);
        verify(eventPublisherMock)
                .post(eq(ItemEventFactory.createCommandEvent(ITEMNAME, OnOffType.ON, ExpireManager.EVENT_SOURCE)));
    }

    @Test
    void testCancelExpiry() throws InterruptedException, ItemNotFoundException {
        Item testItem = new SwitchItem(ITEMNAME);
        when(itemRegistryMock.getItem(ITEMNAME)).thenReturn(testItem);
        when(metadataRegistryMock.get(METADATA_KEY)).thenReturn(config("1s,ON"));

        Event event = ItemEventFactory.createCommandEvent(ITEMNAME, OnOffType.OFF);
        expireManager.receive(event);
        Thread.sleep(500L);
        event = ItemEventFactory.createCommandEvent(ITEMNAME, OnOffType.ON);
        expireManager.receive(event);
        Thread.sleep(2000L);
        verify(eventPublisherMock, never()).post(any());
    }

    @Test
    void testIgnoreStateUpdateExtendsExpiryOnStateChange() throws InterruptedException, ItemNotFoundException {
        Item testItem = new NumberItem(ITEMNAME);
        when(itemRegistryMock.getItem(ITEMNAME)).thenReturn(testItem);
        when(metadataRegistryMock.get(METADATA_KEY))
                .thenReturn(config("2s", Map.of(ExpireConfig.CONFIG_IGNORE_STATE_UPDATES, true)));

        Event event = ItemEventFactory.createCommandEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        event = ItemEventFactory.createStateChangedEvent(ITEMNAME, new DecimalType(2), new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        verify(eventPublisherMock, never()).post(any());
        Thread.sleep(2500L);
        verify(eventPublisherMock, times(1)).post(any());
    }

    @Test
    void testIgnoreStateUpdateExtendsExpiryOnCommand() throws InterruptedException, ItemNotFoundException {
        Item testItem = new NumberItem(ITEMNAME);
        when(itemRegistryMock.getItem(ITEMNAME)).thenReturn(testItem);
        when(metadataRegistryMock.get(METADATA_KEY))
                .thenReturn(config("2s", Map.of(ExpireConfig.CONFIG_IGNORE_STATE_UPDATES, true)));

        Event event = ItemEventFactory.createCommandEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        event = ItemEventFactory.createCommandEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        verify(eventPublisherMock, never()).post(any());
        Thread.sleep(2500L);
        verify(eventPublisherMock, times(1)).post(any());
    }

    @Test
    void testIgnoreStateUpdateDoesNotExtendExpiryOnStateUpdate() throws InterruptedException, ItemNotFoundException {
        Item testItem = new NumberItem(ITEMNAME);
        when(itemRegistryMock.getItem(ITEMNAME)).thenReturn(testItem);
        when(metadataRegistryMock.get(METADATA_KEY))
                .thenReturn(config("2s", Map.of(ExpireConfig.CONFIG_IGNORE_STATE_UPDATES, true)));

        Event event = ItemEventFactory.createCommandEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        event = ItemEventFactory.createStateEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(2500L);
        verify(eventPublisherMock, times(1)).post(any());
    }

    @Test
    void testIgnoreCommandsExtendsExpiryOnStateChange() throws InterruptedException, ItemNotFoundException {
        Item testItem = new NumberItem(ITEMNAME);
        when(itemRegistryMock.getItem(ITEMNAME)).thenReturn(testItem);
        when(metadataRegistryMock.get(METADATA_KEY))
                .thenReturn(config("2s", Map.of(ExpireConfig.CONFIG_IGNORE_COMMANDS, true)));

        Event event = ItemEventFactory.createStateEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        event = ItemEventFactory.createStateChangedEvent(ITEMNAME, new DecimalType(2), new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        verify(eventPublisherMock, never()).post(any());
        Thread.sleep(2000L);
        verify(eventPublisherMock, times(1)).post(any());
    }

    @Test
    void testIgnoreCommandsExtendsExpiryOnStateUpdate() throws InterruptedException, ItemNotFoundException {
        Item testItem = new NumberItem(ITEMNAME);
        when(itemRegistryMock.getItem(ITEMNAME)).thenReturn(testItem);
        when(metadataRegistryMock.get(METADATA_KEY))
                .thenReturn(config("2s", Map.of(ExpireConfig.CONFIG_IGNORE_COMMANDS, true)));

        Event event = ItemEventFactory.createStateEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        event = ItemEventFactory.createStateEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        verify(eventPublisherMock, never()).post(any());
        Thread.sleep(2000L);
        verify(eventPublisherMock, times(1)).post(any());
    }

    @Test
    void testIgnoreCommandDoesNotExtendExpiryOnStateUpdate() throws InterruptedException, ItemNotFoundException {
        Item testItem = new NumberItem(ITEMNAME);
        when(itemRegistryMock.getItem(ITEMNAME)).thenReturn(testItem);
        when(metadataRegistryMock.get(METADATA_KEY))
                .thenReturn(config("2s", Map.of(ExpireConfig.CONFIG_IGNORE_COMMANDS, true)));

        Event event = ItemEventFactory.createStateEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        event = ItemEventFactory.createCommandEvent(ITEMNAME, new DecimalType(1));
        expireManager.receive(event);
        Thread.sleep(1500L);
        verify(eventPublisherMock, times(1)).post(any());
    }

    @Test
    void testMetadataChange() throws InterruptedException, ItemNotFoundException {
        Metadata md = new Metadata(METADATA_KEY, "1s", null);
        when(metadataRegistryMock.get(METADATA_KEY)).thenReturn(md);

        Event event = ItemEventFactory.createCommandEvent(ITEMNAME, OnOffType.ON);
        expireManager.receive(event);

        verify(eventPublisherMock, never()).post(any());
        Thread.sleep(2500L);
        verify(eventPublisherMock)
                .post(eq(ItemEventFactory.createStateEvent(ITEMNAME, UnDefType.UNDEF, ExpireManager.EVENT_SOURCE)));

        when(metadataRegistryMock.get(METADATA_KEY)).thenReturn(null);
        expireManager.metadataChangeListener.removed(md);
        reset(eventPublisherMock);

        event = ItemEventFactory.createCommandEvent(ITEMNAME, OnOffType.ON);
        expireManager.receive(event);
        verify(eventPublisherMock, never()).post(any());
        Thread.sleep(2500L);
        verify(eventPublisherMock, never()).post(any());
    }

    @Test
    void testExpireConfig() {
        Item testItem = new SwitchItem(ITEMNAME);
        ExpireConfig cfg = new ExpireManager.ExpireConfig(testItem, "1s", Map.of());
        assertEquals(Duration.ofSeconds(1), cfg.duration);
        assertEquals(UnDefType.UNDEF, cfg.expireState);
        assertNull(cfg.expireCommand);
        assertFalse(cfg.ignoreStateUpdates);
        assertFalse(cfg.ignoreCommands);

        cfg = new ExpireManager.ExpireConfig(testItem, "5h 3m 2s", Map.of());
        assertEquals(Duration.ofHours(5).plusMinutes(3).plusSeconds(2), cfg.duration);
        assertEquals(UnDefType.UNDEF, cfg.expireState);
        assertNull(cfg.expireCommand);
        assertFalse(cfg.ignoreStateUpdates);
        assertFalse(cfg.ignoreCommands);

        cfg = new ExpireManager.ExpireConfig(testItem, "1h,OFF", Map.of());
        assertEquals(Duration.ofHours(1), cfg.duration);
        assertEquals(OnOffType.OFF, cfg.expireState);
        assertNull(cfg.expireCommand);
        assertFalse(cfg.ignoreStateUpdates);
        assertFalse(cfg.ignoreCommands);

        cfg = new ExpireManager.ExpireConfig(testItem, "1h,state=OFF", Map.of());
        assertEquals(Duration.ofHours(1), cfg.duration);
        assertEquals(OnOffType.OFF, cfg.expireState);
        assertNull(cfg.expireCommand);
        assertFalse(cfg.ignoreStateUpdates);
        assertFalse(cfg.ignoreCommands);

        cfg = new ExpireManager.ExpireConfig(testItem, "1h,command=OFF", Map.of());
        assertEquals(Duration.ofHours(1), cfg.duration);
        assertNull(cfg.expireState);
        assertEquals(OnOffType.OFF, cfg.expireCommand);
        assertFalse(cfg.ignoreStateUpdates);
        assertFalse(cfg.ignoreCommands);

        cfg = new ExpireManager.ExpireConfig(testItem, "1h,command=OFF",
                Map.of(ExpireConfig.CONFIG_IGNORE_STATE_UPDATES, true));
        assertEquals(Duration.ofHours(1), cfg.duration);
        assertNull(cfg.expireState);
        assertEquals(OnOffType.OFF, cfg.expireCommand);
        assertTrue(cfg.ignoreStateUpdates);
        assertFalse(cfg.ignoreCommands);

        cfg = new ExpireManager.ExpireConfig(testItem, "5h 3m 2s",
                Map.of(ExpireConfig.CONFIG_IGNORE_STATE_UPDATES, "true"));
        assertEquals(Duration.ofHours(5).plusMinutes(3).plusSeconds(2), cfg.duration);
        assertEquals(UnDefType.UNDEF, cfg.expireState);
        assertNull(cfg.expireCommand);
        assertTrue(cfg.ignoreStateUpdates);
        assertFalse(cfg.ignoreCommands);

        cfg = new ExpireManager.ExpireConfig(testItem, "1h,command=OFF",
                Map.of(ExpireConfig.CONFIG_IGNORE_COMMANDS, true));
        assertEquals(Duration.ofHours(1), cfg.duration);
        assertNull(cfg.expireState);
        assertEquals(OnOffType.OFF, cfg.expireCommand);
        assertFalse(cfg.ignoreStateUpdates);
        assertTrue(cfg.ignoreCommands);

        cfg = new ExpireManager.ExpireConfig(testItem, "5h 3m 2s", Map.of(ExpireConfig.CONFIG_IGNORE_COMMANDS, "true"));
        assertEquals(Duration.ofHours(5).plusMinutes(3).plusSeconds(2), cfg.duration);
        assertEquals(UnDefType.UNDEF, cfg.expireState);
        assertNull(cfg.expireCommand);
        assertFalse(cfg.ignoreStateUpdates);
        assertTrue(cfg.ignoreCommands);

        try {
            cfg = new ExpireManager.ExpireConfig(testItem, "1h,command=OPEN", Map.of());
            fail();
        } catch (IllegalArgumentException e) {
            // expected as command is invalid
        }

        try {
            cfg = new ExpireManager.ExpireConfig(testItem, "1h,OPEN", Map.of());
            fail();
        } catch (IllegalArgumentException e) {
            // expected as state is invalid
        }

        testItem = new NumberItem("Number:Temperature", ITEMNAME);
        cfg = new ExpireManager.ExpireConfig(testItem, "1h,15 °C", Map.of());
        assertEquals(Duration.ofHours(1), cfg.duration);
        assertEquals(new QuantityType<Temperature>("15 °C"), cfg.expireState);
        assertEquals(null, cfg.expireCommand);

        testItem = new StringItem(ITEMNAME);
        cfg = new ExpireManager.ExpireConfig(testItem, "1h,NULL", Map.of());
        assertEquals(Duration.ofHours(1), cfg.duration);
        assertEquals(UnDefType.NULL, cfg.expireState);
        assertEquals(null, cfg.expireCommand);

        cfg = new ExpireManager.ExpireConfig(testItem, "1h,'NULL'", Map.of());
        assertEquals(Duration.ofHours(1), cfg.duration);
        assertEquals(new StringType("NULL"), cfg.expireState);
        assertEquals(null, cfg.expireCommand);

        cfg = new ExpireManager.ExpireConfig(testItem, "1h,'UNDEF'", Map.of());
        assertEquals(Duration.ofHours(1), cfg.duration);
        assertEquals(new StringType("UNDEF"), cfg.expireState);
        assertEquals(null, cfg.expireCommand);
    }

    private Metadata config(String metadata) {
        return new Metadata(METADATA_KEY, metadata, null);
    }

    private Metadata config(String metadata, Map<String, Object> configuration) {
        return new Metadata(METADATA_KEY, metadata, configuration);
    }
}
