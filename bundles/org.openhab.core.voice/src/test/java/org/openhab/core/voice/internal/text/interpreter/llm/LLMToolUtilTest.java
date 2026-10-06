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

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.voice.internal.text.interpreter.llm.LLMToolUtil.ItemAndPermission;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;

/**
 * Tests for {@link LLMToolUtil}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class LLMToolUtilTest {
    private static final String ITEM_NAME = "TestItem";

    private final ItemRegistry itemRegistry = mock(ItemRegistry.class);
    private final ItemPermissionResolver itemPermissionResolver = mock(ItemPermissionResolver.class);
    private final Item item = mock(Item.class);

    @BeforeEach
    public void setUp() throws ItemNotFoundException {
        when(itemRegistry.getItem(ITEM_NAME)).thenReturn(item);
    }

    @Test
    public void resolveAndValidateItemReturnsItemAndPermission() throws LLMToolException {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_ONLY);

        ItemAndPermission result = LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver,
                Map.of("itemName", ITEM_NAME));

        assertEquals(item, result.item());
        assertEquals(ItemPermission.READ_ONLY, result.permission());
    }

    @Test
    public void resolveAndValidateItemWithReadWritePermission() throws LLMToolException {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_WRITE);

        ItemAndPermission result = LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver,
                Map.of("itemName", ITEM_NAME));

        assertEquals(item, result.item());
        assertEquals(ItemPermission.READ_WRITE, result.permission());
    }

    @Test
    public void resolveAndValidateDotPrefixedItem() throws LLMToolException, ItemNotFoundException {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.READ_ONLY);

        ItemAndPermission result = LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver,
                Map.of("itemName", ".." + ITEM_NAME));

        verify(itemRegistry).getItem(ITEM_NAME);
        assertEquals(item, result.item());
        assertEquals(ItemPermission.READ_ONLY, result.permission());
    }

    @Test
    public void resolveAndValidateItemThrowsLTEOnMissingItemName() {
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver, Map.of()));
        assertEquals("Missing or invalid required parameter 'itemName'", exception.getMessage());
    }

    @Test
    @NonNullByDefault({})
    public void resolveAndValidateItemThrowsLTEOnNullItemName() {
        Map<String, Object> params = Collections.singletonMap("itemName", null);
        LLMToolException exception = assertThrows(LLMToolException.class,
                () -> LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver, params));
        assertEquals("Missing or invalid required parameter 'itemName'", exception.getMessage());
    }

    @Test
    public void resolveAndValidateItemThrowsLTEOnInvalidParameterType() {
        LLMToolException exception = assertThrows(LLMToolException.class, () -> LLMToolUtil
                .resolveAndValidateItem(itemRegistry, itemPermissionResolver, Map.of("itemName", 123)));
        assertEquals("Missing or invalid required parameter 'itemName'", exception.getMessage());
    }

    @Test
    public void resolveAndValidateItemThrowsLTEOnItemNotFound() throws ItemNotFoundException {
        when(itemRegistry.getItem("UnknownItem")).thenThrow(new ItemNotFoundException("UnknownItem"));

        LLMToolException exception = assertThrows(LLMToolException.class, () -> LLMToolUtil
                .resolveAndValidateItem(itemRegistry, itemPermissionResolver, Map.of("itemName", "UnknownItem")));
        assertEquals("Item not found: UnknownItem", exception.getMessage());
        assertInstanceOf(ItemNotFoundException.class, exception.getCause());
    }

    @Test
    public void resolveAndValidateItemThrowsLTEOnNoAccess() {
        when(itemPermissionResolver.getPermission(item)).thenReturn(ItemPermission.NO_ACCESS);

        LLMToolException exception = assertThrows(LLMToolException.class, () -> LLMToolUtil
                .resolveAndValidateItem(itemRegistry, itemPermissionResolver, Map.of("itemName", ITEM_NAME)));
        assertEquals("Item not found: " + ITEM_NAME, exception.getMessage());
        assertNull(exception.getCause());
    }
}
