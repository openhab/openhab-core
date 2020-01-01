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
package org.openhab.core.model.thing.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.thing.internal.GenericItemChannelLinkProvider;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkProvider;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;

/**
 *
 * @author Simon Kaufmann - Initial contribution and API
 */
public class GenericItemChannelLinkProviderJavaTest extends JavaOSGiTest {

    private static final String THINGS_TESTMODEL_NAME = "test.things";
    private static final String ITEMS_TESTMODEL_NAME = "test.items";

    private static final String ITEM = "test";
    private static final String CHANNEL = "test:test:test:test";
    private static final String LINK = ITEM + " -> " + CHANNEL;

    private ModelRepository modelRepository;
    private ThingRegistry thingRegistry;
    private ItemRegistry itemRegistry;
    private ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ItemChannelLinkProvider itemChannelLinkProvider;

    @Mock
    private ProviderChangeListener<ItemChannelLink> listener;

    @Before
    public void setUp() {
        registerVolatileStorageService();

        initMocks(this);

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

        assertThat(itemRegistry.getItems().size(), is(0));
        assertThat(itemChannelLinkRegistry.getAll().size(), is(0));
    }

    @Test
    public void testIntegrationWithGenericItemProvider() throws Exception {
        Thread.sleep(2500); // Wait for the ChannelItemProvider to join the game

        Collection<Thing> things = thingRegistry.getAll();
        assertThat(things.size(), is(0));

        String thingsModel =

                "Bridge hue:bridge:huebridge [ ipAddress = \"192.168.3.84\", userName = \"19fc3fa6fc870a4280a55f21315631f\" ] {"
                        + "LCT001 bulb3 [ lightId = \"3\" ]" + "LCT001 bulb4 [ lightId = \"3\" ]" + "}";
        modelRepository.addOrRefreshModel(THINGS_TESTMODEL_NAME, new ByteArrayInputStream(thingsModel.getBytes()));
        Collection<Thing> actualThings = thingRegistry.getAll();

        assertThat(actualThings.size(), is(3));

        assertThat(itemRegistry.getItems().size(), is(0));
        assertThat(itemChannelLinkRegistry.getAll().size(), is(0));

        String itemsModel = "Color Light3Color \"Light3 Color\" { channel=\"hue:LCT001:huebridge:bulb3:color\" }"
                + "Group:Switch:MAX TestSwitches";

        // Initially load the model
        modelRepository.addOrRefreshModel(ITEMS_TESTMODEL_NAME, new ByteArrayInputStream(itemsModel.getBytes()));
        assertThat(itemRegistry.getItems().size(), is(2));
        assertThat(itemChannelLinkRegistry.getAll().size(), is(1));

        // Update the model to run into GenericItemChannelLinkProvider's amnesia
        modelRepository.addOrRefreshModel(ITEMS_TESTMODEL_NAME, new ByteArrayInputStream(itemsModel.getBytes()));
        assertThat(itemRegistry.getItems().size(), is(2));
        assertThat(itemChannelLinkRegistry.getAll().size(), is(1));

        // Remove the model (-> the link and therefore the item is kept)
        modelRepository.removeModel(ITEMS_TESTMODEL_NAME);
        assertThat(itemRegistry.getItems().size(), is(0));
        assertThat(itemChannelLinkRegistry.getAll().size(), is(0));

        // Now add the model again
        modelRepository.addOrRefreshModel(ITEMS_TESTMODEL_NAME, new ByteArrayInputStream(itemsModel.getBytes()));
        assertThat(itemRegistry.getItems().size(), is(2)); // -> ensure ChannelItemProvider cleans up properly
        assertThat(itemChannelLinkRegistry.getAll().size(), is(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNoAmnesia() throws Exception {
        GenericItemChannelLinkProvider provider = new GenericItemChannelLinkProvider();
        provider.addProviderChangeListener(listener);

        provider.startConfigurationUpdate(ITEMS_TESTMODEL_NAME);
        provider.processBindingConfiguration(ITEMS_TESTMODEL_NAME, "Number", ITEM, CHANNEL, new Configuration());
        provider.stopConfigurationUpdate(ITEMS_TESTMODEL_NAME);
        assertThat(provider.getAll().size(), is(1));
        assertThat(provider.getAll().iterator().next().toString(), is(LINK));
        verify(listener, only()).added(same(provider), eq(new ItemChannelLink(ITEM, new ChannelUID(CHANNEL))));

        reset(listener);
        provider.startConfigurationUpdate(ITEMS_TESTMODEL_NAME);
        provider.processBindingConfiguration(ITEMS_TESTMODEL_NAME, "Number", ITEM, CHANNEL, new Configuration());
        provider.stopConfigurationUpdate(ITEMS_TESTMODEL_NAME);
        assertThat(provider.getAll().size(), is(1));
        assertThat(provider.getAll().iterator().next().toString(), is(LINK));
        verify(listener, only()).updated(same(provider), eq(new ItemChannelLink(ITEM, new ChannelUID(CHANNEL))),
                eq(new ItemChannelLink(ITEM, new ChannelUID(CHANNEL))));

        reset(listener);
        provider.startConfigurationUpdate(ITEMS_TESTMODEL_NAME);
        provider.stopConfigurationUpdate(ITEMS_TESTMODEL_NAME);
        assertThat(provider.getAll().size(), is(0));
        verify(listener, only()).removed(same(provider), eq(new ItemChannelLink(ITEM, new ChannelUID(CHANNEL))));
    }

    @Test
    public void testLinkConfiguration() {
        Collection<Thing> things = thingRegistry.getAll();
        assertThat(things.size(), is(0));

        String thingsModel = "Bridge hue:bridge:huebridge [ ipAddress = \"192.168.3.84\", userName = \"19fc3fa6fc870a4280a55f21315631f\" ] {"
                + "LCT001 bulb3 [ lightId = \"3\" ]" //
                + "LCT001 bulb4 [ lightId = \"3\" ]" //
                + "}";
        modelRepository.addOrRefreshModel(THINGS_TESTMODEL_NAME, new ByteArrayInputStream(thingsModel.getBytes()));
        waitForAssert(() -> {
            assertThat(thingRegistry.getAll().size(), is(3));
        });

        String itemsModel = "Color Light3Color \"Light3 Color\" { channel=\"hue:LCT001:huebridge:bulb3:color\" [ foo=\"bar\", answer=42, always=true ] }"
                + "Group:Switch:MAX TestSwitches";
        modelRepository.addOrRefreshModel(ITEMS_TESTMODEL_NAME, new ByteArrayInputStream(itemsModel.getBytes()));
        waitForAssert(() -> {
            assertThat(itemRegistry.getItems().size(), is(2));
            assertThat(itemChannelLinkRegistry.getAll().size(), is(1));
        });

        ItemChannelLink link = itemChannelLinkRegistry.get("Light3Color -> hue:LCT001:huebridge:bulb3:color");
        assertNotNull(link);
        assertEquals("Light3Color", link.getItemName());
        assertEquals("hue:LCT001:huebridge:bulb3:color", link.getLinkedUID().toString());
        assertEquals("bar", link.getConfiguration().get("foo"));
        assertEquals(new BigDecimal(42), link.getConfiguration().get("answer"));
        assertEquals(true, link.getConfiguration().get("always"));
    }

    @Test
    public void testMultiLinkConfiguration() {
        Collection<Thing> things = thingRegistry.getAll();
        assertThat(things.size(), is(0));

        String thingsModel = "Bridge hue:bridge:huebridge [ ipAddress = \"192.168.3.84\", userName = \"19fc3fa6fc870a4280a55f21315631f\" ] {"
                + "LCT001 bulb2 [ lightId = \"2\" ]" //
                + "LCT001 bulb3 [ lightId = \"3\" ]" //
                + "LCT001 bulb4 [ lightId = \"4\" ]" //
                + "}";
        modelRepository.addOrRefreshModel(THINGS_TESTMODEL_NAME, new ByteArrayInputStream(thingsModel.getBytes()));

        String itemsModel = "Color Light3Color \"Light3 Color\" { " + //
                "channel=\"hue:LCT001:huebridge:bulb2:color,hue:LCT001:huebridge:bulb3:color\" [ value=1 ]," + //
                "channel=\"hue:LCT001:huebridge:bulb4:color\" [ value=2 ]" + //
                "}";
        modelRepository.addOrRefreshModel(ITEMS_TESTMODEL_NAME, new ByteArrayInputStream(itemsModel.getBytes()));

        waitForAssert(() -> {
            assertThat(thingRegistry.getAll().size(), is(4));
            assertThat(itemRegistry.getItems().size(), is(1));
            assertThat(itemChannelLinkRegistry.getAll().size(), is(3));
        });

        assertEquals(new BigDecimal(1), itemChannelLinkRegistry.get("Light3Color -> hue:LCT001:huebridge:bulb2:color")
                .getConfiguration().get("value"));
        assertEquals(new BigDecimal(1), itemChannelLinkRegistry.get("Light3Color -> hue:LCT001:huebridge:bulb3:color")
                .getConfiguration().get("value"));
        assertEquals(new BigDecimal(2), itemChannelLinkRegistry.get("Light3Color -> hue:LCT001:huebridge:bulb4:color")
                .getConfiguration().get("value"));
    }

}
