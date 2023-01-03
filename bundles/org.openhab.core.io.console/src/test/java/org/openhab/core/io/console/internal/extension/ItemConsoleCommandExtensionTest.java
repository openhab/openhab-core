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
import org.openhab.core.io.console.ConsoleCommandCompleter;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.library.items.SwitchItem;

/**
 * @author Cody Cutrer - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class ItemConsoleCommandExtensionTest {
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) ManagedItemProvider managedItemProviderMock;
    private @NonNullByDefault({}) ConsoleCommandCompleter completer;

    @BeforeEach
    public void setup() {
        completer = new ItemConsoleCommandExtension(itemRegistryMock, managedItemProviderMock).getCompleter();
    }

    @Test
    public void completeSubcommands() {
        var candidates = new ArrayList<String>();

        assertTrue(completer.complete(new String[] { "" }, 0, 0, candidates));
        assertEquals(5, candidates.size());
        assertEquals("addTag ", candidates.get(0));
        assertEquals("clear ", candidates.get(1));
        assertEquals("list ", candidates.get(2));
        assertEquals("remove ", candidates.get(3));
        assertEquals("rmTag ", candidates.get(4));
        candidates.clear();

        assertTrue(completer.complete(new String[] { "A", "Item1" }, 0, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("addTag ", candidates.get(0));
    }

    @Test
    public void completeManagedItems() {
        List<Item> items = List.of(new SwitchItem("Item1"));
        when(managedItemProviderMock.getAll()).thenReturn(items);
        var candidates = new ArrayList<String>();

        assertFalse(completer.complete(new String[] { "bogus", "I" }, 1, 1, candidates));
        assertTrue(candidates.isEmpty());

        assertTrue(completer.complete(new String[] { "addTag", "I" }, 0, 6, candidates));
        assertEquals(1, candidates.size());
        assertEquals("addTag ", candidates.get(0));
        candidates.clear();

        assertTrue(completer.complete(new String[] { "addTag", "I" }, 1, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("Item1 ", candidates.get(0));
        candidates.clear();

        assertTrue(completer.complete(new String[] { "rmTag", "I" }, 1, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("Item1 ", candidates.get(0));
    }

    @Test
    public void completeAllItems() {
        List<Item> items = List.of(new SwitchItem("Item2"));
        when(itemRegistryMock.getAll()).thenReturn(items);
        var candidates = new ArrayList<String>();

        assertTrue(completer.complete(new String[] { "remove", "I" }, 0, 6, candidates));
        assertEquals(1, candidates.size());
        assertEquals("remove ", candidates.get(0));
        candidates.clear();

        assertTrue(completer.complete(new String[] { "remove", "I" }, 1, 1, candidates));
        assertEquals(1, candidates.size());
        assertEquals("Item2 ", candidates.get(0));
    }

    @Test
    public void completeRmTag() {
        var item3 = new SwitchItem("Item3");
        var item4 = new SwitchItem("Item4");
        item3.addTag("Tag1");
        when(managedItemProviderMock.get(anyString())).thenAnswer(invocation -> {
            switch ((String) invocation.getArguments()[0]) {
                case "Item3":
                    return item3;
                case "Item4":
                    return item4;
                default:
                    return null;
            }
        });
        var candidates = new ArrayList<String>();

        // wrong sub-command
        assertFalse(completer.complete(new String[] { "addTag", "Item3", "" }, 2, 0, candidates));
        assertTrue(candidates.isEmpty());

        // Item doesn't exist
        assertFalse(completer.complete(new String[] { "rmTag", "Item2", "" }, 2, 0, candidates));
        assertTrue(candidates.isEmpty());

        // Item has no tags
        assertFalse(completer.complete(new String[] { "rmTag", "Item4", "" }, 2, 0, candidates));
        assertTrue(candidates.isEmpty());

        assertTrue(completer.complete(new String[] { "rmTag", "Item3", "" }, 2, 0, candidates));
        assertEquals(1, candidates.size());
        assertEquals("Tag1 ", candidates.get(0));
    }
}
