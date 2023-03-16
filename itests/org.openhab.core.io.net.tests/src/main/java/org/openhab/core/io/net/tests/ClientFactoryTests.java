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
package org.openhab.core.io.net.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.io.net.http.WebSocketFactory;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * Tests for HttpClientFactory and WebSocketFactory implementations.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
class ClientFactoryTests extends JavaOSGiTest {

    private static final String TEST_CONSUMER = "testConsumer";

    private @NonNullByDefault({}) HttpClientFactory httpClientFactory;
    private @NonNullByDefault({}) WebSocketFactory webSocketFactory;

    @BeforeAll
    public void initializeFactories() {
        httpClientFactory = getService(HttpClientFactory.class);
        assertNotNull(httpClientFactory);
        webSocketFactory = getService(WebSocketFactory.class);
        assertNotNull(webSocketFactory);
    }

    @Test
    public void testCreateHttp1Client() {
        HttpClient http1Client = httpClientFactory.createHttpClient(TEST_CONSUMER);
        assertNotNull(http1Client);
    }

    @Test
    public void testCreateHttp2Client() {
        HTTP2Client http2Client = httpClientFactory.createHttp2Client(TEST_CONSUMER);
        assertNotNull(http2Client);
    }

    @Test
    public void testCreateWebSocketClient() {
        WebSocketClient webSocketClient = webSocketFactory.createWebSocketClient(TEST_CONSUMER);
        assertNotNull(webSocketClient);
    }
}
