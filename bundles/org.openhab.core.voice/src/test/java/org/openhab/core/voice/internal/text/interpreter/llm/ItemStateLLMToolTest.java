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
import static org.mockito.Mockito.*;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.StateDescription;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;

/**
 * Test class for {@link ItemStateLLMTool}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ItemStateLLMToolTest {
    private static final String ITEM_NAME = "TestItem";

    private final ItemRegistry itemRegistry = mock(ItemRegistry.class);
    private final ItemPermissionResolver itemPermissionResolver = mock(ItemPermissionResolver.class);
    private final TimeZoneProvider timeZoneProvider = mock(TimeZoneProvider.class);
    private final Item item = mock(Item.class);

    private @NonNullByDefault({}) ItemStateLLMTool tool;

    @BeforeEach
    public void setUp() throws ItemNotFoundException {
        when(itemRegistry.getItem(ITEM_NAME)).thenReturn(item);
        when(item.getName()).thenReturn(ITEM_NAME);
        when(item.getState()).thenReturn(OnOffType.ON);
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_ONLY);
        when(timeZoneProvider.getTimeZone()).thenReturn(ZoneId.systemDefault());

        tool = new ItemStateLLMTool(itemRegistry, itemPermissionResolver, timeZoneProvider);
    }

    @Test
    public void getUIDReturnsCorrectId() {
        assertEquals(ItemStateLLMTool.ID, tool.getUID());
    }

    @Test
    public void callReturnsStateWhenNoDisplayState() throws LLMToolException {
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(null);
        String result = tool.call(Map.of("itemName", ITEM_NAME), Locale.ENGLISH);
        assertEquals("ON", result);
    }

    @Test
    public void callReturnsStateWhenDisplayStateSameAsRaw() throws LLMToolException {
        StateDescription stateDescription = mock(StateDescription.class);
        when(stateDescription.getPattern()).thenReturn("%s");
        when(stateDescription.getOptions()).thenReturn(List.of());
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(stateDescription);

        String result = tool.call(Map.of("itemName", ITEM_NAME), Locale.ENGLISH);
        assertEquals("ON", result);
    }

    @Test
    public void callReturnsDisplayAndRawStateWhenDisplayStateAvailableAndDifferent() throws LLMToolException {
        StateDescription stateDescription = mock(StateDescription.class);
        when(stateDescription.getPattern()).thenReturn("%s Test");
        when(stateDescription.getOptions()).thenReturn(List.of());
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(stateDescription);

        String result = tool.call(Map.of("itemName", ITEM_NAME), Locale.ENGLISH);
        assertEquals("ON Test (ON)", result);
    }

    @Test
    public void callThrowsLTEOnItemNotFound() throws ItemNotFoundException {
        when(itemRegistry.getItem("UnknownItem")).thenThrow(new ItemNotFoundException("UnknownItem"));
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", "UnknownItem"), Locale.ENGLISH));
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("Item not found: UnknownItem"));
    }

    @Test
    public void callThrowsLTEOnNotAccessible() {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.NO_ACCESS);
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> tool.call(Map.of("itemName", ITEM_NAME), Locale.ENGLISH));
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("Item not found: TestItem"));
    }

    @Test
    public void callThrowsLTEOnMissingParams() {
        assertThrows(LLMToolException.class, () -> tool.call(Map.of(), Locale.ENGLISH));
    }

    @Test
    public void callReturnsStateWithDotPrefixedItem() throws LLMToolException {
        when(item.getStateDescription(Locale.ENGLISH)).thenReturn(null);
        String result = tool.call(Map.of("itemName", "......" + ITEM_NAME), Locale.ENGLISH);
        assertEquals("ON", result);
    }
}
