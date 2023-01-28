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

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.items.ActiveItem;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemFactory;
import org.openhab.core.library.CoreItemFactory;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class ItemBuilderTest {

    private @NonNullByDefault({}) ItemBuilderFactoryImpl itemBuilderFactory;
    private @Mock @NonNullByDefault({}) ItemFactory factoryMock;
    private @Mock @NonNullByDefault({}) ActiveItem itemMock;
    private @Mock @NonNullByDefault({}) Item originalItemMock;

    @BeforeEach
    public void setup() {
        itemBuilderFactory = new ItemBuilderFactoryImpl(factoryMock);
    }

    @Test
    public void testMinimal() {
        when(factoryMock.createItem(anyString(), anyString())).thenReturn(itemMock);

        Item res = itemBuilderFactory.newItemBuilder(CoreItemFactory.STRING, "test").build();

        assertSame(itemMock, res);
        verify(factoryMock).createItem(eq(CoreItemFactory.STRING), eq("test"));
        verify(itemMock).setLabel(isNull());
        verify(itemMock).setCategory(isNull());
        verify(itemMock).addGroupNames(eq(Collections.emptyList()));
    }

    @Test
    public void testMinimalGroupItem() {
        Item resItem = itemBuilderFactory.newItemBuilder("Group", "test").build();

        assertEquals(GroupItem.class, resItem.getClass());
        GroupItem res = (GroupItem) resItem;
        verifyNoMoreInteractions(factoryMock);
        assertNull(res.getCategory());
        assertEquals(Collections.emptyList(), res.getGroupNames());
        assertNull(res.getLabel());
        assertNull(res.getFunction());
        assertNull(res.getBaseItem());
    }

    @Test
    public void testFull() {
        when(factoryMock.createItem(anyString(), anyString())).thenReturn(itemMock);

        Item res = itemBuilderFactory.newItemBuilder(CoreItemFactory.STRING, "test") //
                .withCategory("category") //
                .withGroups(List.of("a", "b")) //
                .withLabel("label") //
                .build();

        assertSame(itemMock, res);
        verify(factoryMock).createItem(eq(CoreItemFactory.STRING), eq("test"));
        verify(itemMock).setCategory(eq("category"));
        verify(itemMock).addGroupNames(eq(List.of("a", "b")));
        verify(itemMock).setLabel(eq("label"));
    }

    @Test
    public void testFullGroupItem() {
        Item baseItem = mock(Item.class);

        Item resItem = itemBuilderFactory.newItemBuilder("Group", "test") //
                .withCategory("category") //
                .withGroups(List.of("a", "b")) //
                .withLabel("label") //
                .withBaseItem(baseItem)//
                .withGroupFunction("fcn", new String[] { "param" }) //
                .build();

        assertEquals(GroupItem.class, resItem.getClass());
        GroupItem res = (GroupItem) resItem;
        verifyNoMoreInteractions(factoryMock);
        assertEquals("category", res.getCategory());
        assertEquals(List.of("a", "b"), res.getGroupNames());
        assertEquals("label", res.getLabel());
        assertEquals("fcn", res.getFunction());
        assertEquals(1, res.getFunctionParams().length);
        assertEquals("param", res.getFunctionParams()[0]);
        assertSame(baseItem, res.getBaseItem());
    }

    @Test
    public void testClone() {
        when(originalItemMock.getType()).thenReturn("type");
        when(originalItemMock.getName()).thenReturn("name");
        when(originalItemMock.getLabel()).thenReturn("label");
        when(originalItemMock.getCategory()).thenReturn("category");
        when(originalItemMock.getGroupNames()).thenReturn(List.of("a", "b"));

        when(factoryMock.createItem(anyString(), anyString())).thenReturn(itemMock);

        Item res = itemBuilderFactory.newItemBuilder(originalItemMock).build();

        assertSame(itemMock, res);
        verify(factoryMock).createItem(eq("type"), eq("name"));
        verify(itemMock).setCategory(eq("category"));
        verify(itemMock).addGroupNames(eq(List.of("a", "b")));
        verify(itemMock).setLabel(eq("label"));
    }

    @Test
    public void testCloneGroupItem() {
        Item baseItem = mock(Item.class);
        GroupItem originalItem = new GroupItem("name", baseItem, "sum", null);
        originalItem.setCategory("category");
        originalItem.setLabel("label");
        originalItem.addGroupNames("a", "b");

        Item resItem = itemBuilderFactory.newItemBuilder(originalItem).build();

        assertEquals(GroupItem.class, resItem.getClass());
        GroupItem res = (GroupItem) resItem;
        verifyNoMoreInteractions(factoryMock);
        assertEquals("category", res.getCategory());
        assertEquals(List.of("a", "b"), res.getGroupNames());
        assertEquals("label", res.getLabel());

        assertEquals("sum", res.getFunction());
        assertNull(res.getFunctionParams());
        assertSame(baseItem, res.getBaseItem());
    }

    @Test
    public void testNoFactory() {
        when(factoryMock.createItem(anyString(), anyString())).thenReturn(null);
        assertThrows(IllegalStateException.class,
                () -> itemBuilderFactory.newItemBuilder(CoreItemFactory.STRING, "test").build());
    }

    @Test
    public void testFunctionOnNonGroupItem() {
        GroupFunction mockFunction = mock(GroupFunction.class);
        assertThrows(IllegalArgumentException.class,
                () -> itemBuilderFactory.newItemBuilder(CoreItemFactory.STRING, "test").withGroupFunction("avg", null));
    }

    @Test
    public void testBaseItemOnNonGroupItem() {
        Item mockItem = mock(Item.class);
        assertThrows(IllegalArgumentException.class,
                () -> itemBuilderFactory.newItemBuilder(CoreItemFactory.STRING, "test").withBaseItem(mockItem));
    }
}
