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
package org.openhab.core.io.rest.core.internal.link;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
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
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.link.ManagedItemChannelLinkProvider;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.JsonPath;

/**
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ItemChannelLinkResourceOSGiTest extends JavaOSGiTest {

    private static final String THING_TYPE_UID = "thing:type:uid";
    private static final String UID = "thing:uid:1";
    private static final String ITEM_NAME1 = "Item1";
    private static final String ITEM_NAME2 = "Item2";
    private static final String ITEM_NAME3 = "Item3";
    private static final String CHANNEL_UID1 = THING_TYPE_UID + ":" + UID + ":1";
    private static final String CHANNEL_UID2 = THING_TYPE_UID + ":" + UID + ":2";

    private @NonNullByDefault({}) ItemChannelLink link1;
    private @NonNullByDefault({}) ItemChannelLink link2;
    private @NonNullByDefault({}) ItemChannelLink link3;

    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) ItemChannelLinkResource itemChannelLinkResource;
    private @NonNullByDefault({}) ManagedItemChannelLinkProvider managedItemChannelLinkProvider;

    private @Mock @NonNullByDefault({}) HttpHeaders httpHeadersMock;
    private @Mock @NonNullByDefault({}) ItemChannelLinkProvider itemChannelLinkProviderMock;
    private @Mock @NonNullByDefault({}) UriBuilder uriBuilderMock;
    private @Mock @NonNullByDefault({}) UriInfo uriInfoMock;

    @BeforeEach
    public void beforeEach() {
        registerVolatileStorageService();

        itemChannelLinkResource = getService(RESTResource.class, ItemChannelLinkResource.class);
        assertNotNull(itemChannelLinkResource);

        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        assertNotNull(itemChannelLinkRegistry);

        managedItemChannelLinkProvider = getService(ManagedItemChannelLinkProvider.class);
        assertNotNull(managedItemChannelLinkProvider);

        link1 = new ItemChannelLink(ITEM_NAME1, new ChannelUID(CHANNEL_UID1));
        link2 = new ItemChannelLink(ITEM_NAME2, new ChannelUID(CHANNEL_UID2));
        link3 = new ItemChannelLink(ITEM_NAME3, new ChannelUID(THING_TYPE_UID + ":" + UID + ":3"));

        when(itemChannelLinkProviderMock.getAll()).thenReturn(List.of(link1, link2, link3));
        registerService(itemChannelLinkProviderMock);

        waitForAssert(() -> {
            assertThat(itemChannelLinkRegistry.getAll(), hasSize(3));
        });

        when(uriBuilderMock.build(any())).thenReturn(URI.create(""));

        when(uriInfoMock.getAbsolutePathBuilder()).thenReturn(uriBuilderMock);
        when(uriInfoMock.getPath()).thenReturn("");

        when(httpHeadersMock.getHeaderString(anyString())).thenReturn(null);
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
