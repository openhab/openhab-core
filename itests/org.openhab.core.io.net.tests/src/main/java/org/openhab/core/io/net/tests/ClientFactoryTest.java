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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Promise.Completable;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.io.net.http.WebSocketFactory;
import org.openhab.core.io.net.tests.internal.ClientFactoryTestServer;
import org.openhab.core.io.net.tests.internal.ClientFactoryTestServlet;
import org.openhab.core.test.TestPortUtil;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * Tests for HttpClientFactory and WebSocketFactory implementations.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class ClientFactoryTest extends JavaOSGiTest {

    private static class StreamAdapter extends Stream.Listener.Adapter {
        public final CompletableFuture<String> completable = new CompletableFuture<>();

        @Override
        public void onData(@Nullable Stream stream, @Nullable DataFrame frame, @Nullable Callback callback) {
            assertNotNull(stream);
            assertNotNull(frame);
            assertTrue(frame.isEndStream());
            completable.complete(StandardCharsets.UTF_8.decode(frame.getData()).toString());
        }
    }

    private static final String HOST = "127.0.0.1";
    private static final int TIMEOUT = -1;
    private static final String CONSUMER = "testConsumer";

    private static @Nullable ClientFactoryTestServer server;
    private static String serverUrl = "";
    private static int serverPort;

    @AfterAll
    public static void afterAll() {
        if (server != null) {
            server.stopServer();
        }
    }

    @BeforeAll
    public static void beforeAll() {
        serverPort = TestPortUtil.findFreePort();
        serverUrl = "http://" + HOST + ":" + serverPort;
        server = new ClientFactoryTestServer(HOST, serverPort, TIMEOUT,
                new ServletHolder(new ClientFactoryTestServlet()));
        server.startServer();
    }

    @Test
    public void testHttp1Client() {
        HttpClientFactory httpClientFactory = getService(HttpClientFactory.class);
        assertNotNull(httpClientFactory);

        HttpClient client = httpClientFactory.createHttpClient(CONSUMER);
        assertNotNull(client);

        try {
            // start client
            client.start();

            // send request
            ContentResponse response = client.GET(serverUrl);
            assertEquals(HttpStatus.OK_200, response.getStatus());
            assertEquals(ClientFactoryTestServlet.RESPONSE, response.getContentAsString());

            // stop client
            client.stop();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Disabled("TODO waiting on dependencies on Jetty http2.hpack and Jetty http2.server")
    @Test
    public void testHttp2Client() {
        HttpClientFactory httpClientFactory = getService(HttpClientFactory.class);
        assertNotNull(httpClientFactory);

        HTTP2Client client = httpClientFactory.createHttp2Client(CONSUMER);
        assertNotNull(client);

        Completable<@Nullable Session> sessionCompletable = new Completable<>();
        Completable<@Nullable Stream> streamCompletable = new Completable<>();

        StreamAdapter streamAdapter = new StreamAdapter();
        InetSocketAddress address = new InetSocketAddress(HOST, serverPort);
        MetaData.Request request = new MetaData.Request(HttpMethod.GET.toString(), new HttpURI(serverUrl),
                HttpVersion.HTTP_2, null);

        HeadersFrame headers = new HeadersFrame(request, null, true);

        try {
            // start client
            client.start();

            // establish the session
            client.connect(address, null, sessionCompletable);
            Session session = sessionCompletable.get(1, TimeUnit.SECONDS);
            assertNotNull(session);

            // open a stream
            session.newStream(headers, streamCompletable, streamAdapter);
            Stream stream = streamCompletable.get(1, TimeUnit.SECONDS);
            assertNotNull(stream);

            // wait for the response
            String response = streamAdapter.completable.get(1, TimeUnit.SECONDS);
            assertEquals(ClientFactoryTestServlet.RESPONSE, response);

            // stop client
            client.stop();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWebSocketClient() {
        WebSocketFactory webSocketFactory = getService(WebSocketFactory.class);
        assertNotNull(webSocketFactory);

        WebSocketClient client = webSocketFactory.createWebSocketClient(CONSUMER);
        assertNotNull(client);

        try {
            // stop client
            client.start();

            // TODO run the tests

            // stop client
            client.stop();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
