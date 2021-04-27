/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.internal.link;

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
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.io.rest.RESTResource;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkProvider;
import org.openhab.core.thing.link.ManagedItemChannelLinkProvider;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.JsonPath;

/**
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ItemChannelLinkResourceOSGiTest extends JavaOSGiTest {

    private static final String THING_TYPE_UID = "thing:type:uid";
    private static final String UID = "thing:uid:1";
    private static final String ITEM_NAME1 = "Item1";
    private static final String ITEM_NAME2 = "Item2";
    private static final String ITEM_NAME3 = "Item3";
    private static final String CHANNEL_UID1 = THING_TYPE_UID + ":" + UID + ":1";
    private static final String CHANNEL_UID2 = THING_TYPE_UID + ":" + UID + ":2";

    private ItemChannelLink link1;
    private ItemChannelLink link2;
    private ItemChannelLink link3;

    private @Mock ItemChannelLinkProvider itemChannelLinkProvider;

    private UriInfo uriInfo;
    private HttpHeaders httpHeaders;

    private ItemChannelLinkResource itemChannelLinkResource;
    private ManagedItemChannelLinkProvider managedItemChannelLinkProvider;

    @BeforeEach
    public void beforeEach() {
        registerVolatileStorageService();

        itemChannelLinkResource = getService(RESTResource.class, ItemChannelLinkResource.class);
        assertNotNull(itemChannelLinkResource);

        managedItemChannelLinkProvider = getService(ManagedItemChannelLinkProvider.class);
        assertNotNull(managedItemChannelLinkProvider);

        link1 = new ItemChannelLink(ITEM_NAME1, new ChannelUID(CHANNEL_UID1));
        link2 = new ItemChannelLink(ITEM_NAME2, new ChannelUID(CHANNEL_UID2));
        link3 = new ItemChannelLink(ITEM_NAME3, new ChannelUID(THING_TYPE_UID + ":" + UID + ":3"));

        when(itemChannelLinkProvider.getAll()).thenReturn(List.of(link1, link2, link3));
        registerService(itemChannelLinkProvider);

        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriBuilder.build(any())).thenReturn(URI.create(""));
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriInfo.getPath()).thenReturn("");
        httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getHeaderString(anyString())).thenReturn(null);
    }

    @Test
    public void shouldReturnLinks() throws IOException {
        Response response = itemChannelLinkResource.getAll(null, null);
        List<String> itemNames = readItemNamesFromResponse(response);
        assertThat(itemNames, hasSize(3));
        assertThat(itemNames, hasItems(ITEM_NAME1, ITEM_NAME2, ITEM_NAME3));
    }

    @Test
    public void shouldFilterLinksByChannelUID() throws IOException {
        Response response = itemChannelLinkResource.getAll(CHANNEL_UID1, null);
        List<String> itemNames = readItemNamesFromResponse(response);
        assertThat(itemNames, hasSize(1));
        assertThat(itemNames, hasItems(ITEM_NAME1));
    }

    @Test
    public void shouldFilterLinksByItemName() throws IOException {
        Response response = itemChannelLinkResource.getAll(null, ITEM_NAME2);
        List<String> itemNames = readItemNamesFromResponse(response);
        assertThat(itemNames, hasSize(1));
        assertThat(itemNames, hasItems(ITEM_NAME2));
    }

    @Test
    public void shouldReturnLink() throws Exception {
        Response response = itemChannelLinkResource.getLink(ITEM_NAME1, CHANNEL_UID1);
        List<String> itemNames = readItemNamesFromResponse(response);
        assertThat(itemNames, hasSize(1));
        assertThat(itemNames, hasItems(ITEM_NAME1));
    }

    @Test
    public void shouldIncludeEditableFields() throws IOException, JsonSyntaxException {
        managedItemChannelLinkProvider.add(link1);
        Response response = itemChannelLinkResource.getLink(ITEM_NAME1, CHANNEL_UID1);
        JsonElement result = JsonParser
                .parseString(new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8));
        JsonElement expected = JsonParser.parseString("{channelUID:\"" + CHANNEL_UID1
                + "\", configuration:{}, editable:true, itemName:\"" + ITEM_NAME1 + "\"}");
        assertEquals(expected, result);

        response = itemChannelLinkResource.getAll(CHANNEL_UID1, ITEM_NAME1);
        result = JsonParser
                .parseString(new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8));
        expected = JsonParser.parseString("[{channelUID:\"" + CHANNEL_UID1
                + "\", configuration:{}, editable:true, itemName:\"" + ITEM_NAME1 + "\"}]");
        assertEquals(expected, result);

        response = itemChannelLinkResource.getLink(ITEM_NAME2, CHANNEL_UID2);
        result = JsonParser
                .parseString(new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8));
        expected = JsonParser.parseString("{channelUID:\"" + CHANNEL_UID2
                + "\", configuration:{}, editable:false, itemName:\"" + ITEM_NAME2 + "\", configuration:{}}");
        assertEquals(expected, result);
    }

    private List<String> readItemNamesFromResponse(Response response) throws IOException {
        String jsonResponse = new String(((InputStream) response.getEntity()).readAllBytes(), StandardCharsets.UTF_8);
        return JsonPath.read(jsonResponse, "$..itemName");
    }
}
