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
package org.eclipse.smarthome.core.items;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.i18n.UnitProvider;
import org.eclipse.smarthome.core.internal.items.ItemRegistryImpl;
import org.eclipse.smarthome.core.items.events.ItemAddedEvent;
import org.eclipse.smarthome.core.items.events.ItemRemovedEvent;
import org.eclipse.smarthome.core.items.events.ItemUpdatedEvent;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.service.StateDescriptionService;
import org.eclipse.smarthome.test.java.JavaTest;
import org.eclipse.smarthome.test.storage.VolatileStorageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

/**
 * The {@link ItemRegistryImplTest} runs inside an OSGi container and tests the {@link ItemRegistry}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Andre Fuechsel - extended with tag tests
 * @author Kai Kreuzer - added tests for all items changed cases
 * @author Sebastian Janzen - added test for getItemsByTag
 */
@SuppressWarnings("null")
public class ItemRegistryImplTest extends JavaTest {

    private final static String ITEM_NAME = "switchItem";
    private final static String CAMERA_ITEM_NAME1 = "cameraItem1";
    private final static String CAMERA_ITEM_NAME2 = "cameraItem2";
    private final static String CAMERA_ITEM_NAME3 = "cameraItem3";
    private final static String CAMERA_ITEM_NAME4 = "cameraItem4";
    private final static String CAMERA_TAG = "camera";
    private final static String CAMERA_TAG_UPPERCASE = "CAMERA";
    private final static String SENSOR_TAG = "sensor";
    private final static String OTHER_TAG = "other";

    private ItemRegistry itemRegistry;
    private ManagedItemProvider itemProvider;

    @Mock
    private EventPublisher eventPublisher;

    @Before
    public void setUp() {
        initMocks(this);

        ItemFactory coreItemFactory = new CoreItemFactory();

        GenericItem cameraItem1 = new SwitchItem(CAMERA_ITEM_NAME1);
        GenericItem cameraItem2 = new SwitchItem(CAMERA_ITEM_NAME2);
        GenericItem cameraItem3 = new NumberItem(CAMERA_ITEM_NAME3);
        GenericItem cameraItem4 = new NumberItem(CAMERA_ITEM_NAME4);
        cameraItem1.addTag(CAMERA_TAG);
        cameraItem2.addTag(CAMERA_TAG);
        cameraItem2.addTag(SENSOR_TAG);
        cameraItem3.addTag(CAMERA_TAG);
        cameraItem4.addTag(CAMERA_TAG_UPPERCASE);

        // setup ManageItemProvider with necessary dependencies:
        itemProvider = new ManagedItemProvider() {
            {
                setStorageService(new VolatileStorageService());
                addItemFactory(coreItemFactory);
            }
        };

        itemProvider.add(new SwitchItem(ITEM_NAME));
        itemProvider.add(cameraItem1);
        itemProvider.add(cameraItem2);
        itemProvider.add(cameraItem3);
        itemProvider.add(cameraItem4);

        // setup ItemRegistryImpl with necessary dependencies:
        itemRegistry = new ItemRegistryImpl() {
            {
                addProvider(itemProvider);

                setManagedProvider(itemProvider);
                setEventPublisher(ItemRegistryImplTest.this.eventPublisher);
                setStateDescriptionService(mock(StateDescriptionService.class));
                setUnitProvider(mock(UnitProvider.class));
                setItemStateConverter(mock(ItemStateConverter.class));
            }
        };
    }

    @Test
    public void assertGetItemsReturnsItemFromRegisteredItemProvider() {
        List<Item> items = new ArrayList<>(itemRegistry.getItems());
        assertThat(items.size(), is(5));
        assertThat(items.get(0).getName(), is(equalTo(ITEM_NAME)));
    }

    @Test
    public void assertGetItemsOfTypeReturnsItemFromRegisteredItemProvider() {
        List<Item> items = new ArrayList<>(itemRegistry.getItemsOfType("Switch"));
        assertThat(items.size(), is(3));
        assertThat(items.get(0).getName(), is(equalTo(ITEM_NAME)));
    }

    @Test
    public void assertGetItemsByTagReturnsItemFromRegisteredItemProvider() {
        List<Item> items = new ArrayList<>(itemRegistry.getItemsByTag(CAMERA_TAG));
        assertThat(items, hasSize(4));

        List<String> itemNames = items.stream().map(i -> i.getName()).collect(toList());
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME1));
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME2));
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME3));
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME4));
    }

    @Test
    public void assertGetItemsByTagInUppercaseReturnsItemFromRegisteredItemProvider() {
        List<Item> items = new ArrayList<>(itemRegistry.getItemsByTag(CAMERA_TAG_UPPERCASE));
        assertThat(items, hasSize(4));

        List<String> itemNames = items.stream().map(i -> i.getName()).collect(toList());
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME1));
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME2));
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME3));
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME4));
    }

    @Test
    public void assertGetItemsByTagAndTypeReturnsItemFromRegistereItemProvider() {
        List<Item> items = new ArrayList<>(itemRegistry.getItemsByTagAndType("Switch", CAMERA_TAG));
        assertThat(items, hasSize(2));

        List<String> itemNames = items.stream().map(i -> i.getName()).collect(toList());
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME1));
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME2));

    }

    @Test
    public void assertGetItemsByTagWithTwoTagsReturnsItemFromRegisteredItemProvider() {
        List<Item> items = new ArrayList<>(itemRegistry.getItemsByTag(CAMERA_TAG, SENSOR_TAG));
        assertThat(items.size(), is(1));
        assertThat(items.get(0).getName(), is(equalTo(CAMERA_ITEM_NAME2)));
    }

    @Test
    public void assertGetItemsByTagReturnsNoItemFromRegisteredItemProvider() {
        assertThat(itemRegistry.getItemsByTag(OTHER_TAG).size(), is(0));
    }

    @Test
    public void assertGetItemsByTagCanFilterByClassAndTag() {
        List<SwitchItem> items = new ArrayList<>(itemRegistry.getItemsByTag(SwitchItem.class, CAMERA_TAG));
        assertThat(items, hasSize(2));

        List<String> itemNames = items.stream().map(i -> i.getName()).collect(toList());
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME1));
        assertThat(itemNames, hasItem(CAMERA_ITEM_NAME2));
    }

    @Test
    public void assertGetItemsByTagCanFilterByClassAndTagWithGenericItem() {
        assertThat(itemRegistry.getItemsByTag(GenericItem.class, CAMERA_TAG).size(), is(4));
    }

    @Test
    public void assertItemRegistrySetsAndRemovesMembersOfGroupItems() throws ItemNotFoundException {
        // test added item with group name is added as member to group
        itemProvider.add(new GroupItem("group"));
        SwitchItem switchItem = new SwitchItem("switch");
        switchItem.addGroupName("group");
        itemProvider.add(switchItem);

        GroupItem groupItem = (GroupItem) itemRegistry.getItem("group");
        assertThat(groupItem.getMembers().contains(switchItem), is(true));

        // test removed item is removed as member from group
        itemProvider.remove(switchItem.getUID());
        assertThat(groupItem.getMembers().contains(switchItem), is(false));

        // test added group item with gets all members set when it is added at last
        switchItem.addGroupName("group2");
        itemProvider.add(switchItem);
        itemProvider.add(new GroupItem("group2"));

        GroupItem groupItem2 = (GroupItem) itemRegistry.getItem("group2");
        assertThat(groupItem2.getMembers().contains(switchItem), is(true));

        // test update item
        itemProvider.add(new GroupItem("group3"));
        GroupItem groupItem3 = (GroupItem) itemRegistry.getItem("group");

        SwitchItem updatedSwitchItem = new SwitchItem("switch");
        updatedSwitchItem.addGroupName("group");
        updatedSwitchItem.addGroupName("group3");

        // old item has group: [group, group2], new item has [group, group3]
        itemProvider.update(updatedSwitchItem);

        assertThat(groupItem.getMembers().contains(updatedSwitchItem), is(true));
        assertThat(groupItem2.getMembers().contains(updatedSwitchItem), is(false));
        assertThat(groupItem3.getMembers().contains(updatedSwitchItem), is(true));
    }

    @Test
    public void testGroupUpdateWithModificationOfLiveInstance() {
        itemRegistry.add(new StringItem("item"));
        itemRegistry.add(new GroupItem("group"));

        GenericItem item = (GenericItem) itemRegistry.get("item"); // !
        item.addGroupName("group");
        itemRegistry.update(item);

        Item res = itemRegistry.get("item");
        assertEquals(1, res.getGroupNames().size());
        assertEquals("group", res.getGroupNames().get(0));

        GroupItem group = (GroupItem) itemRegistry.get("group");
        assertEquals(1, group.getMembers().size());
    }

    @Test
    public void assertItemRegistryChangeListenersAreInformedAboutItemChanges() {
        ItemRegistryChangeListener registryChangeListener = mock(ItemRegistryChangeListener.class);
        itemRegistry.addRegistryChangeListener(registryChangeListener);

        Item item = new SwitchItem("switch");
        itemProvider.add(item);
        Item newItem = new SwitchItem("switch");
        itemProvider.update(newItem);
        itemProvider.remove(item.getUID());

        verify(registryChangeListener, times(1)).added(item);
        verify(registryChangeListener, times(1)).updated(item, newItem);
        verify(registryChangeListener, times(1)).removed(item);
    }

    @Test
    public void assertItemRegistryIsThreadSafe() {
        AtomicInteger numberOfSuccessfulGetItemCalls = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    // get item throws an exception if item is not present and counter is not incremented
                    try {
                        itemRegistry.getItem(ITEM_NAME);
                        numberOfSuccessfulGetItemCalls.incrementAndGet();
                    } catch (ItemNotFoundException e) {
                        // bad, but counter will not incremented -> test fails.
                    }
                }
            }).start();
        }

        waitFor(() -> numberOfSuccessfulGetItemCalls.get() >= 100);
        assertThat(numberOfSuccessfulGetItemCalls.get(), is(100));
    }

    @Test
    public void testItemAddedEvent() {
        Item item = new SwitchItem("SomeSwitch");
        itemRegistry.add(item);

        verify(eventPublisher).post(org.mockito.ArgumentMatchers.isA(ItemAddedEvent.class));
    }

    @Test
    public void testItemUpdatedEvent() {
        itemRegistry.add(new SwitchItem("SomeSwitch"));
        InOrder inOrder = inOrder(eventPublisher);
        inOrder.verify(eventPublisher).post(any());

        SwitchItem item = new SwitchItem("SomeSwitch");
        item.addTag(OTHER_TAG);
        itemRegistry.update(item);

        ArgumentCaptor<ItemUpdatedEvent> captor = ArgumentCaptor.forClass(ItemUpdatedEvent.class);
        inOrder.verify(eventPublisher).post(captor.capture());
        assertTrue(captor.getValue().getItem().tags.contains(OTHER_TAG));
    }

    @Test
    public void testItemRemovedEvent() {
        SwitchItem item = new SwitchItem("SomeSwitch");
        item.addTag(OTHER_TAG);
        itemRegistry.add(item);

        InOrder inOrder = inOrder(eventPublisher);
        inOrder.verify(eventPublisher).post(any());

        itemRegistry.remove("SomeSwitch");

        ArgumentCaptor<ItemRemovedEvent> captor = ArgumentCaptor.forClass(ItemRemovedEvent.class);
        inOrder.verify(eventPublisher).post(captor.capture());
        assertTrue(captor.getValue().getItem().tags.contains(OTHER_TAG));
    }

    @Test
    public void assertThatAChangedItemStillHasAnEventPublisher() {
        // add new item
        GenericItem item = new SwitchItem("SomeSwitch");
        assertThat(item.eventPublisher, is(nullValue()));
        itemProvider.add(item);
        assertThat(item.eventPublisher, is(notNullValue()));

        // update item
        GenericItem oldItem = item;
        GenericItem newItem = new SwitchItem("SomeSwitch");
        assertThat(oldItem.eventPublisher, is(notNullValue()));
        assertThat(newItem.eventPublisher, is(nullValue()));
        itemProvider.update(newItem);
        assertThat(oldItem.eventPublisher, is(nullValue()));
        assertThat(newItem.eventPublisher, is(notNullValue()));

        // remove item
        assertThat(newItem.eventPublisher, is(notNullValue()));
        itemProvider.remove(newItem.getUID());
        assertThat(newItem.eventPublisher, is(nullValue()));
    }

    @Test
    public void assertItemIsBeingDisposedOnRemove() {
        GenericItem item = spy(new SwitchItem("Item1"));
        itemProvider.add(item);

        @SuppressWarnings("unchecked")
        RegistryChangeListener<Item> registryChangeListener = mock(RegistryChangeListener.class);
        itemRegistry.addRegistryChangeListener(registryChangeListener);

        itemProvider.remove(item.getUID());

        verify(item).dispose();

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(registryChangeListener).removed(itemCaptor.capture());
        assertTrue(itemCaptor.getValue() == item);
    }

    @Test
    public void assertOldItemIsBeingDisposedOnUpdate() {
        GenericItem item = new SwitchItem("Item1");
        itemProvider.add(item);

        assertNotNull(item.eventPublisher);
        assertNotNull(item.itemStateConverter);
        assertNotNull(item.unitProvider);

        itemProvider.update(new SwitchItem("Item1"));

        assertNull(item.eventPublisher);
        assertNull(item.itemStateConverter);
        assertNull(item.unitProvider);
        assertEquals(0, item.listeners.size());
    }

}
