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

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.util.Promise.Completable;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.io.net.http.WebSocketFactory;
import org.openhab.core.io.net.tests.internal.TestServer;
import org.openhab.core.io.net.tests.internal.TestStreamAdapter;
import org.openhab.core.io.net.tests.internal.TestWebSocket;
import org.openhab.core.test.TestPortUtil;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * Tests for HttpClientFactory and WebSocketFactory implementations.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class ClientFactoryTest extends JavaOSGiTest {
    public static final String RESPONSE = "response";

    private static final String CONSUMER = "consumer";
    private static final String HOST = "127.0.0.1";
    private static final int WAIT_SECONDS = 10;
    private static final long CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(WAIT_SECONDS);

    private static @Nullable TestServer server;

    @AfterAll
    public static void afterAll() throws Exception {
        TestServer testServer = server;
        if (testServer != null) {
            testServer.stopServer();
        }
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        TestServer testServer = new TestServer(HOST, TestPortUtil.findFreePort());
        testServer.startServer();
        server = testServer;
    }

    private TestServer getServer() {
        return Objects.requireNonNull(server);
    }

    @Test
    public void testHttp1Client() throws Exception {
        HttpClientFactory httpClientFactory = getService(HttpClientFactory.class);
        assertNotNull(httpClientFactory);

        HttpClient client = httpClientFactory.createHttpClient(CONSUMER);
        assertNotNull(client);

        try {
            // start client
            client.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            client.start();

            // send request
            ContentResponse response = client.GET(getServer().getHttpUri());

            // check response
            assertEquals(HttpStatus.OK_200, response.getStatus());
            assertEquals(RESPONSE, response.getContentAsString());
        } finally {
            try {
                // stop client
                client.stop();
            } catch (Exception e) {
            }
        }
    }

    @Test
    public void testHttp2Client() throws Exception {
        HttpClientFactory httpClientFactory = getService(HttpClientFactory.class);
        assertNotNull(httpClientFactory);

        HTTP2Client client = httpClientFactory.createHttp2Client(CONSUMER);
        assertNotNull(client);

        Completable<@Nullable Session> sessionCompletable = new Completable<>();
        Completable<@Nullable Stream> streamCompletable = new Completable<>();

        TestStreamAdapter streamAdapter = new TestStreamAdapter();

        MetaData.Request request = new MetaData.Request(HttpMethod.GET.toString(),
                new HttpURI(getServer().getHttpUri()), HttpVersion.HTTP_2, new HttpFields());

        HeadersFrame headers = new HeadersFrame(request, null, true);

        try {
            // start client
            client.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            client.start();

            // establish session
            client.connect(new InetSocketAddress(getServer().getHost(), getServer().getPort()),
                    new Session.Listener.Adapter(), sessionCompletable);
            Session session = sessionCompletable.get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(session);
            assertFalse(session.isClosed());

            // open stream
            session.newStream(headers, streamCompletable, streamAdapter);
            Stream stream = streamCompletable.get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(stream);

            // check response
            String response = streamAdapter.completable.get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertEquals(RESPONSE, response);
        } finally {
            try {
                // stop client
                client.stop();
            } catch (Exception e) {
            }
        }
    }

    @Test
    public void testWebSocketClient() throws Exception {
        WebSocketFactory webSocketFactory = getService(WebSocketFactory.class);
        assertNotNull(webSocketFactory);

        WebSocketClient client = webSocketFactory.createWebSocketClient(CONSUMER);
        assertNotNull(client);

        try {
            // start client
            client.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            client.start();

            // create session future
            TestWebSocket webSocket = new TestWebSocket();
            Future<org.eclipse.jetty.websocket.api.Session> sessionFuture = client.connect(webSocket,
                    getServer().getWebSocketUri());
            assertNotNull(sessionFuture);

            // check response
            String response = webSocket.completable.get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertEquals(RESPONSE, response);
        } finally {
            try {
                // stop client
                client.stop();
            } catch (Exception e) {
            }
        }
    }
}
