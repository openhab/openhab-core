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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpAuthenticationStore;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the HttpUtil
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Martin van Wingerden - Added tests based on HttpClientFactory
 */
@NonNullByDefault
public class HttpUtilTest extends BaseHttpUtilTest {

    private static final int MAX_PROXY_CLIENTS = 8;

    @AfterEach
    public void releaseProxyClients() {
        // proxy clients are cached in a static map, do not leak them into other tests
        new HttpUtil().unsetHttpClientFactory(clientFactoryMock);
    }

    private HttpClient mockProxyClient() throws Exception {
        HttpClient proxyClientMock = mock(HttpClient.class);
        when(proxyClientMock.getProxyConfiguration()).thenReturn(new ProxyConfiguration());
        when(proxyClientMock.getAuthenticationStore()).thenReturn(new HttpAuthenticationStore());
        when(proxyClientMock.newRequest(URI.create(URL))).thenReturn(requestMock);
        return proxyClientMock;
    }

    private void executeViaProxy(int proxyPort) throws Exception {
        HttpUtil.executeUrl("GET", URL, null, null, null, 500, "proxy.example.org", proxyPort, null, null, null);
    }

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

    @Test
    public void proxyClientIsReusedForTheSameProxyConfiguration() throws Exception {
        mockResponse(HttpStatus.OK_200);
        HttpClient proxyClientMock = mockProxyClient();
        when(clientFactoryMock.createHttpClient(any())).thenReturn(proxyClientMock);

        executeViaProxy(8080);
        executeViaProxy(8080);

        verify(clientFactoryMock, times(1)).createHttpClient(any());
    }

    @Test
    public void unbindingTheFactoryStopsProxyClientsAndDropsTheCache() throws Exception {
        mockResponse(HttpStatus.OK_200);
        HttpClient first = mockProxyClient();
        HttpClient second = mockProxyClient();
        when(clientFactoryMock.createHttpClient(any())).thenReturn(first, second);

        executeViaProxy(8080);
        new HttpUtil().unsetHttpClientFactory(clientFactoryMock);

        verify(first).stop();
        // no factory, no client
        assertThrows(IllegalStateException.class, () -> executeViaProxy(8080));

        // after binding again a new client is created, the stopped one is never handed out again
        new HttpUtil().setHttpClientFactory(clientFactoryMock);
        executeViaProxy(8080);
        verify(clientFactoryMock, times(2)).createHttpClient(any());
        verify(second, never()).stop();
    }

    @Test
    public void leastRecentlyUsedProxyClientIsRetiredWhenThereAreTooMany() throws Exception {
        mockResponse(HttpStatus.OK_200);
        HttpClient[] clients = new HttpClient[MAX_PROXY_CLIENTS + 1];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = mockProxyClient();
        }
        when(clientFactoryMock.createHttpClient(any())).thenReturn(clients[0],
                java.util.Arrays.copyOfRange(clients, 1, clients.length));

        for (int i = 0; i < clients.length; i++) {
            executeViaProxy(8000 + i);
        }

        verify(clients[0]).stop();
        for (int i = 1; i < clients.length; i++) {
            verify(clients[i], never()).stop();
        }
    }

    @Test
    public void retiredProxyClientIsStoppedOnlyAfterItsRequestHasFinished() throws Exception {
        mockResponse(HttpStatus.OK_200);
        HttpClient proxyClientMock = mockProxyClient();
        when(clientFactoryMock.createHttpClient(any())).thenReturn(proxyClientMock);

        CountDownLatch requestStarted = new CountDownLatch(1);
        CountDownLatch finishRequest = new CountDownLatch(1);
        when(requestMock.send()).thenAnswer(invocation -> {
            requestStarted.countDown();
            assertTrue(finishRequest.await(5, TimeUnit.SECONDS));
            return contentResponseMock;
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> request = executor.submit(() -> {
                executeViaProxy(8080);
                return null;
            });
            assertTrue(requestStarted.await(5, TimeUnit.SECONDS));

            // unbinding while the request is in flight must not pull the client away from it
            new HttpUtil().unsetHttpClientFactory(clientFactoryMock);
            verify(proxyClientMock, never()).stop();

            finishRequest.countDown();
            request.get(5, TimeUnit.SECONDS);
            verify(proxyClientMock, timeout(5000)).stop();
        } finally {
            finishRequest.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void proxyClientCreatedWhileUnbindingIsNotLeftBehind() throws Exception {
        mockResponse(HttpStatus.OK_200);
        HttpClient proxyClientMock = mockProxyClient();
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch continueCreating = new CountDownLatch(1);
        when(clientFactoryMock.createHttpClient(any())).thenAnswer(invocation -> {
            creating.countDown();
            assertTrue(continueCreating.await(5, TimeUnit.SECONDS));
            return proxyClientMock;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> request = executor.submit(() -> {
                executeViaProxy(8080);
                return null;
            });
            assertTrue(creating.await(5, TimeUnit.SECONDS));

            // the factory goes away while a client is being created for it
            Future<?> unbind = executor.submit(() -> new HttpUtil().unsetHttpClientFactory(clientFactoryMock));
            continueCreating.countDown();
            request.get(5, TimeUnit.SECONDS);
            unbind.get(5, TimeUnit.SECONDS);

            // whichever side won, the client must not stay cached for a later factory
            verify(proxyClientMock, timeout(5000)).stop();
            new HttpUtil().setHttpClientFactory(clientFactoryMock);
            HttpClient other = mockProxyClient();
            when(clientFactoryMock.createHttpClient(any())).thenReturn(other);
            executeViaProxy(8080);
            verify(other, never()).stop();
            verify(clientFactoryMock, times(2)).createHttpClient(any());
        } finally {
            continueCreating.countDown();
            executor.shutdownNow();
        }
    }
}
