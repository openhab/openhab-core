/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.model.item.internal;

import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.events.AbstractItemRegistryEvent;
import org.openhab.core.items.events.ItemAddedEvent;
import org.openhab.core.items.events.ItemRemovedEvent;
import org.openhab.core.items.events.ItemUpdatedEvent;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.ArithmeticGroupFunction;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.model.core.EventType;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alex Tugarev - Initial contribution
 * @author Andre Fuechsel - Added some tests
 * @author Michael Grammling - Added some tests
 * @author Simon Kaufmann - Added some tests
 * @author Stefan Triller - Added test for ItemAddedEvents with multiple model files
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class GenericItemProviderTest extends JavaOSGiTest {

    private static final String ITEMS_MODEL_TYPE = "items";
    private static final String TESTMODEL_NAME = "testModel.items";
    private static final String TESTMODEL_NAME2 = "testModel2.items";

    private static final Collection<String> TESTMODEL_NAMES = Stream.of(TESTMODEL_NAME, TESTMODEL_NAME2)
            .collect(Collectors.toList());

    private final Logger logger = LoggerFactory.getLogger(GenericItemProviderTest.class);

    private ItemRegistry itemRegistry;
    private MetadataRegistry metadataRegistry;
    private ModelRepository modelRepository;

    @Before
    public void setUp() {
        itemRegistry = getService(ItemRegistry.class);
        assertThat(itemRegistry, is(notNullValue()));
        assertThat(itemRegistry.getAll(), hasSize(0));

        metadataRegistry = getService(MetadataRegistry.class);
        assertThat(metadataRegistry, is(notNullValue()));
        assertThat(metadataRegistry.getAll(), hasSize(0));

        modelRepository = getService(ModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));
        assertThat(modelRepository.getAllModelNamesOfType(ITEMS_MODEL_TYPE).iterator().hasNext(), is(false));
    }

    /**
     * Make sure the models and items are removed and the removal events have been processed.
     */
    @After
    public void tearDown() {
        Collection<Item> itemsToRemove = itemRegistry.getAll();
        List<String> modelNamesToRemove = TESTMODEL_NAMES.stream()
                .filter(name -> modelRepository.getModel(name) != null).collect(Collectors.toList());

        if (!modelNamesToRemove.isEmpty()) {
            Set<String> removedModelNames = new HashSet<>();
            ModelRepositoryChangeListener modelListener = new ModelRepositoryChangeListener() {
                @Override
                public void modelChanged(String modelName, EventType type) {
                    logger.debug("Received event: {} {}", modelName, type);
                    if (type == EventType.REMOVED) {
                        removedModelNames.add(modelName);
                    }
                }
            };

            List<AbstractItemRegistryEvent> removedItemEvents = new ArrayList<>();
            @NonNullByDefault
            EventSubscriber itemEventSubscriber = new EventSubscriber() {
                @Override
                public void receive(Event event) {
                    logger.debug("Received event: {}", event);
                    removedItemEvents.add((AbstractItemRegistryEvent) event);
                }

                @Override
                public Set<String> getSubscribedEventTypes() {
                    return Stream.of(ItemRemovedEvent.TYPE).collect(toSet());
                }

                @Override
                public @Nullable EventFilter getEventFilter() {
                    return null;
                }
            };

            modelRepository.addModelRepositoryChangeListener(modelListener);
            registerService(itemEventSubscriber);

            modelNamesToRemove.forEach(modelRepository::removeModel);

            waitForAssert(() -> {
                assertThat(removedItemEvents, hasSize(itemsToRemove.size()));
                assertThat(removedModelNames, hasSize(modelNamesToRemove.size()));
            });

            modelRepository.removeModelRepositoryChangeListener(modelListener);
            unregisterService(itemEventSubscriber);
        }
    }

    @Test
    public void assertThatItemsFromTestModelWereAddedToItemRegistry() {
        Collection<Item> items = itemRegistry.getAll();
        assertThat(items, hasSize(0));

        String model = "Group Weather [TAG1]\n" + "Group Weather_Chart (Weather)\n"
                + "Number Weather_Temperature      \"Outside Temperature [%.1f °C]\" <temperature> (Weather_Chart) [TAG1, TAG2] { channel=\"yahooweather:weather:berlin:temperature\" }\n"
                + "Number Weather_Temp_Max         \"Todays Maximum [%.1f °C]\"  <temperature> (Weather_Chart)\n"
                + "Number Weather_Temp_Min         \"Todays Minimum [%.1f °C]\"  <temperature> (Weather_Chart)\n"
                + "Number Weather_Chart_Period     \"Chart Period\"\n"
                + "DateTime Weather_LastUpdate     \"Last Update [%1$ta %1$tR]\" <clock> [TAG1, TAG2, TAG3]";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Item> actualItems = itemRegistry.getAll();

        assertThat(actualItems, hasSize(7));
    }

    @Test
    public void assertThatGroupItemWithBaseItemGetsEqualityFunction() {
        Collection<Item> items = itemRegistry.getAll();
        assertThat(items, hasSize(0));

        String model = "Group:Switch Switches1";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Item> actualItems = itemRegistry.getAll();

        assertThat(actualItems, hasSize(1));
        GroupItem groupItem = (GroupItem) actualItems.iterator().next();
        assertThat(groupItem.getFunction(), instanceOf(GroupFunction.Equality.class));
    }

    @Test
    public void assertThatGroupItemWithEqualFunctionGetsEquality() {
        Collection<Item> items = itemRegistry.getAll();
        assertThat(items, hasSize(0));

        String model = "Group:Switch:EQUALITY Switches2";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Item> actualItems = itemRegistry.getAll();

        assertThat(actualItems, hasSize(1));
        GroupItem groupItem = (GroupItem) actualItems.iterator().next();
        assertThat(groupItem.getFunction(), instanceOf(GroupFunction.Equality.class));
    }

    @Test
    public void assertThatItemsHaveTagsIfSpecified() {
        Collection<Item> items = itemRegistry.getAll();
        assertThat(items, hasSize(0));

        String model = "DateTime Weather_LastUpdate     \"Last Update [%1$ta %1$tR]\" <clock> [TAG1, TAG2, TAG3, TAG4-WITH-DASHES, \"TAG5 String Tag\"]";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Item> actualItems = itemRegistry.getAll();

        assertThat(actualItems, hasSize(1));

        Item lastItem = new LinkedList<>(actualItems).getLast();
        assertThat(lastItem.getTags().stream().sorted().collect(joining(", ")),
                is(equalTo("TAG1, TAG2, TAG3, TAG4-WITH-DASHES, TAG5 String Tag")));
    }

    @Test
    public void assertThatAbrokenModelIsIgnored() {
        assertThat(itemRegistry.getAll(), hasSize(0));

        String model = "String test \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String {something is wrong} test \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        assertThat(itemRegistry.getAll(), hasSize(0));
    }

    @Test
    public void assertThatItemsAreRemovedCorrectlyIfTheModelGetsBroken() {
        assertThat(itemRegistry.getAll(), hasSize(0));

        String model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        assertThat(itemRegistry.getAll(), hasSize(2));

        model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String {something is wrong} test \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll(), hasSize(0));
        });

        model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        assertThat(itemRegistry.getAll(), hasSize(2));
    }

    @Test
    public void assertThatItemEventsAreSentCorrectly() {
        List<AbstractItemRegistryEvent> receivedEvents = new ArrayList<>();

        @NonNullByDefault
        EventSubscriber itemEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                logger.debug("Received event: {}", event);
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

        assertThat(itemRegistry.getAll(), hasSize(0));

        receivedEvents.clear();
        String model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll(), hasSize(2));
            assertThat(receivedEvents, hasSize(2));
            assertThat(receivedEvents.stream().filter(e -> "test1".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
            assertThat(receivedEvents.stream().filter(e -> "test2".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
        });

        receivedEvents.clear();
        model = "String test1 \"Test Item Changed [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll(), hasSize(1));
            assertThat(receivedEvents, hasSize(2));
            assertThat(receivedEvents.stream().filter(e -> "test1".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemUpdatedEvent.class));
            assertThat(receivedEvents.stream().filter(e -> "test2".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemRemovedEvent.class));
        });

        receivedEvents.clear();
        model = "";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll(), hasSize(0));
            assertThat(receivedEvents, hasSize(1));
            assertThat(receivedEvents.stream().filter(e -> "test1".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemRemovedEvent.class));
        });

        unregisterService(itemEventSubscriber);
    }

    @Test
    public void assertThatItemEventsAreSentOnlyOncePerItemEvenWithMultipleItemFiles() {
        List<AbstractItemRegistryEvent> receivedEvents = new ArrayList<>();

        @NonNullByDefault
        EventSubscriber itemEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                logger.debug("Received event: {}", event);
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

        assertThat(itemRegistry.getAll(), hasSize(0));

        receivedEvents.clear();
        String model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        waitForAssert(() -> {
            assertThat(itemRegistry.getAll(), hasSize(2));
            assertThat(receivedEvents, hasSize(2));
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
            assertThat(itemRegistry.getAll(), hasSize(4));
            assertThat(receivedEvents, hasSize(2));
            assertThat(receivedEvents.stream().filter(e -> "test3".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
            assertThat(receivedEvents.stream().filter(e -> "test4".equals(e.getItem().name)).findFirst().get(),
                    instanceOf(ItemAddedEvent.class));
        });

        unregisterService(itemEventSubscriber);
    }

    @Test
    public void assertThatTheItemRegistryGetsTheSameInstanceOnItemUpdatesWithoutChanges() throws Exception {
        assertThat(itemRegistry.getAll(), hasSize(0));

        String model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test3 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        assertThat(itemRegistry.getAll(), hasSize(3));
        Item unchangedItem = itemRegistry.getItem("test1");

        model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }\n"
                + "String test2 \"Test Item Changed [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        assertThat(itemRegistry.getAll(), hasSize(2));
        assertThat(itemRegistry.getItem("test1"), is(unchangedItem));

        model = "";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        assertThat(itemRegistry.getAll(), hasSize(0));

        model = "String test1 \"Test Item [%s]\" { channel=\"test:test:test:test\" }";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        assertThat(itemRegistry.getAll(), hasSize(1));
        assertThat(itemRegistry.getItem("test1"), is(unchangedItem));
    }

    @Test
    public void testStableOrder() {
        String model = "Group testGroup " + //
                "Number number1 (testGroup) " + //
                "Number number2 (testGroup) " + //
                "Number number3 (testGroup) " + //
                "Number number4 (testGroup) " + //
                "Number number5 (testGroup) " + //
                "Number number6 (testGroup) " + //
                "Number number7 (testGroup) " + //
                "Number number8 (testGroup) " + //
                "Number number9 (testGroup) ";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        GroupItem groupItem = (GroupItem) itemRegistry.get("testGroup");
        assertThat(groupItem, is(notNullValue()));

        int number = 0;
        Iterator<Item> it = groupItem.getMembers().iterator();
        while (it.hasNext()) {
            Item item = it.next();
            assertThat(item.getName(), is("number" + (++number)));
        }
    }

    @Test
    public void testStableReloadOrder() {
        String model = "Group testGroup " + //
                "Number number1 (testGroup) " + //
                "Number number2 (testGroup) " + //
                "Number number3 (testGroup) " + //
                "Number number4 (testGroup) " + //
                "Number number5 (testGroup) " + //
                "Number number6 (testGroup) " + //
                "Number number7 (testGroup) " + //
                "Number number8 (testGroup) " + //
                "Number number9 (testGroup) ";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        assertThat(itemRegistry.getAll(), hasSize(10));

        model = "Group testGroup " + //
                "Number number1 (testGroup) " + //
                "Number number2 (testGroup) " + //
                "Number number3 (testGroup) " + //
                "Number number4 (testGroup) " + //
                "Number number5 (testGroup) " + //
                "Number number6 (testGroup) " + //
                "Number number7 \"Number Seven\" (testGroup) " + //
                "Number number8 (testGroup) " + //
                "Number number9 (testGroup) ";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        GroupItem groupItem = (GroupItem) itemRegistry.get("testGroup");
        assertThat(groupItem, is(notNullValue()));

        int number = 0;
        Iterator<Item> it = groupItem.getMembers().iterator();
        while (it.hasNext()) {
            Item item = it.next();
            assertThat(item.getName(), is("number" + (++number)));
            if (number == 7) {
                assertThat(item.getLabel(), is("Number Seven"));
            }
        }
    }

    @Test
    public void testGroupAssignmentsAreConsidered() {
        String model = "Group testGroup " + //
                "Number number1 (testGroup) " + //
                "Number number2 ";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        model = "Group testGroup " + //
                "Number number1 (testGroup) " + //
                "Number number2 (testGroup)";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        GenericItem item = (GenericItem) itemRegistry.get("number2");
        assertThat(item, is(notNullValue()));
        assertThat(item.getGroupNames(), hasItem("testGroup"));
        GroupItem groupItem = (GroupItem) itemRegistry.get("testGroup");
        assertThat(groupItem, is(notNullValue()));
        assertThat(groupItem.getAllMembers(), hasItem(item));
    }

    @Test
    public void testGroupItemIsSame() {
        GenericItemProvider gip = (GenericItemProvider) getService(ItemProvider.class);
        assertThat(gip, is(notNullValue()));

        GroupItem g1 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        GroupItem g2 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));

        assertThat(gip.hasItemChanged(g1, g2), is(false));
    }

    @Test
    public void testGroupItemChangesBaseItem() {
        GenericItemProvider gip = (GenericItemProvider) getService(ItemProvider.class);
        assertThat(gip, is(notNullValue()));

        GroupItem g1 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        GroupItem g2 = new GroupItem("testGroup", new NumberItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));

        assertThat(gip.hasItemChanged(g1, g2), is(true));
    }

    @Test
    public void testGroupItemChangesFunctionParameters() {
        GenericItemProvider gip = (GenericItemProvider) getService(ItemProvider.class);
        assertThat(gip, is(notNullValue()));

        GroupItem g1 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        GroupItem g2 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, UnDefType.UNDEF));

        assertThat(gip.hasItemChanged(g1, g2), is(true));
    }

    @Test
    public void testGroupItemChangesBaseItemAndFunction() {
        GenericItemProvider gip = (GenericItemProvider) getService(ItemProvider.class);
        assertThat(gip, is(notNullValue()));

        GroupItem g1 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        GroupItem g2 = new GroupItem("testGroup", new NumberItem("number"), new ArithmeticGroupFunction.Sum());

        assertThat(gip.hasItemChanged(g1, g2), is(true));
    }

    @Test
    public void testMetadataSimple() {
        String model = "Switch simple { namespace=\"value\" } ";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Item item = itemRegistry.get("simple");
        assertThat(item, is(notNullValue()));

        Metadata res = metadataRegistry.get(new MetadataKey("namespace", "simple"));
        assertThat(res, is(notNullValue()));
        assertThat(res.getValue(), is("value"));
        assertThat(res.getConfiguration(), is(notNullValue()));
    }

    @Test
    public void testMetadataConfigured() {
        String model = "Switch simple { namespace=\"value\" } " + //
                "Switch configured { foo=\"bar\" [ answer=42 ] } ";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Item item = itemRegistry.get("configured");
        assertThat(item, is(notNullValue()));

        Metadata res = metadataRegistry.get(new MetadataKey("foo", "configured"));
        assertThat(res, is(notNullValue()));
        assertThat(res.getValue(), is("bar"));
        assertThat(res.getConfiguration().get("answer"), is(new BigDecimal(42)));

        Collection<Item> itemsToRemove = itemRegistry.getAll();
        List<AbstractItemRegistryEvent> removedItemEvents = new ArrayList<>();

        @NonNullByDefault
        EventSubscriber itemEventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                logger.debug("Received event: {}", event);
                removedItemEvents.add((AbstractItemRegistryEvent) event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Stream.of(ItemRemovedEvent.TYPE).collect(toSet());
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        };

        registerService(itemEventSubscriber);

        modelRepository.removeModel(TESTMODEL_NAME);

        waitForAssert(() -> {
            assertThat(removedItemEvents, hasSize(itemsToRemove.size()));
        });

        unregisterService(itemEventSubscriber);

        res = metadataRegistry.get(new MetadataKey("foo", "configured"));
        assertThat(res, is(nullValue()));
    }

    @Test
    public void testMetadataUpdate() {
        modelRepository.addOrRefreshModel(TESTMODEL_NAME,
                new ByteArrayInputStream("Switch s { meta=\"foo\" }".getBytes()));
        Metadata metadata1 = metadataRegistry.get(new MetadataKey("meta", "s"));
        assertThat(metadata1, is(notNullValue()));
        assertThat(metadata1.getValue(), is("foo"));

        modelRepository.addOrRefreshModel(TESTMODEL_NAME,
                new ByteArrayInputStream("Switch s { meta=\"bar\" }".getBytes()));
        Metadata metadata2 = metadataRegistry.get(new MetadataKey("meta", "s"));
        assertThat(metadata2, is(notNullValue()));
        assertThat(metadata2.getValue(), is("bar"));

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream("Switch s".getBytes()));
        Metadata metadata3 = metadataRegistry.get(new MetadataKey("meta", "s"));
        assertThat(metadata3, is(nullValue()));
    }

    @Test
    public void testTagUpdate() {
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream("Switch s [foo]".getBytes()));
        Item item1 = itemRegistry.get("s");
        assertThat(item1, is(notNullValue()));
        assertThat(item1.getTags(), hasSize(1));
        assertThat(item1.getTags(), hasItem("foo"));

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream("Switch s [foo, bar]".getBytes()));
        Item item2 = itemRegistry.get("s");
        assertThat(item2, is(notNullValue()));
        assertThat(item2.getTags(), hasSize(2));
        assertThat(item2.getTags(), hasItem("foo"));
        assertThat(item2.getTags(), hasItem("bar"));

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream("Switch s".getBytes()));
        Item item3 = itemRegistry.get("s");
        assertThat(item3, is(notNullValue()));
        assertThat(item3.getTags(), hasSize(0));
    }

    @Test
    public void testSquareBracketsInFormat() {
        String model = "Switch s \"Info [XPATH(/*[name()='liveStreams']/*[name()='stream']):%s]\"";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Item item = itemRegistry.get("s");
        assertThat(item, is(notNullValue()));
        StateDescription stateDescription = item.getStateDescription();
        assertThat(stateDescription, is(notNullValue()));
        assertThat(stateDescription.getPattern(), is("XPATH(/*[name()='liveStreams']/*[name()='stream']):%s"));
    }

}
