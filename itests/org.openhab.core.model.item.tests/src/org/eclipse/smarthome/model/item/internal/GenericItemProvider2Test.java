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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Iterator;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.ArithmeticGroupFunction;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class GenericItemProvider2Test extends JavaOSGiTest {

    private static final String TESTMODEL_NAME = "testModel.items";
    private static final String TESTMODEL_NAME2 = "testModel2.items";

    private ModelRepository modelRepository;
    private ItemRegistry itemRegistry;
    private MetadataRegistry metadataRegistry;

    @Before
    public void setUp() {
        registerVolatileStorageService();

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        modelRepository = getService(ModelRepository.class);
        assertNotNull(modelRepository);

        metadataRegistry = getService(MetadataRegistry.class);
        assertNotNull(metadataRegistry);

        modelRepository.removeModel(TESTMODEL_NAME);
        modelRepository.removeModel(TESTMODEL_NAME2);

        assertEquals(0, itemRegistry.getAll().size());
    }

    @After
    public void tearDown() {
        modelRepository.removeModel(TESTMODEL_NAME);
        modelRepository.removeModel(TESTMODEL_NAME2);
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
        assertNotNull(groupItem);

        int number = 0;
        Iterator<Item> it = groupItem.getMembers().iterator();
        while (it.hasNext()) {
            Item item = it.next();
            assertEquals("number" + (++number), item.getName());
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
        assertEquals(10, itemRegistry.getAll().size());

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
        assertNotNull(groupItem);

        int number = 0;
        Iterator<Item> it = groupItem.getMembers().iterator();
        while (it.hasNext()) {
            Item item = it.next();
            assertEquals("number" + (++number), item.getName());
            if (number == 7) {
                assertEquals("Number Seven", item.getLabel());
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
        assertTrue(item.getGroupNames().contains("testGroup"));
        GroupItem groupItem = (GroupItem) itemRegistry.get("testGroup");
        assertTrue(groupItem.getAllMembers().contains(item));
    }

    @Test
    public void testGroupItemIsSame() {
        GenericItemProvider gip = new GenericItemProvider();

        GroupItem g1 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        GroupItem g2 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));

        assertFalse(gip.hasItemChanged(g1, g2));
    }

    @Test
    public void testGroupItemChangesBaseItem() {
        GenericItemProvider gip = new GenericItemProvider();

        GroupItem g1 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        GroupItem g2 = new GroupItem("testGroup", new NumberItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));

        assertTrue(gip.hasItemChanged(g1, g2));
    }

    @Test
    public void testGroupItemChangesFunctionParameters() {
        GenericItemProvider gip = new GenericItemProvider();

        GroupItem g1 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        GroupItem g2 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, UnDefType.UNDEF));

        assertTrue(gip.hasItemChanged(g1, g2));
    }

    @Test
    public void testGroupItemChangesBaseItemAndFunction() {
        GenericItemProvider gip = new GenericItemProvider();

        GroupItem g1 = new GroupItem("testGroup", new SwitchItem("test"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        GroupItem g2 = new GroupItem("testGroup", new NumberItem("number"), new ArithmeticGroupFunction.Sum());

        assertTrue(gip.hasItemChanged(g1, g2));
    }

    @Test
    public void testMetadata_simple() {
        String model = "Switch simple { namespace=\"value\" } ";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Item item = itemRegistry.get("simple");
        assertNotNull(item);

        Metadata res = metadataRegistry.get(new MetadataKey("namespace", "simple"));
        assertNotNull(res);
        assertEquals("value", res.getValue());
        assertNotNull(res.getConfiguration());
    }

    @Test
    public void testMetadata_configured() {
        String model = "Switch simple { namespace=\"value\" } " + //
                "Switch configured { foo=\"bar\" [ answer=42 ] } ";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Item item = itemRegistry.get("configured");
        assertNotNull(item);

        Metadata res = metadataRegistry.get(new MetadataKey("foo", "configured"));
        assertNotNull(res);
        assertEquals("bar", res.getValue());
        assertEquals(new BigDecimal(42), res.getConfiguration().get("answer"));

        modelRepository.removeModel(TESTMODEL_NAME);

        res = metadataRegistry.get(new MetadataKey("foo", "configured"));
        assertNull(res);
    }

    @Test
    public void testMetadataUpdate() {
        modelRepository.addOrRefreshModel(TESTMODEL_NAME,
                new ByteArrayInputStream("Switch s { meta=\"foo\" }".getBytes()));
        Metadata metadata1 = metadataRegistry.get(new MetadataKey("meta", "s"));
        assertNotNull(metadata1);
        assertEquals("foo", metadata1.getValue());

        modelRepository.addOrRefreshModel(TESTMODEL_NAME,
                new ByteArrayInputStream("Switch s { meta=\"bar\" }".getBytes()));
        Metadata metadata2 = metadataRegistry.get(new MetadataKey("meta", "s"));
        assertNotNull(metadata2);
        assertEquals("bar", metadata2.getValue());

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream("Switch s".getBytes()));
        Metadata metadata3 = metadataRegistry.get(new MetadataKey("meta", "s"));
        assertNull(metadata3);
    }

    @Test
    public void testTagUpdate() {
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream("Switch s [foo]".getBytes()));
        Item item1 = itemRegistry.get("s");
        assertNotNull(item1);
        assertEquals(1, item1.getTags().size());
        assertTrue(item1.getTags().contains("foo"));

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream("Switch s [foo, bar]".getBytes()));
        Item item2 = itemRegistry.get("s");
        assertNotNull(item2);
        assertEquals(2, item2.getTags().size());
        assertTrue(item2.getTags().contains("foo"));
        assertTrue(item2.getTags().contains("bar"));

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream("Switch s".getBytes()));
        Item item3 = itemRegistry.get("s");
        assertNotNull(item3);
        assertEquals(0, item3.getTags().size());
    }

    @Test
    public void testSquareBracketsInFormat() {
        String model = "Switch s \"Info [XPATH(/*[name()='liveStreams']/*[name()='stream']):%s]\"";
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Item item = itemRegistry.get("s");
        assertNotNull(item);
        assertEquals("XPATH(/*[name()='liveStreams']/*[name()='stream']):%s", item.getStateDescription().getPattern());
    }

}
