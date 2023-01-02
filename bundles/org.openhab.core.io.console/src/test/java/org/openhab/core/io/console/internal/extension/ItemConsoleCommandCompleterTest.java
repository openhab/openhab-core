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
package org.openhab.core.io.console.internal.extension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemNotUniqueException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.SwitchItem;

/**
 * @author Cody Cutrer - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class ItemConsoleCommandCompleterTest {
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;

    @BeforeEach
    public void setup() {
        List<Item> items = List.of(new SwitchItem("Item1"), new SwitchItem("Item2"), new SwitchItem("JItem1"));
        when(itemRegistryMock.getAll()).thenReturn(items);
    }

    private void mockGetItemByPattern() throws ItemNotFoundException, ItemNotUniqueException {
        when(itemRegistryMock.getItemByPattern(anyString())).thenAnswer(invocation -> {
            switch ((String) invocation.getArguments()[0]) {
                case "Item1":
                    return itemRegistryMock.getAll().iterator().next();
                default:
                    throw new ItemNotFoundException("It");
            }
        });
    }

    @Test
    public void completeItems() throws ItemNotFoundException, ItemNotUniqueException {
        var completer = new ItemConsoleCommandCompleter(itemRegistryMock);
        var candidates = new ArrayList<String>();

        assertTrue(completer.complete(new String[] { "It" }, 0, 2, candidates));
        assertEquals(2, candidates.size());
        assertEquals("Item1 ", candidates.get(0));
        assertEquals("Item2 ", candidates.get(1));
        candidates.clear();

        assertTrue(completer.complete(new String[] { "JI" }, 0, 2, candidates));
        assertEquals(1, candidates.size());
        assertEquals("JItem1 ", candidates.get(0));
        candidates.clear();

        // case sensitive
        assertFalse(completer.complete(new String[] { "it" }, 0, 2, candidates));
        assertTrue(candidates.isEmpty());

        // doesn't complete anything when we're not referring to the current argument
        assertFalse(completer.complete(new String[] { "It", "It" }, 1, 2, candidates));
        assertTrue(candidates.isEmpty());

        // doesn't complete anything for the second argument
        assertFalse(completer.complete(new String[] { "Item1", "" }, 1, 0, candidates));
        assertTrue(candidates.isEmpty());
    }

    @Test
    public void completeSend() throws ItemNotFoundException, ItemNotUniqueException {
        var completer = new ItemConsoleCommandCompleter(itemRegistryMock,
                i -> i.getAcceptedCommandTypes().toArray(Class<?>[]::new));
        var candidates = new ArrayList<String>();
        mockGetItemByPattern();

        // Can't find the item; no commands at all
        assertFalse(completer.complete(new String[] { "It", "O" }, 1, 1, candidates));
        assertTrue(candidates.isEmpty());

        assertTrue(completer.complete(new String[] { "Item1", "" }, 1, 0, candidates));
        assertEquals(3, candidates.size());
        assertEquals("OFF ", candidates.get(0));
        assertEquals("ON ", candidates.get(1));
        assertEquals("REFRESH ", candidates.get(2));
        candidates.clear();

        // case insensitive
        assertTrue(completer.complete(new String[] { "Item1", "o" }, 1, 1, candidates));
        assertEquals(2, candidates.size());
        assertEquals("OFF ", candidates.get(0));
        assertEquals("ON ", candidates.get(1));
    }

    @Test
    public void completeUpdate() throws ItemNotFoundException, ItemNotUniqueException {
        var completer = new ItemConsoleCommandCompleter(itemRegistryMock,
                i -> i.getAcceptedDataTypes().toArray(Class<?>[]::new));
        var candidates = new ArrayList<String>();
        mockGetItemByPattern();

        // Can't find the item; no commands at all
        assertFalse(completer.complete(new String[] { "It", "O" }, 1, 1, candidates));
        assertTrue(candidates.isEmpty());

        assertTrue(completer.complete(new String[] { "Item1", "" }, 1, 0, candidates));
        assertEquals(4, candidates.size());
        assertEquals("NULL ", candidates.get(0));
        assertEquals("OFF ", candidates.get(1));
        assertEquals("ON ", candidates.get(2));
        assertEquals("UNDEF ", candidates.get(3));
        candidates.clear();

        // case insensitive
        assertTrue(completer.complete(new String[] { "Item1", "o" }, 1, 1, candidates));
        assertEquals(2, candidates.size());
        assertEquals("OFF ", candidates.get(0));
        assertEquals("ON ", candidates.get(1));
    }
}
