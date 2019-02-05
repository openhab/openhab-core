/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.audio.internal;

import static org.junit.Assert.fail;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.audio.ByteArrayAudioStream;
import org.eclipse.smarthome.core.audio.FixedLengthAudioStream;
import org.eclipse.smarthome.test.TestPortUtil;
import org.eclipse.smarthome.test.TestServer;
import org.eclipse.smarthome.test.java.JavaTest;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for tests using the {@link AudioServlet}.
 *
 * @author Henning Treu - initial contribution
 *
 */
public abstract class AbstractAudioServeltTest extends JavaTest {

    protected AudioServlet audioServlet;

    private int port;
    private TestServer server;

    private final static String AUDIO_SERVLET_PROTOCOL = "http";
    private final static String AUDIO_SERVLET_HOSTNAME = "localhost";

    private CompletableFuture<Boolean> serverStarted;

    private HttpClient httpClient;

    @Before
    public void setupServerAndClient() {
        audioServlet = new AudioServlet();

        ServletHolder servletHolder = new ServletHolder(audioServlet);

        port = TestPortUtil.findFreePort();
        server = new TestServer(AUDIO_SERVLET_HOSTNAME, port, 10000, servletHolder);
        serverStarted = server.startServer();

        httpClient = new HttpClient();
    }

    @After
    public void tearDownServerAndClient() throws Exception {
        server.stopServer();
        httpClient.stop();
    }

    protected ByteArrayAudioStream getByteArrayAudioStream(byte[] byteArray, String container, String codec) {
        int bitDepth = 16;
        int bitRate = 1000;
        long frequency = 16384;

        AudioFormat audioFormat = new AudioFormat(container, codec, true, bitDepth, bitRate, frequency);

        return new ByteArrayAudioStream(byteArray, audioFormat);
    }

    protected ContentResponse getHttpResponse(AudioStream audioStream) throws Exception {
        String url = serveStream(audioStream);
        return getHttpRequest(url).send();
    }

    protected String serveStream(AudioStream stream) throws Exception {
        return serveStream(stream, null);
    }

    protected void startHttpClient(HttpClient client) {
        if (!client.isStarted()) {
            try {
                client.start();
            } catch (Exception e) {
                fail("An exception " + e + " was thrown, while starting the HTTP client");
            }
        }
    }

    protected Request getHttpRequest(String url) {
        startHttpClient(httpClient);
        return httpClient.newRequest(url).method(HttpMethod.GET);
    }

    protected String serveStream(AudioStream stream, Integer timeInterval) throws Exception {
        serverStarted.get(); // wait for the server thread to be started

        String path;
        if (timeInterval != null) {
            path = audioServlet.serve((FixedLengthAudioStream) stream, timeInterval);
        } else {
            path = audioServlet.serve(stream);
        }

        return generateURL(AUDIO_SERVLET_PROTOCOL, AUDIO_SERVLET_HOSTNAME, port, path);
    }

    private String generateURL(String protocol, String hostname, int port, String path) {
        return String.format("%s://%s:%s%s", protocol, hostname, port, path);
    }

}
