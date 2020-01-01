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
package org.openhab.core.model.thing.test.hue;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkProvider;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;

/**
 * @author Henning Treu - Initial contribution
 */
public class GenericItemChannelLinkProviderTest extends JavaOSGiTest {

    private static final String THINGS_TESTMODEL_NAME = "test.things";
    private static final String ITEMS_TESTMODEL_NAME = "test.items";

    private ModelRepository modelRepository;
    private ThingRegistry thingRegistry;
    private ItemRegistry itemRegistry;
    private ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ItemChannelLinkProvider itemChannelLinkProvider;

    @Before
    public void setUp() {
        registerVolatileStorageService();

        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));
        modelRepository = getService(ModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));
        itemRegistry = getService(ItemRegistry.class);
        assertThat(itemRegistry, is(notNullValue()));
        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        assertThat(itemChannelLinkRegistry, is(notNullValue()));
        itemChannelLinkProvider = getService(ItemChannelLinkProvider.class);
        assertThat(itemChannelLinkProvider, is(notNullValue()));
        modelRepository.removeModel(THINGS_TESTMODEL_NAME);
        modelRepository.removeModel(ITEMS_TESTMODEL_NAME);
    }

    @After
    public void tearDown() {
        modelRepository.removeModel(THINGS_TESTMODEL_NAME);
        modelRepository.removeModel(ITEMS_TESTMODEL_NAME);
    }

    @Test
    public void testThatAnItemChannelLinkWasCreatedForAthingAndAnItem() {
        Collection<Thing> things = thingRegistry.getAll();
        assertThat(things.size(), is(0));

        String thingsModel = "Bridge hue:bridge:huebridge [ ipAddress = \"192.168.3.84\", userName = \"19fc3fa6fc870a4280a55f21315631f\" ] {"
                + //
                "    LCT001 bulb3 [ lightId = \"3\" ]" + //
                "    LCT001 bulb4 [ lightId = \"3\" ]" + //
                "}";

        modelRepository.addOrRefreshModel(THINGS_TESTMODEL_NAME, new ByteArrayInputStream(thingsModel.getBytes()));
        Collection<Thing> actualThings = thingRegistry.getAll();

        assertThat(actualThings.size(), is(3));

        Collection<Item> items = itemRegistry.getItems();
        assertThat(items.size(), is(0));

        Collection<ItemChannelLink> itemChannelLinks = itemChannelLinkRegistry.getAll();
        assertThat(itemChannelLinks.size(), is(0));

        String itemsModel = "Color Light3Color \"Light3 Color\" { channel=\"hue:LCT001:huebridge:bulb3:color\" }";

        modelRepository.addOrRefreshModel(ITEMS_TESTMODEL_NAME, new ByteArrayInputStream(itemsModel.getBytes()));
        Collection<Item> actualItems = itemRegistry.getItems();

        assertThat(actualItems.size(), is(1));

        List<ItemChannelLink> actualItemChannelLinks = new ArrayList<>(itemChannelLinkRegistry.getAll());
        assertThat(actualItemChannelLinks.size(), is(1));
        assertThat(actualItemChannelLinks.get(0).toString(),
                is(equalTo("Light3Color -> hue:LCT001:huebridge:bulb3:color")));
    }

    @Test
    public void testThatMultipleLinksAreCreated() {
        Collection<Thing> things = thingRegistry.getAll();
        assertThat(things.size(), is(0));

        String thingsModel = "Bridge hue:bridge:huebridge [ ipAddress = \"192.168.3.84\", userName = \"19fc3fa6fc870a4280a55f21315631f\" ] {"
                + //
                "    LCT001 bulb3 [ lightId = \"3\" ]" + //
                "    LCT001 bulb4 [ lightId = \"3\" ]" + //
                "}";

        modelRepository.addOrRefreshModel(THINGS_TESTMODEL_NAME, new ByteArrayInputStream(thingsModel.getBytes()));
        Collection<Thing> actualThings = thingRegistry.getAll();

        assertThat(actualThings.size(), is(3));

        Collection<Item> items = itemRegistry.getItems();
        assertThat(items.size(), is(0));

        Collection<ItemChannelLink> itemChannelLinks = itemChannelLinkRegistry.getAll();
        assertThat(itemChannelLinks.size(), is(0));

        String itemsModel = "Color Light3Color \"Light3 Color\" { channel=\"hue:LCT001:huebridge:bulb3:color, hue:LCT001:huebridge:bulb4:color\" }";

        modelRepository.addOrRefreshModel(ITEMS_TESTMODEL_NAME, new ByteArrayInputStream(itemsModel.getBytes()));
        Collection<Item> actualItems = itemRegistry.getItems();

        assertThat(actualItems.size(), is(1));

        List<ItemChannelLink> actualItemChannelLinks = new ArrayList<>(itemChannelLinkRegistry.getAll());
        assertThat(actualItemChannelLinks.size(), is(2));
        assertThat(actualItemChannelLinks.get(0).toString(),
                is(equalTo("Light3Color -> hue:LCT001:huebridge:bulb3:color")));
        assertThat(actualItemChannelLinks.get(1).toString(),
                is(equalTo("Light3Color -> hue:LCT001:huebridge:bulb4:color")));
    }
}
