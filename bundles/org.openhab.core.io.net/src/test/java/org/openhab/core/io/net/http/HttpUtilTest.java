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
package org.openhab.core.io.net.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpAuthenticationStore;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Tests for the HttpUtil
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Martin van Wingerden - Added tests based on HttpClientFactory
 */
@NonNullByDefault
public class HttpUtilTest extends BaseHttpUtilTest {

    @Test
    public void baseTest() throws Exception {
        mockResponse(HttpStatus.OK_200);

        String result = HttpUtil.executeUrl("GET", URL, 500);

        assertEquals("Some content", result);

        verify(httpClientMock).newRequest(URI.create(URL));
        verify(requestMock).method(HttpMethod.GET);
        verify(requestMock).timeout(500, TimeUnit.MILLISECONDS);
        verify(requestMock).send();
    }

    @Test
    public void testAuthentication() throws Exception {
        when(httpClientMock.newRequest(URI.create("http://john:doe@example.org/"))).thenReturn(requestMock);
        mockResponse(HttpStatus.OK_200);

        String result = HttpUtil.executeUrl("GET", "http://john:doe@example.org/", 500);

        assertEquals("Some content", result);

        verify(requestMock).header(HttpHeader.AUTHORIZATION, "Basic am9objpkb2U=");
    }

    @Test
    public void testCreateHttpMethod() {
        assertEquals(HttpMethod.GET, HttpUtil.createHttpMethod("GET"));
        assertEquals(HttpMethod.PUT, HttpUtil.createHttpMethod("PUT"));
        assertEquals(HttpMethod.POST, HttpUtil.createHttpMethod("POST"));
        assertEquals(HttpMethod.PATCH, HttpUtil.createHttpMethod("PATCH"));
        assertEquals(HttpMethod.DELETE, HttpUtil.createHttpMethod("DELETE"));
    }

    @Test
    public void testCreateHttpMethodForUnsupportedFake() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> HttpUtil.createHttpMethod("FAKE"));
        assertEquals("Given HTTP Method 'FAKE' is unknown", exception.getMessage());
    }

    @Test
    public void testCreateHttpMethodForUnsupportedTrace() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> HttpUtil.createHttpMethod("TRACE"));
        assertEquals("Given HTTP Method 'TRACE' is unknown", exception.getMessage());
    }

    @Test
    public void testPatchWithContent() throws Exception {
        mockResponse(HttpStatus.OK_200);
        String payload = "test payload";
        InputStream contentStream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));

        String result = HttpUtil.executeUrl("PATCH", URL, contentStream, "text/plain", 500);

        assertEquals("Some content", result);

        verify(httpClientMock).newRequest(URI.create(URL));
        verify(requestMock).method(HttpMethod.PATCH);
        verify(requestMock).timeout(500, TimeUnit.MILLISECONDS);
        verify(requestMock).content(any(), eq("text/plain"));
        verify(requestMock).send();
    }

    @Test
    public void proxyRequestsDoNotTouchTheCommonHttpClient() throws Exception {
        mockResponse(HttpStatus.OK_200);
        HttpClient proxyClientMock = mock(HttpClient.class);
        ProxyConfiguration proxyConfigMock = new ProxyConfiguration();
        when(clientFactoryMock.createHttpClient(any())).thenReturn(proxyClientMock);
        when(proxyClientMock.getProxyConfiguration()).thenReturn(proxyConfigMock);
        when(proxyClientMock.getAuthenticationStore()).thenReturn(new HttpAuthenticationStore());
        when(proxyClientMock.newRequest(URI.create(URL))).thenReturn(requestMock);

        HttpUtil.executeUrl("GET", URL, null, null, null, 500, "proxy.example.org", 8080, "user", "secret", null);

        // the shared client must neither be used nor modified
        verify(clientFactoryMock, never()).getCommonHttpClient();
        verify(httpClientMock, never()).getProxyConfiguration();
        verify(httpClientMock, never()).getAuthenticationStore();
        assertEquals(1, proxyConfigMock.getProxies().size());
    }

    @Test
    public void requestsWithoutProxyUseTheCommonHttpClient() throws Exception {
        mockResponse(HttpStatus.OK_200);

        HttpUtil.executeUrl("GET", URL, 500);

        verify(clientFactoryMock).getCommonHttpClient();
        verify(clientFactoryMock, never()).createHttpClient(any());
    }
}
