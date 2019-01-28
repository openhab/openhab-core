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
package org.eclipse.smarthome.io.rest.core.internal.item;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.ManagedItemProvider;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.eclipse.smarthome.core.items.MetadataProvider;
import org.eclipse.smarthome.core.items.dto.GroupItemDTO;
import org.eclipse.smarthome.core.items.dto.MetadataDTO;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;

public class ItemResourceOSGiTest extends JavaOSGiTest {

    private static final String ITEM_NAME1 = "Item1";
    private static final String ITEM_NAME2 = "Item2";
    private static final String ITEM_NAME3 = "Item3";

    private GenericItem item1;
    private GenericItem item2;
    private GenericItem item3;

    @Mock
    private ItemProvider itemProvider;

    private ItemResource itemResource;

    private ManagedItemProvider managedItemProvider;

    @Before
    public void setup() {
        initMocks(this);

        itemResource = getService(RESTResource.class, ItemResource.class);
        assertNotNull(itemResource);

        itemResource.uriInfo = mock(UriInfo.class);

        registerVolatileStorageService();
        managedItemProvider = getService(ManagedItemProvider.class);

        item1 = new SwitchItem(ITEM_NAME1);
        item2 = new SwitchItem(ITEM_NAME2);
        item3 = new DimmerItem(ITEM_NAME3);

        when(itemProvider.getAll()).thenReturn(Arrays.asList(item1, item2, item3));
        registerService(itemProvider);
    }

    @Test
    public void shouldFilterItemsByTag() throws Exception {
        item1.addTag("Tag1");
        item2.addTag("Tag1");
        item2.addTag("Tag2");
        item3.addTag("Tag2");

        Response response = itemResource.getItems(null, null, "Tag1", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasItems(ITEM_NAME1, ITEM_NAME2));

        response = itemResource.getItems(null, null, "Tag2", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasItems(ITEM_NAME2, ITEM_NAME3));

        response = itemResource.getItems(null, null, "NotExistingTag", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasSize(0));
    }

    @Test
    public void shouldFilterItemsByType() throws Exception {
        Response response = itemResource.getItems(null, "Switch", null, null, false, null);
        assertThat(readItemNamesFromResponse(response), hasItems(ITEM_NAME1, ITEM_NAME2));

        response = itemResource.getItems(null, "Dimmer", null, null, false, null);
        assertThat(readItemNamesFromResponse(response), hasItems(ITEM_NAME3));

        response = itemResource.getItems(null, "Color", null, null, false, null);
        assertThat(readItemNamesFromResponse(response), hasSize(0));
    }

    @Test
    public void shouldAddAndRemoveTags() throws Exception {
        managedItemProvider.add(new SwitchItem("Switch"));

        Response response = itemResource.getItems(null, null, "MyTag", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasSize(0));

        itemResource.addTag("Switch", "MyTag");
        response = itemResource.getItems(null, null, "MyTag", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasSize(1));

        itemResource.removeTag("Switch", "MyTag");
        response = itemResource.getItems(null, null, "MyTag", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasSize(0));
    }

    @Test
    public void shouldIncludeRequestedFieldsOnly() throws Exception {
        JsonParser parser = new JsonParser();
        managedItemProvider.add(new SwitchItem("Switch"));
        itemResource.addTag("Switch", "MyTag");
        Response response = itemResource.getItems(null, null, "MyTag", null, false, "type,name");

        JsonElement result = parser.parse(IOUtils.toString((InputStream) response.getEntity()));
        JsonElement expected = parser.parse("[{editable: true, type: \"Switch\", name: \"Switch\"}]");
        assertEquals(expected, result);
    }

    @Test
    public void shouldProvideReturnCodesForTagHandling() {
        Response response = itemResource.addTag("Switch", "MyTag");
        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));

        response = itemResource.removeTag("Switch", "MyTag");
        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));

        unregisterService(itemProvider);
        when(itemProvider.getAll()).thenReturn(Collections.singletonList(new SwitchItem("UnmanagedItem")));
        registerService(itemProvider);

        response = itemResource.addTag("UnmanagedItem", "MyTag");
        assertThat(response.getStatus(), is(Status.METHOD_NOT_ALLOWED.getStatusCode()));
    }

    private List<String> readItemNamesFromResponse(Response response) throws IOException {
        String jsonResponse = IOUtils.toString((InputStream) response.getEntity());
        return JsonPath.read(jsonResponse, "$..name");
    }

    @Test
    public void addMultipleItems() throws IOException {

        List<GroupItemDTO> itemList = new ArrayList<>();
        GroupItemDTO[] items = new GroupItemDTO[] {};

        GroupItemDTO item1DTO = new GroupItemDTO();
        item1DTO.name = "item1";
        item1DTO.type = "Switch";
        item1DTO.label = "item1Label";
        itemList.add(item1DTO);

        GroupItemDTO item2DTO = new GroupItemDTO();
        item2DTO.name = "item2";
        item2DTO.type = "Rollershutter";
        item2DTO.label = "item2Label";
        itemList.add(item2DTO);

        items = itemList.toArray(items);
        Response response = itemResource.createOrUpdateItems(items);

        String jsonResponse = IOUtils.toString((InputStream) response.getEntity());
        List<String> statusCodes = JsonPath.read(jsonResponse, "$..status");

        // expect 2x created
        assertThat(statusCodes.size(), is(2));
        assertThat(statusCodes.get(0), is("created"));
        assertThat(statusCodes.get(1), is("created"));

        itemList.clear();

        item1DTO.label = "item1LabelNew";
        itemList.add(item1DTO);
        item2DTO.type = "WrongType";
        itemList.add(item2DTO);

        items = itemList.toArray(items);
        response = itemResource.createOrUpdateItems(items);

        jsonResponse = IOUtils.toString((InputStream) response.getEntity());
        statusCodes = JsonPath.read(jsonResponse, "$..status");

        // expect error and updated
        assertThat(statusCodes.size(), is(2));
        assertThat(statusCodes.get(0), is("error"));
        assertThat(statusCodes.get(1), is("updated"));
    }

    @Test
    public void testMetadata() {
        MetadataDTO dto = new MetadataDTO();
        dto.value = "some value";
        assertEquals(201, itemResource.addMetadata(ITEM_NAME1, "namespace", dto).getStatus());
        assertEquals(200, itemResource.removeMetadata(ITEM_NAME1, "namespace").getStatus());
        assertEquals(404, itemResource.removeMetadata(ITEM_NAME1, "namespace").getStatus());
    }

    @Test
    public void testAddMetadata_nonExistingItem() {
        MetadataDTO dto = new MetadataDTO();
        dto.value = "some value";
        Response response = itemResource.addMetadata("nonExisting", "foo", dto);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testAddMetadata_ValueEmtpy() {
        MetadataDTO dto = new MetadataDTO();
        dto.value = "";
        Response response = itemResource.addMetadata(ITEM_NAME1, "foo", dto);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testAddMetadata_ValueNull() {
        MetadataDTO dto = new MetadataDTO();
        dto.value = null;
        Response response = itemResource.addMetadata(ITEM_NAME1, "foo", dto);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testAddMetadata_update() {
        MetadataDTO dto = new MetadataDTO();
        dto.value = "some value";
        assertEquals(201, itemResource.addMetadata(ITEM_NAME1, "namespace", dto).getStatus());
        MetadataDTO dto2 = new MetadataDTO();
        dto2.value = "new value";
        assertEquals(200, itemResource.addMetadata(ITEM_NAME1, "namespace", dto2).getStatus());
    }

    @Test
    public void testRemoveMetadata_nonExistingItem() {
        Response response = itemResource.removeMetadata("nonExisting", "anything");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testRemoveMetadata_nonExistingNamespace() {
        Response response = itemResource.removeMetadata(ITEM_NAME1, "anything");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testRemoveMetadata_unmanagedMetadata() {
        MetadataProvider provider = mock(MetadataProvider.class);
        when(provider.getAll()).thenReturn(
                Collections.singleton(new Metadata(new MetadataKey("namespace", ITEM_NAME1), "some value", null)));
        registerService(provider);

        Response response = itemResource.removeMetadata(ITEM_NAME1, "namespace");
        assertEquals(409, response.getStatus());
    }

}
