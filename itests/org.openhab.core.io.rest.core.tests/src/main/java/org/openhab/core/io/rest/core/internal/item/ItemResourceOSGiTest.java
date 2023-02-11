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
package org.openhab.core.io.rest.core.internal.item;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.ManagedItemProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.items.dto.GroupItemDTO;
import org.openhab.core.items.dto.MetadataDTO;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.transform.TransformationException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;

/**
 * @author Henning Treu - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ItemResourceOSGiTest extends JavaOSGiTest {

    private static final String ITEM_NAME1 = "Item1";
    private static final String ITEM_NAME2 = "Item2";
    private static final String ITEM_NAME3 = "Item3";
    private static final String ITEM_NAME4 = "Item4";
    private static final String ITEM_LABEL4 = "Test äöüß";

    private @NonNullByDefault({}) GenericItem item1;
    private @NonNullByDefault({}) GenericItem item2;
    private @NonNullByDefault({}) GenericItem item3;
    private @NonNullByDefault({}) GenericItem item4;

    private @NonNullByDefault({}) ItemResource itemResource;
    private @NonNullByDefault({}) ManagedItemProvider managedItemProvider;

    private @Mock @NonNullByDefault({}) HttpHeaders httpHeadersMock;
    private @Mock @NonNullByDefault({}) ItemProvider itemProviderMock;
    private @Mock @NonNullByDefault({}) UriBuilder uriBuilderMock;
    private @Mock @NonNullByDefault({}) UriInfo uriInfoMock;

    @BeforeEach
    public void beforeEach() {
        registerVolatileStorageService();

        itemResource = getService(RESTResource.class, ItemResource.class);
        assertNotNull(itemResource);

        managedItemProvider = getService(ManagedItemProvider.class);
        assertNotNull(managedItemProvider);

        item1 = new SwitchItem(ITEM_NAME1);
        item2 = new SwitchItem(ITEM_NAME2);
        item3 = new DimmerItem(ITEM_NAME3);
        item4 = new StringItem(ITEM_NAME4);

        when(itemProviderMock.getAll()).thenReturn(List.of(item1, item2, item3, item4));
        registerService(itemProviderMock);

        when(uriBuilderMock.build(any())).thenReturn(URI.create(""));
        when(uriBuilderMock.path(anyString())).thenReturn(uriBuilderMock);

        when(uriInfoMock.getAbsolutePathBuilder()).thenReturn(uriBuilderMock);
        when(uriInfoMock.getBaseUriBuilder()).thenReturn(uriBuilderMock);
        when(uriInfoMock.getPath()).thenReturn("");

        when(httpHeadersMock.getHeaderString(anyString())).thenReturn(null);
    }

    @Test
    public void shouldReturnUnicodeItems() throws IOException, TransformationException {
        item4.setLabel(ITEM_LABEL4);

        Response response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, null, null, null, false, null);
        assertThat(readItemLabelsFromResponse(response), hasItems(ITEM_LABEL4));
    }

    @Test
    public void shouldReturnUnicodeItem() throws IOException, TransformationException {
        item4.setLabel(ITEM_LABEL4);

        Response response = itemResource.getItemData(uriInfoMock, httpHeadersMock, null, null, true, ITEM_NAME4);
        assertThat(readItemLabelsFromResponse(response), hasItems(ITEM_LABEL4));
    }

    @Test
    public void shouldFilterItemsByTag() throws Exception {
        item1.addTag("Tag1");
        item2.addTag("Tag1");
        item2.addTag("Tag2");
        item3.addTag("Tag2");
        item4.addTag("Tag4");

        Response response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, null, "Tag1", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasItems(ITEM_NAME1, ITEM_NAME2));

        response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, null, "Tag2", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasItems(ITEM_NAME2, ITEM_NAME3));

        response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, null, "NotExistingTag", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasSize(0));
    }

    @Test
    public void shouldFilterItemsByType() throws Exception {
        Response response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, CoreItemFactory.SWITCH, null,
                null, false, null);
        assertThat(readItemNamesFromResponse(response), hasItems(ITEM_NAME1, ITEM_NAME2));

        response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, CoreItemFactory.DIMMER, null, null, false,
                null);
        assertThat(readItemNamesFromResponse(response), hasItems(ITEM_NAME3));

        response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, CoreItemFactory.COLOR, null, null, false,
                null);
        assertThat(readItemNamesFromResponse(response), hasSize(0));
    }

    @Test
    public void shouldAddAndRemoveTags() throws Exception {
        managedItemProvider.add(new SwitchItem("Switch"));

        Response response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, null, "MyTag", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasSize(0));

        itemResource.addTag("Switch", "MyTag");
        response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, null, "MyTag", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasSize(1));

        itemResource.removeTag("Switch", "MyTag");
        response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, null, "MyTag", null, false, null);
        assertThat(readItemNamesFromResponse(response), hasSize(0));
    }

    @Test
    public void shouldIncludeRequestedFieldsOnly() throws Exception {
        managedItemProvider.add(new SwitchItem("Switch"));
        itemResource.addTag("Switch", "MyTag");
        Response response = itemResource.getItems(uriInfoMock, httpHeadersMock, null, null, "MyTag", null, false,
                "type,name");

        JsonElement result = JsonParser
                .parseString(new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8));
        JsonElement expected = JsonParser.parseString("[{type: \"Switch\", name: \"Switch\"}]");
        assertEquals(expected, result);
    }

    @Test
    public void shouldProvideReturnCodesForTagHandling() {
        Response response = itemResource.addTag("Switch", "MyTag");
        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));

        response = itemResource.removeTag("Switch", "MyTag");
        assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));

        unregisterService(itemProviderMock);
        when(itemProviderMock.getAll()).thenReturn(List.of(new SwitchItem("UnmanagedItem")));
        registerService(itemProviderMock);

        response = itemResource.addTag("UnmanagedItem", "MyTag");
        assertThat(response.getStatus(), is(Status.METHOD_NOT_ALLOWED.getStatusCode()));
    }

    private List<String> readItemNamesFromResponse(Response response) throws IOException {
        String jsonResponse = new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8);
        return JsonPath.read(jsonResponse, "$..name");
    }

    private List<String> readItemLabelsFromResponse(Response response) throws IOException, TransformationException {
        String jsonResponse = new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8);
        return JsonPath.read(jsonResponse, "$..label");
    }

    @Test
    public void addMultipleItems() throws IOException {
        List<GroupItemDTO> itemList = new ArrayList<>();
        GroupItemDTO[] items = {};

        GroupItemDTO item1DTO = new GroupItemDTO();
        item1DTO.name = "item1";
        item1DTO.type = CoreItemFactory.SWITCH;
        item1DTO.label = "item1Label";
        itemList.add(item1DTO);

        GroupItemDTO item2DTO = new GroupItemDTO();
        item2DTO.name = "item2";
        item2DTO.type = CoreItemFactory.ROLLERSHUTTER;
        item2DTO.label = "item2Label";
        itemList.add(item2DTO);

        items = itemList.toArray(items);
        Response response = itemResource.createOrUpdateItems(items);

        String jsonResponse = new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8);
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

        jsonResponse = new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8);
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
    public void testAddMetadataNonExistingItem() {
        MetadataDTO dto = new MetadataDTO();
        dto.value = "some value";
        Response response = itemResource.addMetadata("nonExisting", "foo", dto);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testAddMetadataValueEmpty() {
        MetadataDTO dto = new MetadataDTO();
        dto.value = "";
        Response response = itemResource.addMetadata(ITEM_NAME1, "foo", dto);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testAddMetadataValueNull() {
        MetadataDTO dto = new MetadataDTO();
        dto.value = null;
        Response response = itemResource.addMetadata(ITEM_NAME1, "foo", dto);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testAddMetadataUpdate() {
        MetadataDTO dto = new MetadataDTO();
        dto.value = "some value";
        assertEquals(201, itemResource.addMetadata(ITEM_NAME1, "namespace", dto).getStatus());
        MetadataDTO dto2 = new MetadataDTO();
        dto2.value = "new value";
        assertEquals(200, itemResource.addMetadata(ITEM_NAME1, "namespace", dto2).getStatus());
    }

    @Test
    public void testRemoveMetadataNonExistingItem() {
        Response response = itemResource.removeMetadata("nonExisting", "anything");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testRemoveMetadataNonExistingNamespace() {
        Response response = itemResource.removeMetadata(ITEM_NAME1, "anything");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testRemoveMetadataUnmanagedMetadata() {
        MetadataProvider provider = mock(MetadataProvider.class);
        when(provider.getAll())
                .thenReturn(Set.of(new Metadata(new MetadataKey("namespace", ITEM_NAME1), "some value", null)));
        registerService(provider);

        Response response = itemResource.removeMetadata(ITEM_NAME1, "namespace");
        assertEquals(409, response.getStatus());
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> findTagTestSource() {
        return Stream.of( //
                Arguments.of(ITEM_NAME3, "Location", hasItems(ITEM_NAME1)), // super-class of set tag
                Arguments.of(ITEM_NAME3, "HVAC", hasItems(ITEM_NAME2)), // tag
                Arguments.of(ITEM_NAME3, "Point", hasItems(ITEM_NAME3)), // self
                Arguments.of(ITEM_NAME3, "NotATag", null), // invalid tag
                Arguments.of(ITEM_NAME3, "Outdoor", null) // valid tag, not present
        );
    }

    @ParameterizedTest
    @MethodSource("findTagTestSource")
    public void findTagTest(String itemName, String semanticClassName, @Nullable Matcher<Iterable<String>> matcher)
            throws IOException {
        // setup test: item1 has the location, item2 the equipment, item3 is the point
        item1.addTag("Office");
        item2.addTag("HVAC");
        item2.addGroupName(ITEM_NAME1);
        item3.addTag("Point");
        item3.addGroupName(ITEM_NAME2);

        // do test
        Response response = itemResource.getSemanticItem(uriInfoMock, httpHeadersMock, null, itemName,
                semanticClassName);
        if (matcher != null) {
            assertThat(readItemNamesFromResponse(response), matcher);
        } else {
            assertThat(response.getStatus(), is(404));
        }
    }
}
