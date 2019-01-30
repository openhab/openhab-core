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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.smarthome.core.items.ManagedItemProvider.PersistedItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.ArithmeticGroupFunction.And;
import org.eclipse.smarthome.core.library.types.ArithmeticGroupFunction.Avg;
import org.eclipse.smarthome.core.library.types.ArithmeticGroupFunction.Sum;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.storage.Storage;
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The {@link ManagedItemProviderOSGiTest} runs inside an
 * OSGi container and tests the {@link ManagedItemProvider}.
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Kai Kreuzer - added tests for repeated addition and removal
 * @author Andre Fuechsel - added tests for tags
 * @author Simon Kaufmann - added test for late registration of item factory
 */
public class ManagedItemProviderOSGiTest extends JavaOSGiTest {

    private ManagedItemProvider itemProvider;
    private ItemRegistry itemRegistry;

    @Before
    public void setUp() {
        registerVolatileStorageService();
        itemProvider = getService(ManagedItemProvider.class);
        itemRegistry = getService(ItemRegistry.class);
    }

    @After
    public void tearDown() {
        for (Item item : itemProvider.getAll()) {
            itemProvider.remove(item.getName());
        }

        unregisterService(itemProvider);
    }

    private static class StrangeItem extends GenericItem {
        static final String STRANGE_TEST_TYPE = "StrangeTestType";

        public StrangeItem(String name) {
            super(STRANGE_TEST_TYPE, name);
        }

        @Override
        public List<Class<? extends State>> getAcceptedDataTypes() {
            return Collections.emptyList();
        }

        @Override
        public List<Class<? extends Command>> getAcceptedCommandTypes() {
            return Collections.emptyList();
        }
    }

    private static class StrangeItemFactory implements ItemFactory {
        @Override
        public GenericItem createItem(String itemTypeName, String itemName) {
            return new StrangeItem(itemName);
        }

        @Override
        public String[] getSupportedItemTypes() {
            return new String[] { StrangeItem.STRANGE_TEST_TYPE };
        }
    }

    @Test
    public void assertGetItemsReturnsItemFromRegisteredManagedItemProvider() {
        assertThat(itemProvider.getAll().size(), is(0));

        itemProvider.add(new SwitchItem("SwitchItem"));
        itemProvider.add(new StringItem("StringItem"));

        Collection<Item> items = itemProvider.getAll();
        assertThat(items.size(), is(2));

        itemProvider.remove("StringItem");
        itemProvider.remove("SwitchItem");

        assertThat(itemProvider.getAll().size(), is(0));
    }

    @Test
    public void updatingExistingItemReturnsOldValue() {
        assertThat(itemProvider.getAll().size(), is(0));

        itemProvider.add(new StringItem("Item"));
        Item result = itemProvider.update(new SwitchItem("Item"));

        assertThat(result.getType(), is("String"));

        itemProvider.remove("Item");

        assertThat(itemProvider.getAll().size(), is(0));
    }

    @Test
    public void assertRemovalReturnsOldValue() {
        assertThat(itemProvider.getAll().size(), is(0));

        itemProvider.add(new StringItem("Item"));
        Item result = itemProvider.remove("Unknown");

        assertNull(result);

        result = itemProvider.remove("Item");

        assertThat(result.getName(), is("Item"));

        assertThat(itemProvider.getAll().size(), is(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void assertTwoItemsWithSameNameCanNotBeAdded() {
        assertThat(itemProvider.getAll().size(), is(0));

        itemProvider.add(new StringItem("Item"));
        itemProvider.add(new StringItem("Item"));
    }

    @Test
    public void assertTagsAreStoredAndRetrievedAsWell() {
        assertThat(itemProvider.getAll().size(), is(0));

        SwitchItem item1 = new SwitchItem("SwitchItem1");
        SwitchItem item2 = new SwitchItem("SwitchItem2");
        item1.addTag("tag1");
        item1.addTag("tag2");
        item2.addTag("tag3");

        itemProvider.add(item1);
        itemProvider.add(item2);

        Collection<Item> items = itemProvider.getAll();
        assertThat(items.size(), is(2));

        Item result1 = itemProvider.remove("SwitchItem1");
        Item result2 = itemProvider.remove("SwitchItem2");

        assertThat(result1.getName(), is("SwitchItem1"));
        assertThat(result1.getTags().size(), is(2));
        assertThat(result1.hasTag("tag1"), is(true));
        assertThat(result1.hasTag("tag2"), is(true));
        assertThat(result1.hasTag("tag3"), is(false));

        assertThat(result2.getName(), is("SwitchItem2"));
        assertThat(result2.getTags().size(), is(1));
        assertThat(result2.hasTag("tag1"), is(false));
        assertThat(result2.hasTag("tag2"), is(false));
        assertThat(result2.hasTag("tag3"), is(true));

        assertThat(itemProvider.getAll().size(), is(0));
    }

    @Test
    public void assertRemoveRecursivelyWorks() {
        assertThat(itemProvider.getAll().size(), is(0));

        GroupItem group = new GroupItem("group");

        GenericItem item1 = new SwitchItem("SwitchItem1");
        item1.addGroupName(group.getName());
        GenericItem item2 = new SwitchItem("SwitchItem2");
        item2.addGroupName(group.getName());

        itemProvider.add(group);
        itemProvider.add(item1);
        itemProvider.add(item2);

        assertThat(itemProvider.getAll().size(), is(3));

        Item oldItem = itemProvider.remove(group.getName(), true);

        assertThat(oldItem, is(group));
        assertThat(itemProvider.getAll().size(), is(0));
    }

    @Test
    public void assertItemsAreThereOnceTheFactoryGetsAdded() throws ItemNotFoundException {
        StorageService storageService = getService(StorageService.class);
        assertThat(storageService, is(notNullValue()));

        Storage<PersistedItem> storage = storageService.getStorage(Item.class.getName());
        StrangeItem item = new StrangeItem("SomeStrangeItem");
        String key = item.getUID();

        // put an item into the storage that cannot be handled (yet)
        storage.put(key, itemProvider.toPersistableElement(item));

        // start without the appropriate item factory - it's going to fail silently, leaving a debug log
        assertThat(itemProvider.getAll().size(), is(0));
        assertThat(itemRegistry.getItems().size(), is(0));
        assertThat(itemProvider.get("SomeStrangeItem"), is(nullValue()));
        try {
            assertThat(itemRegistry.getItem("SomeStrangeItem"), is(nullValue()));
            fail("the item is not (yet) expected to be there");
        } catch (ItemNotFoundException e) {
            // all good
        }

        // now register the item factory. The item should be there...
        StrangeItemFactory factory = new StrangeItemFactory();
        registerService(factory);
        try {
            assertThat(itemProvider.getAll().size(), is(1));
            assertThat(itemRegistry.getItems().size(), is(1));
            assertThat(itemProvider.get("SomeStrangeItem"), is(notNullValue()));
            assertThat(itemRegistry.getItem("SomeStrangeItem"), is(notNullValue()));
        } finally {
            unregisterService(factory);
        }
    }

    @SuppressWarnings("null")
    @Test
    public void assertItemsAreAddedAsGroupMembersOnDeferredCreation() throws ItemNotFoundException {
        StorageService storageService = getService(StorageService.class);
        assertThat(storageService, is(notNullValue()));

        Storage<PersistedItem> storage = storageService.getStorage(Item.class.getName());
        StrangeItem item = new StrangeItem("SomeStrangeItem");
        GroupItem groupItem = new GroupItem("SomeGroupItem");
        item.addGroupName(groupItem.getName());
        groupItem.addMember(item);
        String itemKey = item.getUID();

        // put items into the storage that cannot be handled (yet)
        storage.put(itemKey, itemProvider.toPersistableElement(item));
        itemProvider.add(groupItem);

        // start without the appropriate item factory - it only creates the group item

        assertThat(itemProvider.getAll().size(), is(1));
        assertThat(itemRegistry.getItems().size(), is(1));
        assertThat(itemProvider.get("SomeStrangeItem"), is(nullValue()));
        assertThat(itemProvider.get("SomeGroupItem"), is(notNullValue()));
        assertThat(itemRegistry.getItem("SomeGroupItem"), is(notNullValue()));
        try {
            assertThat(itemRegistry.getItem("SomeStrangeItem"), is(nullValue()));
            fail("the item is not (yet) expected to be there");
        } catch (ItemNotFoundException e) {
            // all good
        }

        // now register the item factory. Both items should be there...
        StrangeItemFactory factory = new StrangeItemFactory();
        registerService(factory);
        try {
            assertThat(itemProvider.getAll().size(), is(2));
            assertThat(itemRegistry.getItems().size(), is(2));
            Item item1 = itemRegistry.get("SomeStrangeItem");
            Item item2 = itemRegistry.get("SomeGroupItem");
            assertThat(itemRegistry.get("SomeStrangeItem"), is(notNullValue()));
            assertThat(item1, is(notNullValue()));
            assertThat(item1.getGroupNames().size(), is(1));
            assertThat(((GroupItem) item2).getMembers().size(), is(1));
        } finally {
            unregisterService(factory);
        }
    }

    @SuppressWarnings("null")
    @Test
    public void assertFunctionsAreStoredAndRetrievedAsWell() {
        assertThat(itemProvider.getAll().size(), is(0));

        GroupItem item1 = new GroupItem("GroupItem", new NumberItem("Number"), new Avg());
        itemProvider.add(item1);

        Collection<Item> items = itemProvider.getAll();
        assertThat(items.size(), is(1));

        GroupItem result1 = (GroupItem) itemProvider.remove("GroupItem");

        assertThat(result1.getName(), is("GroupItem"));
        assertEquals(result1.getFunction().getClass(), Avg.class);

        assertThat(itemProvider.getAll().size(), is(0));
    }

    @SuppressWarnings("null")
    @Test
    public void assertGroupFunctionsAreStoredAndRetrievedAsWell() {
        assertThat(itemProvider.getAll().size(), is(0));

        GroupFunction function1 = new And(OnOffType.ON, OnOffType.OFF);
        GroupFunction function2 = new Sum();
        GroupItem item1 = new GroupItem("GroupItem1", new SwitchItem("Switch"), function1);
        GroupItem item2 = new GroupItem("GroupItem2", new NumberItem("Number"), function2);

        assertThat(item1.getName(), is("GroupItem1"));
        assertEquals(item1.getFunction().getClass(), And.class);
        assertThat(item1.getFunction().getParameters(), is(new State[] { OnOffType.ON, OnOffType.OFF }));

        assertThat(item2.name, is("GroupItem2"));
        assertEquals(item2.getFunction().getClass(), Sum.class);
        assertThat(item2.getFunction().getParameters(), is(new State[0]));

        itemProvider.add(item1);
        itemProvider.add(item2);

        Collection<Item> items = itemProvider.getAll();
        assertThat(items.size(), is(2));

        GroupItem result1 = (GroupItem) itemProvider.remove("GroupItem1");
        GroupItem result2 = (GroupItem) itemProvider.remove("GroupItem2");

        assertThat(result1.getName(), is("GroupItem1"));
        assertEquals(result1.getFunction().getClass(), And.class);
        assertThat(result1.function.getParameters(), is(new State[] { OnOffType.ON, OnOffType.OFF }));

        assertThat(result2.getName(), is("GroupItem2"));
        assertEquals(result2.getFunction().getClass(), Sum.class);
        assertThat(result2.function.getParameters(), is(new State[0]));

        assertThat(itemProvider.getAll().size(), is(0));
    }
}
