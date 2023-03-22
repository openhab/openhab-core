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
import java.net.URI;
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
import org.junit.jupiter.api.Test;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.io.net.http.WebSocketFactory;
import org.openhab.core.io.net.tests.internal.TestServer;
import org.openhab.core.io.net.tests.internal.TestStreamAdapter;
import org.openhab.core.io.net.tests.internal.TestWebSocket;
import org.openhab.core.test.TestPortUtil;
import org.openhab.core.test.java.JavaOSGiTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for HttpClientFactory and WebSocketFactory implementations.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class ClientFactoryTest extends JavaOSGiTest {

    private static final String CONSUMER = "consumer";
    public static final String RESPONSE = "response";

    private static final String HOST = "127.0.0.1";
    private static final int WAIT_SECONDS = 10;

    private final Logger logger = LoggerFactory.getLogger(ClientFactoryTest.class);

    private int port;
    private String wsUrl = "";
    private String httpUrl = "";
    private String http2Url = "";

    private static @Nullable TestServer server;

    @Test
    public void pipelineAllMethods() throws Exception {
        logger.warn("Test started");
        beforeAll();
        logger.warn("Server initialized");
        testHttp1Client();
        logger.warn("Http1 test completed");
        testHttp2Client();
        logger.warn("Http2 test completed");
        testWebSocketClient();
        logger.warn("WebSocket test completed");
        afterAll();
        logger.warn("No errors");
    }

    private void afterAll() throws Exception {
        TestServer theServer = server;
        if (theServer != null) {
            theServer.stopServer();
        }
    }

    private void beforeAll() throws Exception {
        port = TestPortUtil.findFreePort();
        wsUrl = "ws://" + HOST + ":" + port + "/ws";
        httpUrl = "http://" + HOST + ":" + port + "/http1";
        http2Url = "http://" + HOST + ":" + port + "/http2";
        TestServer theServer = new TestServer(HOST, port);
        theServer.startServer();
        server = theServer;
    }

    private void testHttp1Client() throws Exception {
        HttpClientFactory httpClientFactory = getService(HttpClientFactory.class);
        assertNotNull(httpClientFactory);

        HttpClient client = httpClientFactory.createHttpClient(CONSUMER);
        // HttpClient client = new HttpClient();
        assertNotNull(client);

        try {
            // start client
            client.setConnectTimeout(WAIT_SECONDS * 1000);
            client.start();

            // send request
            ContentResponse response = client.GET(httpUrl);

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

    private void testHttp2Client() throws Exception {
        HttpClientFactory httpClientFactory = getService(HttpClientFactory.class);
        assertNotNull(httpClientFactory);

        HTTP2Client client = httpClientFactory.createHttp2Client(CONSUMER);
        // HTTP2Client client = new HTTP2Client();
        assertNotNull(client);

        // initialize address
        InetSocketAddress address = new InetSocketAddress(HOST, port);

        Completable<@Nullable Session> sessionCompletable = new Completable<>();
        Completable<@Nullable Stream> streamCompletable = new Completable<>();

        TestStreamAdapter streamAdapter = new TestStreamAdapter();

        MetaData.Request request = new MetaData.Request(HttpMethod.GET.toString(), new HttpURI(http2Url),
                HttpVersion.HTTP_2, new HttpFields());

        HeadersFrame headers = new HeadersFrame(request, null, true);

        try {
            // start client
            client.setConnectTimeout(WAIT_SECONDS * 1000);
            client.start();

            // establish session
            client.connect(address, new Session.Listener.Adapter(), sessionCompletable);
            Session session = sessionCompletable.get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(session);
            assertFalse(session.isClosed());

            // open stream
            session.newStream(headers, streamCompletable, streamAdapter);
            Stream stream = streamCompletable.get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(stream);

            // confirm stream is open
            assertFalse(stream.isClosed());
            assertFalse(stream.isReset());

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

    private void testWebSocketClient() throws Exception {
        WebSocketFactory webSocketFactory = getService(WebSocketFactory.class);
        assertNotNull(webSocketFactory);

        WebSocketClient client = webSocketFactory.createWebSocketClient(CONSUMER);
        // WebSocketClient client = new WebSocketClient();
        assertNotNull(client);

        try {
            // start client
            client.setConnectTimeout(WAIT_SECONDS * 1000);
            client.start();

            // initialize address
            URI address = new URI(wsUrl);

            // establish session
            Future<org.eclipse.jetty.websocket.api.Session> session;
            TestWebSocket webSocket = new TestWebSocket();
            session = client.connect(webSocket, address);
            assertNotNull(session);

            // confirm session is open
            assertFalse(session.isDone());
            assertFalse(session.isCancelled());

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
