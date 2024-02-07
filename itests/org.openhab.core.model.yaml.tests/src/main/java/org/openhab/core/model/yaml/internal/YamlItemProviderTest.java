/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.ArithmeticGroupFunction;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.test.CountingRegistryListener;
import org.openhab.core.service.WatchService;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * The {@link YamlItemProviderTest} contains tests for the
 * {@link org.openhab.core.model.yaml.internal.item.YamlItemProvider}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class YamlItemProviderTest extends JavaOSGiTest {

    private static final Path MODEL_NAME = Path.of("model-items.yaml");
    private static final Path INITIAL_MODEL = Path.of("items-initial.yaml");
    private static final Path MODIFIED_MODEL = Path.of("items-modified.yaml");

    private @NonNullByDefault({}) Path confPath;
    private @NonNullByDefault({}) WatchService watchService;
    private @NonNullByDefault({}) YamlModelRepository modelRepository;
    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @NonNullByDefault({}) CountingRegistryListener<Item> listener;

    @BeforeEach
    public void beforeEach() throws IOException {
        watchService = getService(WatchService.class);
        assertThat(watchService, is(notNullValue()));
        confPath = watchService.getWatchPath().resolve("yaml");
        Files.createDirectories(confPath);
        modelRepository = getService(YamlModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));
        itemRegistry = getService(ItemRegistry.class);
        assertThat(itemRegistry, is(notNullValue()));

        listener = new CountingRegistryListener<>();
        itemRegistry.addRegistryChangeListener(listener);
    }

    @Test
    void testItemsProcessing() throws IOException {
        Files.copy(bundleContext.getBundle().getResource(INITIAL_MODEL.toString()).openStream(),
                confPath.resolve(MODEL_NAME));

        waitForAssert(() -> {
            assertThat(listener.getAddedCounter(), is(4));
            assertThat(listener.getRemovedCounter(), is(0));
            assertThat(listener.getUpdatedCounter(), is(0));
        });

        // first entry
        Item item1 = itemRegistry.get("TestGroup1");
        assertThat(item1, is(notNullValue()));
        assertThat(item1, is(instanceOf(GroupItem.class)));
        assertThat(item1.getUID(), is("TestGroup1"));
        assertThat(item1.getLabel(), is("Group with Function"));
        assertThat(((GroupItem) item1).getBaseItem(), is(instanceOf(SwitchItem.class)));
        GroupFunction groupFunction = ((GroupItem) item1).getFunction();
        assertThat(groupFunction, is(instanceOf(ArithmeticGroupFunction.Or.class)));
        assertThat(groupFunction.getParameters(), is(arrayContaining(OnOffType.ON, OnOffType.OFF)));

        // second entry
        Item item2 = itemRegistry.get("TestGroup2");
        assertThat(item2, is(notNullValue()));
        assertThat(item2, is(instanceOf(GroupItem.class)));
        assertThat(item2.getUID(), is("TestGroup2"));
        assertThat(item2.getLabel(), is("Simple Group"));

        // third entry
        Item item3 = itemRegistry.get("TestItem1");
        assertThat(item3, is(notNullValue()));
        assertThat(item3, is(instanceOf(SwitchItem.class)));
        assertThat(item3.getUID(), is("TestItem1"));
        assertThat(item3.getGroupNames(), hasItems("TestGroup"));
        assertThat(item3.getTags(), hasItems("AlarmSystem"));

        // fourth entry
        Item item4 = itemRegistry.get("TestItem2");
        assertThat(item4, is(notNullValue()));
        assertThat(item4, is(instanceOf(NumberItem.class)));
        assertThat(item4.getType(), is("Number:Temperature"));
        assertThat(item4.getUID(), is("TestItem2"));
        assertThat(item4.getLabel(), is("Test Label"));
        assertThat(item4.getCategory(), is("temperature"));
        assertThat(item4.getGroupNames(), hasItems("TestGroup2"));

        Files.copy(bundleContext.getBundle().getResource(MODIFIED_MODEL.toString()).openStream(),
                confPath.resolve(MODEL_NAME), StandardCopyOption.REPLACE_EXISTING);

        waitForAssert(() -> {
            assertThat(listener.getAddedCounter(), is(5));
            assertThat(listener.getRemovedCounter(), is(2));
            assertThat(listener.getUpdatedCounter(), is(2));
        });

        // first entry modified
        item1 = itemRegistry.get("TestGroup1");
        assertThat(item1, is(notNullValue()));
        assertThat(item1, is(instanceOf(GroupItem.class)));
        assertThat(item1.getUID(), is("TestGroup1"));
        assertThat(item1.getLabel(), is("Group with Function"));
        assertThat(((GroupItem) item1).getBaseItem(), is(instanceOf(ContactItem.class)));
        groupFunction = ((GroupItem) item1).getFunction();
        assertThat(groupFunction, is(instanceOf(ArithmeticGroupFunction.And.class)));
        assertThat(groupFunction.getParameters(), is(arrayContaining(OpenClosedType.OPEN, OpenClosedType.CLOSED)));

        // second entry removed
        item2 = itemRegistry.get("TestGroup2");
        assertThat(item2, is(nullValue()));

        // third entry unmodified
        item3 = itemRegistry.get("TestItem1");
        assertThat(item3, is(notNullValue()));
        assertThat(item3, is(instanceOf(SwitchItem.class)));
        assertThat(item3.getUID(), is("TestItem1"));
        assertThat(item3.getGroupNames(), hasItems("TestGroup"));
        assertThat(item3.getTags(), hasItems("AlarmSystem"));

        // fourth entry removed
        item4 = itemRegistry.get("TestItem2");
        assertThat(item4, is(nullValue()));

        // fifth entry added
        Item item5 = itemRegistry.get("TestItem3");
        assertThat(item5, is(notNullValue()));
        assertThat(item5, is(instanceOf(ContactItem.class)));

        // remove model
        Files.delete(confPath.resolve(MODEL_NAME));
        waitForAssert(() -> assertThat(itemRegistry.getAll(), is(empty())));
    }
}
