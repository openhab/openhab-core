/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Base64;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.sitemap.Image;
import org.openhab.core.sitemap.Sitemap;
import org.openhab.core.sitemap.Switch;
import org.openhab.core.sitemap.Video;
import org.openhab.core.sitemap.registry.SitemapRegistry;
import org.openhab.core.types.UnDefType;
import org.openhab.core.ui.items.ItemUIRegistry;
import org.osgi.service.http.HttpService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Unit tests for the {@link ProxyServletService} class.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Mark Herwege - Implement sitemap registry
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
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

    private @NonNullByDefault({}) ProxyServletService service;

    private @Mock @NonNullByDefault({}) ItemUIRegistry itemUIRegistryMock;
    private @Mock @NonNullByDefault({}) HttpService httpServiceMock;
    private @Mock @NonNullByDefault({}) SitemapRegistry sitemapRegistryMock;
    private @Mock @NonNullByDefault({}) Sitemap sitemapMock;
    private @Mock @NonNullByDefault({}) HttpServletRequest requestMock;
    private @Mock @NonNullByDefault({}) Switch switchWidgetMock;
    private @Mock @NonNullByDefault({}) Image imageWidgetMock;
    private @Mock @NonNullByDefault({}) Video videoWidgetMock;

    @BeforeEach
    public void setUp() {
        service = new ProxyServletService(httpServiceMock, itemUIRegistryMock, sitemapRegistryMock, Map.of());

        sitemapMock = mock(Sitemap.class);
        when(sitemapRegistryMock.get(eq(SITEMAP_NAME))).thenReturn(sitemapMock);

        when(itemUIRegistryMock.getWidget(eq(sitemapMock), eq(SWITCH_WIDGET_ID))).thenReturn(switchWidgetMock);
        when(itemUIRegistryMock.getWidget(eq(sitemapMock), eq(IMAGE_WIDGET_ID))).thenReturn(imageWidgetMock);
        when(itemUIRegistryMock.getWidget(eq(sitemapMock), eq(VIDEO_WIDGET_ID))).thenReturn(videoWidgetMock);

        when(itemUIRegistryMock.getItemState(eq(ITEM_NAME_UNDEF_STATE))).thenReturn(UnDefType.UNDEF);
        when(itemUIRegistryMock.getItemState(eq(ITEM_NAME_NULL_STATE))).thenReturn(UnDefType.NULL);
        when(itemUIRegistryMock.getItemState(eq(ITEM_NAME_ON_STATE))).thenReturn(OnOffType.ON);
        when(itemUIRegistryMock.getItemState(eq(ITEM_NAME_INVALID_URL))).thenReturn(new StringType(INVALID_URL));
        when(itemUIRegistryMock.getItemState(eq(ITEM_NAME_VALID_IMAGE_URL)))
                .thenReturn(new StringType(ITEM_VALID_IMAGE_URL));
        when(itemUIRegistryMock.getItemState(eq(ITEM_NAME_VALID_VIDEO_URL)))
                .thenReturn(new StringType(ITEM_VALID_VIDEO_URL));

        when(requestMock.getParameter(eq("sitemap"))).thenReturn(SITEMAP_NAME);
    }

    @Test
    public void testMaybeAppendAuthHeaderWithFullCredentials() {
        Request request = mock(Request.class);
        URI uri = URI.create("http://testuser:testpassword@127.0.0.1:8080/content");
        service.maybeAppendAuthHeader(uri, request);
        verify(request).header(HttpHeader.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString("testuser:testpassword".getBytes()));
    }

    @Test
    public void testMaybeAppendAuthHeaderWithoutPassword() {
        Request request = mock(Request.class);
        URI uri = URI.create("http://testuser@127.0.0.1:8080/content");
        service.maybeAppendAuthHeader(uri, request);
        verify(request).header(HttpHeader.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString("testuser:".getBytes()));
    }

    @Test
    public void testMaybeAppendAuthHeaderWithoutCredentials() {
        Request request = mock(Request.class);
        URI uri = URI.create("http://127.0.0.1:8080/content");
        service.maybeAppendAuthHeader(uri, request);
        verify(request, never()).header(any(HttpHeader.class), anyString());
    }

    @Test
    public void testProxyUriUnexpectedWidgetType() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(SWITCH_WIDGET_ID);
        URI uri = service.uriFromRequest(requestMock);
        assertNull(uri);
    }

    @Test
    public void testProxyUriImageWithoutItemButValidUrl() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidgetMock.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidgetMock.getItem()).thenReturn(null);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testProxyUriImageWithoutItemAndInvalidUrl() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidgetMock.getUrl()).thenReturn(INVALID_URL);
        when(imageWidgetMock.getItem()).thenReturn(null);
        URI uri = service.uriFromRequest(requestMock);
        assertNull(uri);
    }

    @Test
    public void testProxyUriImageWithItemButUndefState() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidgetMock.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidgetMock.getItem()).thenReturn(ITEM_NAME_UNDEF_STATE);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testProxyUriImageWithItemButNullState() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidgetMock.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidgetMock.getItem()).thenReturn(ITEM_NAME_NULL_STATE);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testProxyUriImageWithItemButUnexpectedState() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidgetMock.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidgetMock.getItem()).thenReturn(ITEM_NAME_ON_STATE);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testProxyUriImageWithItemButStateWithInvalidUrl() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidgetMock.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidgetMock.getItem()).thenReturn(ITEM_NAME_INVALID_URL);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testProxyUriImageWithItemAndStateWithValidUrl() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidgetMock.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidgetMock.getItem()).thenReturn(ITEM_NAME_VALID_IMAGE_URL);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testMaxProxyThreadsPassedToJettyAsMaxThreads() {
        ProxyServletService svc = new ProxyServletService(httpServiceMock, itemUIRegistryMock, sitemapRegistryMock,
                Map.of("maxProxyThreads", "20", "allowedHosts", "openHAB.org", "groupMembersSorting", "NAME"));
        var props = svc.propsFromConfig(Map.of("maxProxyThreads", 20, "allowedHosts", "openHAB.org"),
                mock(javax.servlet.Servlet.class));
        assertEquals("20", props.get("maxThreads"));
        assertNull(props.get("maxProxyThreads"));
        assertNull(props.get("allowedHosts"));
    }

    @Test
    public void testMaxThreadsDefaultAndInvalidValues() {
        ProxyServletService svc = serviceWithAllowedHosts("");
        int expected = Math.max(8, Runtime.getRuntime().availableProcessors());
        for (Map<String, Object> config : java.util.List.<Map<String, Object>> of(Map.of(),
                Map.of("maxProxyThreads", "abc"), Map.of("maxProxyThreads", "0"), Map.of("maxProxyThreads", -3))) {
            var props = svc.propsFromConfig(config, mock(javax.servlet.Servlet.class));
            assertEquals(String.valueOf(expected), props.get("maxThreads"));
        }
    }

    private ProxyServletService serviceWithAllowedHosts(String allowedHosts) {
        return new ProxyServletService(httpServiceMock, itemUIRegistryMock, sitemapRegistryMock,
                Map.of("allowedHosts", allowedHosts));
    }

    private @Nullable URI imageUriWithItemState(ProxyServletService svc, String state) {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(IMAGE_WIDGET_ID);
        when(imageWidgetMock.getUrl()).thenReturn(VALID_IMAGE_URL);
        when(imageWidgetMock.getItem()).thenReturn(ITEM_NAME_VALID_IMAGE_URL);
        when(itemUIRegistryMock.getItemState(eq(ITEM_NAME_VALID_IMAGE_URL))).thenReturn(new StringType(state));
        return svc.uriFromRequest(requestMock);
    }

    @Test
    public void testItemStateUrlUsedWhenHostAllowed() {
        URI uri = imageUriWithItemState(serviceWithAllowedHosts("openHAB.org"), ITEM_VALID_IMAGE_URL);
        assertNotNull(uri);
        assertEquals(ITEM_VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testItemStateUrlUsedWhenHostAndPortAllowed() {
        URI uri = imageUriWithItemState(serviceWithAllowedHosts("other.local, 192.168.1.20:8080"),
                "http://192.168.1.20:8080/cam.jpg");
        assertNotNull(uri);
        assertEquals("http://192.168.1.20:8080/cam.jpg", uri.toString());
    }

    @Test
    public void testItemStateUrlIgnoredWhenPortNotAllowed() {
        URI uri = imageUriWithItemState(serviceWithAllowedHosts("192.168.1.20:8080"),
                "http://192.168.1.20:9090/cam.jpg");
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testItemStateUrlIgnoredWhenHostNotAllowed() {
        URI uri = imageUriWithItemState(serviceWithAllowedHosts("openHAB.org"), "http://169.254.169.254/latest");
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testItemStateUrlIgnoredWhenContainingCredentials() {
        URI uri = imageUriWithItemState(serviceWithAllowedHosts("openHAB.org"), "https://user:pw@openhab.org/x.jpg");
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testItemStateUrlIgnoredForNonHttpScheme() {
        URI uri = imageUriWithItemState(serviceWithAllowedHosts("openHAB.org"), "file://openhab.org/etc/passwd");
        assertNotNull(uri);
        assertEquals(VALID_IMAGE_URL, uri.toString());
    }

    @Test
    public void testEmptyAllowedHostsLogsOnceAndUsesSitemapUrl() {
        Logger logger = (Logger) LoggerFactory.getLogger(ProxyServletService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            URI firstUri = imageUriWithItemState(service, ITEM_VALID_IMAGE_URL);
            URI secondUri = imageUriWithItemState(service, ITEM_VALID_IMAGE_URL);

            assertNotNull(firstUri);
            assertEquals(VALID_IMAGE_URL, firstUri.toString());
            assertNotNull(secondUri);
            assertEquals(VALID_IMAGE_URL, secondUri.toString());
            assertEquals(1,
                    appender.list.stream()
                            .filter(event -> event.getLevel().equals(Level.WARN)
                                    && event.getFormattedMessage().contains("URLs from item states are disabled"))
                            .count());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    public void testProxyUriVideoWithoutItemButValidUrl() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidgetMock.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidgetMock.getItem()).thenReturn(null);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_VIDEO_URL, uri.toString());
    }

    @Test
    public void testProxyUriVideoWithoutItemAndInvalidUrl() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidgetMock.getUrl()).thenReturn(INVALID_URL);
        when(videoWidgetMock.getItem()).thenReturn(null);
        URI uri = service.uriFromRequest(requestMock);
        assertNull(uri);
    }

    @Test
    public void testProxyUriVideoWithItemButUndefState() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidgetMock.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidgetMock.getItem()).thenReturn(ITEM_NAME_UNDEF_STATE);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_VIDEO_URL, uri.toString());
    }

    @Test
    public void testProxyUriVideoWithItemButNullState() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidgetMock.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidgetMock.getItem()).thenReturn(ITEM_NAME_NULL_STATE);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_VIDEO_URL, uri.toString());
    }

    @Test
    public void testProxyUriVideoWithItemButUnexpectedState() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidgetMock.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidgetMock.getItem()).thenReturn(ITEM_NAME_ON_STATE);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_VIDEO_URL, uri.toString());
    }

    @Test
    public void testProxyUriVideoWithItemButStateWithInvalidUrl() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidgetMock.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidgetMock.getItem()).thenReturn(ITEM_NAME_INVALID_URL);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_VIDEO_URL, uri.toString());
    }

    @Test
    public void testProxyUriVideoWithItemAndStateWithValidUrl() {
        when(requestMock.getParameter(eq("widgetId"))).thenReturn(VIDEO_WIDGET_ID);
        when(videoWidgetMock.getUrl()).thenReturn(VALID_VIDEO_URL);
        when(videoWidgetMock.getItem()).thenReturn(ITEM_NAME_VALID_VIDEO_URL);
        URI uri = service.uriFromRequest(requestMock);
        assertNotNull(uri);
        assertEquals(VALID_VIDEO_URL, uri.toString());
    }
}
