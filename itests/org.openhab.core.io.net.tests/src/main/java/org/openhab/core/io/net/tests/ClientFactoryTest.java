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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodName.class)
public class ClientFactoryTest extends JavaOSGiTest {

    private static final String CONSUMER = "consumer";
    public static final String RESPONSE = "response";

    private static final String HOST = "127.0.0.1";
    private static final int TIMEOUT = -1;
    private static final int WAIT_SECONDS = 10;

    private static int port;
    private static String wsUrl = "";
    private static String httpUrl = "";
    private static String http2Url = "";

    private static @Nullable TestServer server;

    // synchronization objects for multi- thread testing
    private static final Completable<Boolean> SERVER_RUNNING = new Completable<>();
    private static final Completable<Boolean> HTTP_TEST_DONE = new Completable<>();
    private static final Completable<Boolean> HTTP2_TEST_DONE = new Completable<>();
    private static final Completable<Boolean> WS_TEST_DONE = new Completable<>();
    private static final Object THREADED_TEST_PIPELINE = new Object();

    @AfterAll
    public static void afterAll() throws Exception {
        HTTP_TEST_DONE.get(WAIT_SECONDS, TimeUnit.SECONDS);
        HTTP2_TEST_DONE.get(WAIT_SECONDS, TimeUnit.SECONDS);
        WS_TEST_DONE.get(WAIT_SECONDS, TimeUnit.SECONDS);

        TestServer theServer = server;
        if (theServer != null) {
            theServer.stopServer();
        }
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        port = TestPortUtil.findFreePort();
        wsUrl = "ws://" + HOST + ":" + port + "/ws";
        httpUrl = "http://" + HOST + ":" + port + "/http1";
        http2Url = "http://" + HOST + ":" + port + "/http2";
        TestServer theServer = new TestServer(HOST, port, TIMEOUT);
        theServer.startServer();
        server = theServer;

        SERVER_RUNNING.complete(true);
    }

    @Test
    public void testHttp1Client() throws Exception {
        SERVER_RUNNING.get(WAIT_SECONDS, TimeUnit.SECONDS);
        synchronized (THREADED_TEST_PIPELINE) {
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
        HTTP_TEST_DONE.complete(true);
    }

    @Test
    public void testHttp2Client() throws Exception {
        SERVER_RUNNING.get(WAIT_SECONDS, TimeUnit.SECONDS);
        synchronized (THREADED_TEST_PIPELINE) {
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
        HTTP2_TEST_DONE.complete(true);
    }

    @Test
    public void testWebSocketClient() throws Exception {
        SERVER_RUNNING.get(WAIT_SECONDS, TimeUnit.SECONDS);
        synchronized (THREADED_TEST_PIPELINE) {
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
        WS_TEST_DONE.complete(true);
    }
}
