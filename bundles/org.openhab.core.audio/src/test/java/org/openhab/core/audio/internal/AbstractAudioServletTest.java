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
package org.openhab.core.audio.internal;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.ByteArrayAudioStream;
import org.openhab.core.audio.FixedLengthAudioStream;
import org.openhab.core.test.TestPortUtil;
import org.openhab.core.test.TestServer;
import org.openhab.core.test.java.JavaTest;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Base class for tests using the {@link AudioServlet}.
 *
 * @author Henning Treu - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public abstract class AbstractAudioServletTest extends JavaTest {

    private static final String AUDIO_SERVLET_PROTOCOL = "http";
    private static final String AUDIO_SERVLET_HOSTNAME = "localhost";

    protected @NonNullByDefault({}) AudioServlet audioServlet;

    private int port;
    private @NonNullByDefault({}) TestServer server;
    private @NonNullByDefault({}) HttpClient httpClient;
    private @NonNullByDefault({}) CompletableFuture<Boolean> serverStarted;

    private @Mock @NonNullByDefault({}) HttpService httpServiceMock;
    private @Mock @NonNullByDefault({}) HttpContext httpContextMock;

    @BeforeEach
    public void setupServerAndClient() {
        audioServlet = new AudioServlet();

        ServletHolder servletHolder = new ServletHolder(audioServlet);

        port = TestPortUtil.findFreePort();
        server = new TestServer(AUDIO_SERVLET_HOSTNAME, port, 10000, servletHolder);
        serverStarted = server.startServer();

        httpClient = new HttpClient();
    }

    @AfterEach
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

    protected ContentResponse getHttpResponseWithAccept(AudioStream audioStream, String acceptHeader) throws Exception {
        String url = serveStream(audioStream);
        return getHttpRequest(url).header("Accept", acceptHeader).send();
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

    protected String serveStream(AudioStream stream, @Nullable Integer timeInterval) throws Exception {
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
