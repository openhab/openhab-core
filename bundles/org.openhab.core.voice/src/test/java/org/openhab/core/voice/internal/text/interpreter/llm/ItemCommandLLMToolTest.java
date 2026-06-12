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
package org.openhab.core.voice.internal.text.interpreter.llm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;

/**
 * Test class for {@link ItemCommandLLMTool}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ItemCommandLLMToolTest {
    private static final String ITEM_NAME = "TestItem";

    private final ItemRegistry itemRegistry = mock(ItemRegistry.class);
    private final ItemPermissionResolver itemPermissionResolver = mock(ItemPermissionResolver.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final Item item = mock(Item.class);

    private @NonNullByDefault({}) ItemCommandLLMTool tool;

    @BeforeEach
    public void setUp() throws ItemNotFoundException {
        when(itemRegistry.getItem(ITEM_NAME)).thenReturn(item);
        when(item.getName()).thenReturn(ITEM_NAME);
        when(item.getAcceptedCommandTypes()).thenReturn(List.of(OnOffType.class));
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_WRITE);

        tool = new ItemCommandLLMTool(itemRegistry, itemPermissionResolver, eventPublisher);
    }

    @Test
    public void getUIDReturnsCorrectId() {
        assertEquals(ItemCommandLLMTool.ID, tool.getUID());
    }

    @Test
    public void callSendsCommand() throws LLMToolException {
        String result = tool.call(Map.of("itemName", ITEM_NAME, "command", "ON"), Locale.ENGLISH);
        assertTrue(result.contains("Successfully sent command 'ON' to item 'TestItem'"));
        verify(eventPublisher).post(any());
    }

    @Test
    public void callThrowsLTEOnItemNotFound() throws ItemNotFoundException {
        when(itemRegistry.getItem("UnknownItem")).thenThrow(new ItemNotFoundException("UnknownItem"));
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", "UnknownItem", "command", "ON"), Locale.ENGLISH));
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("Item not found: UnknownItem"));
        verify(eventPublisher, never()).post(any());
    }

    @Test
    public void callThrowsLTEOnNotAccessible() {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.NO_ACCESS);
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "command", "ON"), Locale.ENGLISH));
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("Item not found: TestItem"));
        verify(eventPublisher, never()).post(any());
    }

    @Test
    public void callThrowsLTEOnReadOnly() {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_ONLY);
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "command", "ON"), Locale.ENGLISH));
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("Item is read-only: TestItem"));
        verify(eventPublisher, never()).post(any());
    }

    @Test
    public void callThrowsLTEOnInvalidCommand() {
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME, "command", "INVALID"), Locale.ENGLISH));
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("Failed to parse command 'INVALID' for item 'TestItem'"));
        verify(eventPublisher, never()).post(any());
    }

    @Test
    public void callThrowsLTEOnMissingParams() {
        assertThrows(LLMToolException.class, () -> tool.call(Map.of(), Locale.ENGLISH));
    }
}
