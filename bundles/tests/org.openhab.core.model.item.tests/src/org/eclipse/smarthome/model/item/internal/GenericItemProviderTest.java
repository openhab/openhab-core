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
package org.eclipse.smarthome.model.item.internal;

import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.GroupFunction;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.AbstractItemRegistryEvent;
import org.eclipse.smarthome.core.items.events.ItemAddedEvent;
import org.eclipse.smarthome.core.items.events.ItemRemovedEvent;
import org.eclipse.smarthome.core.items.events.ItemUpdatedEvent;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Alex Tugarev - Initial Contribution
 * @author Andre Fuechsel
 * @author Michael Grammling
 * @author Simon Kaufmann
 * @author Stefan Triller - Added test for ItemAddedEvents with multiple model files
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class GenericItemProviderTest extends JavaOSGiTest {

    private final static String TESTMODEL_NAME = "testModel.items";
    private final static String TESTMODEL_NAME2 = "testModel2.items";

    private ModelRepository modelRepository;
    private ItemRegistry itemRegistry;

    @Before
    public void setUp() {
        itemRegistry = getService(ItemRegistry.class);
        assertThat(itemRegistry, is(notNullValue()));
        modelRepository = getService(ModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));
        modelRepository.removeModel(TESTMODEL_NAME);
        modelRepository.removeModel(TESTMODEL_NAME2);
    }

    @After
    public void tearDown() {
        modelRepository.removeModel(TESTMODEL_NAME);
        modelRepository.removeModel(TESTMODEL_NAME2);
    }

    @Test
    public void assertThatItemsFromTestModelWereAddedToItemRegistry() {
        Collection<Item> items = itemRegistry.getAll();
        assertThat(items.size(), is(0));

        String model = "Group Weather [TAG1]\n" + "Group Weather_Chart (Weather)\n"
                + "Number Weather_Temperature      \"Outside Temperature [%.1f °C]\" <temperature> (Weather_Chart) [TAG1, TAG2] { channel=\"yahooweather:weather:berlin:temperature\" }\n"
                + "Number Weather_Temp_Max         \"Todays Maximum [%.1f °C]\"  <temperature> (Weather_Chart)\n"
                + "Number Weather_Temp_Min         \"Todays Minimum [%.1f °C]\"  <temperature> (Weather_Chart)\n"
                + "Number Weather_Chart_Period     \"Chart Period\"\n"
                + "DateTime Weather_LastUpdate     \"Last Update [%1$ta %1$tR]\" <clock> [TAG1, TAG2, TAG3]";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Item> actualItems = itemRegistry.getAll();

        assertThat(actualItems.size(), is(7));
    }

    @Test
    public void assertThatGroupItemWithBaseItemGetsEqualityFunction() {
        Collection<Item> items = itemRegistry.getAll();
        assertThat(items.size(), is(0));

        String model = "Group:Switch Switches";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Item> actualItems = itemRegistry.getAll();

        assertThat(actualItems.size(), is(1));
        GroupItem groupItem = (GroupItem) actualItems.iterator().next();
        assertThat(groupItem.getFunction(), instanceOf(GroupFunction.Equality.class));
    }

    @Test
    public void assertThatGroupItemWithEqualFunctionGetsEquality() {
        Collection<Item> items = itemRegistry.getAll();
        assertThat(items.size(), is(0));

        String model = "Group:Switch:EQUALITY Switches";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Item> actualItems = itemRegistry.getAll();

        assertThat(actualItems.size(), is(1));
        GroupItem groupItem = (GroupItem) actualItems.iterator().next();
        assertThat(groupItem.getFunction(), instanceOf(GroupFunction.Equality.class));
    }

    @Test
    public void assertThatItemsHaveTagsIfSpecified() {
        Collection<Item> items = itemRegistry.getAll();
        assertThat(items.size(), is(0));

        String model = "DateTime Weather_LastUpdate     \"Last Update [%1$ta %1$tR]\" <clock> [TAG1, TAG2, TAG3, TAG4-WITH-DASHES, \"TAG5 String Tag\"]";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Item> actualItems = itemRegistry.getAll();

        assertThat(actualItems.size(), is(1));

        Item lastItem = new LinkedList<>(actualItems).getLast();
        assertThat(lastItem.getTags().stream().sorted().collect(joining(", ")),
                is(equalTo("TAG1, TAG2, TAG3, TAG4-WITH-DASHES, TAG5 String Tag")));
    }

    @Test
    public void assertThatAbrokenModelIsIgnored() {
        assertThat(itemRegistry.getAll().size(), is(0));

        String model = "String test \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String {something is wrong} test \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        assertThat(itemRegistry.getAll().size(), is(0));
    }

    @Test
    public void assertThatItemsAreRemovedCorrectlyIfTheModelGetsBroken() {
        assertThat(itemRegistry.getAll().size(), is(0));

        String model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        assertThat(itemRegistry.getAll().size(), is(2));

        model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String {something is wrong} test \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll().size(), is(0));
        });

        model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        assertThat(itemRegistry.getAll().size(), is(2));
    }

    @Test
    public void assertThatItemEventsAreSentCorrectly() {
        List<AbstractItemRegistryEvent> receivedEvents = new ArrayList<>();

        @NonNullByDefault
        EventSubscriber itemEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvents.add((AbstractItemRegistryEvent) event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Stream.of(ItemAddedEvent.TYPE, ItemUpdatedEvent.TYPE, ItemRemovedEvent.TYPE).collect(toSet());
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        };

        registerService(itemEventSubscriber);

        assertThat(itemRegistry.getAll().size(), is(0));

        receivedEvents.clear();
        String model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll().size(), is(2));
            assertThat(receivedEvents.size(), is(2));
            assertThat(receivedEvents.stream().filter(e -> "test1".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
            assertThat(receivedEvents.stream().filter(e -> "test2".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
        });

        receivedEvents.clear();
        model = "String test1 \"Test Item Changed [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll().size(), is(1));
            assertThat(receivedEvents.size(), is(2));
            assertThat(receivedEvents.stream().filter(e -> "test1".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemUpdatedEvent.class));
            assertThat(receivedEvents.stream().filter(e -> "test2".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemRemovedEvent.class));
        });

        receivedEvents.clear();
        model = "";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll().size(), is(0));
            assertThat(receivedEvents.size(), is(1));
            assertThat(receivedEvents.stream().filter(e -> "test1".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemRemovedEvent.class));
        });
    }

    @Test
    public void assertThatItemEventsAreSentOnlyOncePerItemEvenWithMultipleItemFiles() {
        List<AbstractItemRegistryEvent> receivedEvents = new ArrayList<>();

        @NonNullByDefault
        EventSubscriber itemEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvents.add((AbstractItemRegistryEvent) event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Stream.of(ItemAddedEvent.TYPE, ItemUpdatedEvent.TYPE, ItemRemovedEvent.TYPE).collect(toSet());
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        };

        registerService(itemEventSubscriber);

        assertThat(itemRegistry.getAll().size(), is(0));

        receivedEvents.clear();
        String model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll().size(), is(2));
            assertThat(receivedEvents.size(), is(2));
            assertThat(receivedEvents.stream().filter(e -> "test1".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
            assertThat(receivedEvents.stream().filter(e -> "test2".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
        });

        receivedEvents.clear();

        model = "String test3 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test4 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME2, new ByteArrayInputStream(model.getBytes()));

        // only ItemAddedEvents for items test3 and test4 should be fired, NOT for test1 and test2 again
        waitForAssert(() -> {
            assertThat(itemRegistry.getAll().size(), is(4));
            assertThat(receivedEvents.size(), is(2));
            assertThat(receivedEvents.stream().filter(e -> "test3".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
            assertThat(receivedEvents.stream().filter(e -> "test4".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
        });
    }

    @Test
    public void assertThatTheItemRegistryGetsTheSameInstanceOnItemUpdatesWithoutChanges() throws Exception {
        assertThat(itemRegistry.getAll().size(), is(0));

        String model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test3 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        assertThat(itemRegistry.getAll().size(), is(3));
        Item unchangedItem = itemRegistry.getItem("test1");

        model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item Changed [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        assertThat(itemRegistry.getAll().size(), is(2));
        assertThat(itemRegistry.getItem("test1"), is(unchangedItem));

        model = "";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        assertThat(itemRegistry.getAll().size(), is(0));

        model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        assertThat(itemRegistry.getAll().size(), is(1));
        assertThat(itemRegistry.getItem("test1"), is(unchangedItem));
    }

}
