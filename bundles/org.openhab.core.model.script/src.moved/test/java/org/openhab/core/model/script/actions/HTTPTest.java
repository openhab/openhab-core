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
package org.openhab.core.model.script.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openhab.core.io.net.http.HttpUtil;

/**
 * Unit tests for {@link HTTP} actions, covering GET, PUT, POST, PATCH, DELETE, and error handling.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
class HTTPTest {

    private @Nullable MockedStatic<HttpUtil> httpUtilMock;

    @BeforeEach
    void setUp() {
        httpUtilMock = mockStatic(HttpUtil.class);
    }

    @AfterEach
    void tearDown() {
        httpUtilMock.close();
    }

    // --- GET Tests ---

    @Test
    void testSendHttpGetRequestBasic() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("GET"), eq("http://example.com"), eq(5000)))
                .thenReturn("get-response");

        String response = HTTP.sendHttpGetRequest("http://example.com");

        assertEquals("get-response", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl("GET", "http://example.com", 5000));
    }

    @Test
    void testSendHttpGetRequestWithTimeout() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("GET"), eq("http://example.com"), eq(3000)))
                .thenReturn("get-timeout");

        String response = HTTP.sendHttpGetRequest("http://example.com", 3000);

        assertEquals("get-timeout", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl("GET", "http://example.com", 3000));
    }

    @Test
    void testSendHttpGetRequestWithHeadersAndTimeout() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("GET"), eq("http://example.com"), any(Properties.class),
                eq(null), eq(null), eq(2000))).thenReturn("get-headers");

        Map<String, String> headers = Map.of("X-Test", "Value");
        String response = HTTP.sendHttpGetRequest("http://example.com", headers, 2000);

        assertEquals("get-headers", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl(eq("GET"), eq("http://example.com"), any(Properties.class),
                eq(null), eq(null), eq(2000)));
    }

    // --- PUT Tests ---

    @Test
    void testSendHttpPutRequestBasic() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("PUT"), eq("http://example.com"), eq(1000)))
                .thenReturn("put-response");

        String response = HTTP.sendHttpPutRequest("http://example.com");

        assertEquals("put-response", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl("PUT", "http://example.com", 1000));
    }

    @Test
    void testSendHttpPutRequestWithContentAndTimeout() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("PUT"), eq("http://example.com"), any(InputStream.class),
                eq("text/plain"), eq(2000))).thenReturn("put-content");

        String response = HTTP.sendHttpPutRequest("http://example.com", "text/plain", "hello", 2000);

        assertEquals("put-content", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl(eq("PUT"), eq("http://example.com"), any(InputStream.class),
                eq("text/plain"), eq(2000)));
    }

    // --- POST Tests ---

    @Test
    void testSendHttpPostRequestBasic() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("POST"), eq("http://example.com"), eq(1000)))
                .thenReturn("post-response");

        String response = HTTP.sendHttpPostRequest("http://example.com");

        assertEquals("post-response", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl("POST", "http://example.com", 1000));
    }

    @Test
    void testSendHttpPostRequestWithContentHeadersAndTimeout() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("POST"), eq("http://example.com"), any(Properties.class),
                any(InputStream.class), eq("application/json"), eq(3000))).thenReturn("post-full");

        Map<String, String> headers = Map.of("Authorization", "Bearer token");
        String response = HTTP.sendHttpPostRequest("http://example.com", "application/json", "{\"a\":1}", headers,
                3000);

        assertEquals("post-full", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl(eq("POST"), eq("http://example.com"), any(Properties.class),
                any(InputStream.class), eq("application/json"), eq(3000)));
    }

    // --- PATCH Tests ---

    @Test
    void testSendHttpPatchRequestBasic() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("PATCH"), eq("http://example.com"), eq(1000)))
                .thenReturn("patch-response");

        String response = HTTP.sendHttpPatchRequest("http://example.com");

        assertEquals("patch-response", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl("PATCH", "http://example.com", 1000));
    }

    @Test
    void testSendHttpPatchRequestWithContentAndHeaders() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("PATCH"), eq("http://example.com"), any(Properties.class),
                any(InputStream.class), eq("application/json"), eq(2000))).thenReturn("patch-full");

        Map<String, String> headers = Map.of("X-Custom", "Header");
        String response = HTTP.sendHttpPatchRequest("http://example.com", "application/json", "content", headers, 2000);

        assertEquals("patch-full", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl(eq("PATCH"), eq("http://example.com"), any(Properties.class),
                any(InputStream.class), eq("application/json"), eq(2000)));
    }

    // --- DELETE Tests ---

    @Test
    void testSendHttpDeleteRequestBasic() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(eq("DELETE"), eq("http://example.com"), eq(1000)))
                .thenReturn("delete-response");

        String response = HTTP.sendHttpDeleteRequest("http://example.com");

        assertEquals("delete-response", response);
        httpUtilMock.verify(() -> HttpUtil.executeUrl("DELETE", "http://example.com", 1000));
    }

    // --- Error Handling Tests ---

    @Test
    void testRequestHandlesIOExceptionGracefully() throws Exception {
        httpUtilMock.when(() -> HttpUtil.executeUrl(anyString(), anyString(), anyInt()))
                .thenThrow(new IOException("Connection refused"));

        String response = HTTP.sendHttpGetRequest("http://example.com");

        assertNull(response);
    }
}
