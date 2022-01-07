/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.ui.internal.proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.model.sitemap.SitemapProvider;
import org.openhab.core.model.sitemap.sitemap.Image;
import org.openhab.core.model.sitemap.sitemap.Sitemap;
import org.openhab.core.model.sitemap.sitemap.Switch;
import org.openhab.core.model.sitemap.sitemap.Video;
import org.openhab.core.types.UnDefType;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.osgi.service.http.HttpService;

/**
 * Unit tests for the {@link ProxyServletService} class.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class ProxyServletServiceTest {

    private static final String SITEMAP_NAME = "testSitemap";

    private static final String SWITCH_WIDGET_ID = "switchWidget";
    private static final String IMAGE_WIDGET_ID = "imageWidget";
    private static final String VIDEO_WIDGET_ID = "videoWidget";

    private static final String ITEM_NAME_UNDEF_STATE = "itemUNDEF";
    private static final String ITEM_NAME_NULL_STATE = "itemNULL";
    private static final String ITEM_NAME_ON_STATE = "itemON";
    private static final String ITEM_NAME_INVALID_URL = "itemInvalidUrl";
    private static final String ITEM_NAME_VALID_IMAGE_URL = "itemValidImageUrl";
    private static final String ITEM_NAME_VALID_VIDEO_URL = "itemValidVideoUrl";

    private static final String INVALID_URL = "test";
    private static final String ITEM_VALID_IMAGE_URL = "https://openhab.org/item.jpg";
    private static final String ITEM_VALID_VIDEO_URL = "https://openhab.org/item.mp4";
    private static final String VALID_IMAGE_URL = "https://openhab.org/test.jpg";
    private static final String VALID_VIDEO_URL = "https://openhab.org/test.mp4";

    private static ProxyServletService service;

    private ItemUIRegistry itemUIRegistry;
    private HttpService httpService;
    private SitemapProvider sitemapProvider;
    private Sitemap sitemap;
    private HttpServletRequest request;
    private Switch switchWidget;
    private Image imageWidget;
    private Video videoWidget;

    @BeforeEach
    public void setUp() {
        itemUIRegistry = mock(ItemUIRegistry.class);
        httpService = mock(HttpService.class);

        service = new ProxyServletService(itemUIRegistry, httpService);

        sitemapProvider = mock(SitemapProvider.class);
        service.sitemapProviders.add(sitemapProvider);

        sitemap = mock(Sitemap.class);
        when(sitemapProvider.getSitemap(eq(SITEMAP_NAME))).thenReturn(sitemap);

        switchWidget = mock(Switch.class);
        when(itemUIRegistry.getWidget(eq(sitemap), eq(SWITCH_WIDGET_ID))).thenReturn(switchWidget);
        imageWidget = mock(Image.class);
        when(itemUIRegistry.getWidget(eq(sitemap), eq(IMAGE_WIDGET_ID))).thenReturn(imageWidget);
        videoWidget = mock(Video.class);
        when(itemUIRegistry.getWidget(eq(sitemap), eq(VIDEO_WIDGET_ID))).thenReturn(videoWidget);

        when(itemUIRegistry.getItemState(eq(ITEM_NAME_UNDEF_STATE))).thenReturn(UnDefType.UNDEF);
        when(itemUIRegistry.getItemState(eq(ITEM_NAME_NULL_STATE))).thenReturn(UnDefType.NULL);
        when(itemUIRegistry.getItemState(eq(ITEM_NAME_ON_STATE))).thenReturn(OnOffType.ON);
        when(itemUIRegistry.getItemState(eq(ITEM_NAME_INVALID_URL))).thenReturn(new StringType(INVALID_URL));
        when(itemUIRegistry.getItemState(eq(ITEM_NAME_VALID_IMAGE_URL)))
                .thenReturn(new StringType(ITEM_VALID_IMAGE_URL));
        when(itemUIRegistry.getItemState(eq(ITEM_NAME_VALID_VIDEO_URL)))
                .thenReturn(new StringType(ITEM_VALID_VIDEO_URL));

        request = mock(HttpServletRequest.class);
        when(request.getParameter(eq("sitemap"))).thenReturn(SITEMAP_NAME);
    }

    @Test
    public void testMaybeAppendAuthHeaderWithFullCredentials() throws URISyntaxException {
        Request request = mock(Request.class);
        URI uri = new URI("http://testuser:testpassword@127.0.0.1:8080/content");
        service.maybeAppendAuthHeader(uri, request);
        verify(request).header(HttpHeader.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString("testuser:testpassword".getBytes()));
    }

    @Test
    public void testMaybeAppendAuthHeaderWithoutPassword() throws URISyntaxException {
        Request request = mock(Request.class);
        URI uri = new URI("http://testuser@127.0.0.1:8080/content");
        service.maybeAppendAuthHeader(uri, request);
        verify(request).header(HttpHeader.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString("testuser:".getBytes()));
    }

    @Test
    public void testMaybeAppendAuthHeaderWithoutCredentials() throws URISyntaxException {
        Request request = mock(Request.class);
        URI uri = new URI("http://127.0.0.1:8080/content");
        service.maybeAppendAuthHeader(uri, request);
        verify(request, never()).header(any(HttpHeader.class), anyString());
    }

    @Test
    public void testProxyUriUnexpectedWidgetType() {
        when(request.getParameter(eq("widgetId"))).thenReturn(SWITCH_WIDGET_ID);
        URI uri = service.uriFromRequest(request);
        assertNull(uri);
    }

    @Test
    public void testProxyUriImageWithoutItemButValidUrl() {
        when(request.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidget.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidget.getItem()).thenReturn(null);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_IMAGE_URL);
    }

    @Test
    public void testProxyUriImageWithoutItemAndInvalidUrl() {
        when(request.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidget.getUrl()).thenReturn(INVALID_URL);
        when(imageWidget.getItem()).thenReturn(null);
        URI uri = service.uriFromRequest(request);
        assertNull(uri);
    }

    @Test
    public void testProxyUriImageWithItemButUndefState() {
        when(request.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidget.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidget.getItem()).thenReturn(ITEM_NAME_UNDEF_STATE);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_IMAGE_URL);
    }

    @Test
    public void testProxyUriImageWithItemButNullState() {
        when(request.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidget.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidget.getItem()).thenReturn(ITEM_NAME_NULL_STATE);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_IMAGE_URL);
    }

    @Test
    public void testProxyUriImageWithItemButUnexpectedState() {
        when(request.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidget.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidget.getItem()).thenReturn(ITEM_NAME_ON_STATE);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_IMAGE_URL);
    }

    @Test
    public void testProxyUriImageWithItemButStateWithInvalidUrl() {
        when(request.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidget.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidget.getItem()).thenReturn(ITEM_NAME_INVALID_URL);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_IMAGE_URL);
    }

    @Test
    public void testProxyUriImageWithItemAndStateWithValidUrl() {
        when(request.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidget.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidget.getItem()).thenReturn(ITEM_NAME_VALID_IMAGE_URL);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), ITEM_VALID_IMAGE_URL);
    }

    @Test
    public void testProxyUriVideoWithoutItemButValidUrl() {
        when(request.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidget.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidget.getItem()).thenReturn(null);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_VIDEO_URL);
    }

    @Test
    public void testProxyUriVideoWithoutItemAndInvalidUrl() {
        when(request.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidget.getUrl()).thenReturn(INVALID_URL);
        when(videoWidget.getItem()).thenReturn(null);
        URI uri = service.uriFromRequest(request);
        assertNull(uri);
    }

    @Test
    public void testProxyUriVideoWithItemButUndefState() {
        when(request.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidget.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidget.getItem()).thenReturn(ITEM_NAME_UNDEF_STATE);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_VIDEO_URL);
    }

    @Test
    public void testProxyUriVideoWithItemButNullState() {
        when(request.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidget.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidget.getItem()).thenReturn(ITEM_NAME_NULL_STATE);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_VIDEO_URL);
    }

    @Test
    public void testProxyUriVideoWithItemButUnexpectedState() {
        when(request.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidget.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidget.getItem()).thenReturn(ITEM_NAME_ON_STATE);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_VIDEO_URL);
    }

    @Test
    public void testProxyUriVideoWithItemButStateWithInvalidUrl() {
        when(request.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidget.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidget.getItem()).thenReturn(ITEM_NAME_INVALID_URL);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), VALID_VIDEO_URL);
    }

    @Test
    public void testProxyUriVideoWithItemAndStateWithValidUrl() {
        when(request.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidget.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidget.getItem()).thenReturn(ITEM_NAME_VALID_VIDEO_URL);
        URI uri = service.uriFromRequest(request);
        assertNotNull(uri);
        assertEquals(uri.toString(), ITEM_VALID_VIDEO_URL);
    }
}
