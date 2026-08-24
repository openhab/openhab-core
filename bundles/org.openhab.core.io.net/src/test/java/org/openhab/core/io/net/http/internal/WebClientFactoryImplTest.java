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
package org.openhab.core.io.net.http.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Kai Kreuzer - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class WebClientFactoryImplTest {

    private @NonNullByDefault({}) WebClientFactoryImpl webClientFactory;

    private static final String TEST_URL = "https://www.eclipse.org/";

    private @Mock @NonNullByDefault({}) ExtensibleTrustManagerImpl extensibleTrustManagerMock;

    @BeforeEach
    public void setup() {
        webClientFactory = new WebClientFactoryImpl(extensibleTrustManagerMock);
        webClientFactory.activate(createConfigMap(4, 200, 60, 2, 10, 60));
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        // Regression check for the former shutdown hang: stopping the common HTTP client used to
        // tear down the shared thread pool underneath the common WebSocketClient, whose selector
        // then died with a ClosedSelectorException and deactivate() blocked forever.
        // Deactivation must always complete.
        deactivateWithTimeout();
    }

    private void deactivateWithTimeout() throws InterruptedException {
        Thread deactivateThread = new Thread(() -> webClientFactory.deactivate(), "webClientFactory-deactivate");
        deactivateThread.setDaemon(true);
        deactivateThread.start();
        deactivateThread.join(10_000);

        // Capture the verdict before interrupting: the interrupt is cleanup, so an interrupt that
        // happens to unblock the deactivation must not turn an observed timeout into a pass.
        boolean timedOut = deactivateThread.isAlive();
        if (timedOut) {
            deactivateThread.interrupt();
        }
        assertThat("deactivate() did not complete", timedOut, is(false));
    }

    @Test
    public void testGetClients() throws Exception {
        HttpClient httpClient = webClientFactory.getCommonHttpClient();
        WebSocketClient webSocketClient = webClientFactory.getCommonWebSocketClient();

        assertThat(httpClient, is(notNullValue()));
        assertThat(webSocketClient, is(notNullValue()));
    }

    @Test
    public void testSharedThreadPoolIsUnmanagedAndStoppedByDeactivate() throws Exception {
        HttpClient httpClient = webClientFactory.getCommonHttpClient();
        WebSocketClient webSocketClient = webClientFactory.getCommonWebSocketClient();

        Executor executor = httpClient.getExecutor();
        assertThat(executor, is(instanceOf(QueuedThreadPool.class)));
        QueuedThreadPool sharedPool = (QueuedThreadPool) executor;

        // both clients use the same pool, but neither container manages its lifecycle
        assertThat(webSocketClient.getHttpClient().getExecutor(), is(sameInstance(executor)));
        assertThat(httpClient.isManaged(sharedPool), is(false));
        assertThat(webSocketClient.getHttpClient().isManaged(sharedPool), is(false));
        assertThat(sharedPool.isRunning(), is(true));

        // deactivation stops the pool itself, after both clients
        deactivateWithTimeout();
        assertThat(sharedPool.isRunning(), is(false));
    }

    @Disabled("connecting to the outside world makes this test flaky")
    @Test
    public void testCommonClientUsesExtensibleTrustManager() throws Exception {
        ArgumentCaptor<X509Certificate[]> certificateChainCaptor = ArgumentCaptor.forClass(X509Certificate[].class);
        ArgumentCaptor<SSLEngine> sslEngineCaptor = ArgumentCaptor.forClass(SSLEngine.class);
        HttpClient httpClient = webClientFactory.getCommonHttpClient();

        ContentResponse response = httpClient.GET(TEST_URL);
        if (response.getStatus() != 200) {
            fail("Statuscode != 200");
        }

        verify(extensibleTrustManagerMock).checkServerTrusted(certificateChainCaptor.capture(), anyString(),
                sslEngineCaptor.capture());
        verifyNoMoreInteractions(extensibleTrustManagerMock);
        assertThat(sslEngineCaptor.getValue().getPeerHost(), is("www.eclipse.org"));
        assertThat(sslEngineCaptor.getValue().getPeerPort(), is(443));
        assertThat(certificateChainCaptor.getValue()[0].getSubjectX500Principal().getName(),
                containsString("eclipse.org"));
    }

    @Disabled("connecting to the outside world makes this test flaky")
    @Test
    public void testCommonClientUsesExtensibleTrustManagerFailure() throws Exception {
        doThrow(new CertificateException()).when(extensibleTrustManagerMock).checkServerTrusted(
                ArgumentMatchers.any(X509Certificate[].class), anyString(), ArgumentMatchers.any(SSLEngine.class));
        HttpClient httpClient = webClientFactory.getCommonHttpClient();

        assertThrows(SSLHandshakeException.class, () -> {
            try {
                httpClient.GET(TEST_URL);
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        });
    }

    @Disabled("only for manual test")
    @Test
    public void testMultiThreadedShared() throws Exception {
        ThreadPoolExecutor workers = new ThreadPoolExecutor(20, 80, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50 * 50));

        final List<HttpClient> clients = new ArrayList<>();

        final int maxClients = 2;
        final int maxRequests = 2;

        for (int i = 0; i < maxClients; i++) {
            HttpClient httpClient = webClientFactory.getCommonHttpClient();
            clients.add(httpClient);
        }

        final List<String> failures = new ArrayList<>();

        for (int i = 0; i < maxRequests; i++) {
            for (final HttpClient client : clients) {
                workers.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ContentResponse response = client.GET(TEST_URL);
                            if (response.getStatus() != 200) {
                                failures.add("Statuscode != 200");
                            }
                        } catch (InterruptedException | ExecutionException | TimeoutException e) {
                            failures.add("Unexpected exception:" + e.getMessage());
                        }
                    }
                });
            }
        }

        workers.shutdown();
        workers.awaitTermination(120, TimeUnit.SECONDS);
        if (!failures.isEmpty()) {
            fail(failures.toString());
        }
    }

    @Disabled("only for manual test")
    @Test
    public void testMultiThreadedCustom() throws Exception {
        ThreadPoolExecutor workers = new ThreadPoolExecutor(20, 80, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50 * 50));

        final List<HttpClient> clients = new ArrayList<>();

        final int maxClients = 2;
        final int maxRequests = 2;

        for (int i = 0; i < maxClients; i++) {
            HttpClient httpClient = webClientFactory.createHttpClient("consumer" + i);
            clients.add(httpClient);
        }

        final List<String> failures = new ArrayList<>();

        for (int i = 0; i < maxRequests; i++) {
            for (final HttpClient client : clients) {
                workers.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ContentResponse response = client.GET(TEST_URL);
                            if (response.getStatus() != 200) {
                                failures.add("Statuscode != 200");
                            }
                        } catch (InterruptedException | ExecutionException | TimeoutException e) {
                            failures.add("Unexpected exception:" + e.getMessage());
                        }
                    }
                });
            }
        }

        workers.shutdown();
        workers.awaitTermination(120, TimeUnit.SECONDS);
        if (!failures.isEmpty()) {
            fail(failures.toString());
        }

        for (HttpClient client : clients) {
            client.stop();
        }
    }

    private Map<String, Object> createConfigMap(int minThreadsShared, int maxThreadsShared, int keepAliveTimeoutShared,
            int minThreadsCustom, int maxThreadsCustom, int keepAliveTimeoutCustom) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("minThreadsShared", minThreadsShared);
        configMap.put("maxThreadsShared", maxThreadsShared);
        configMap.put("keepAliveTimeoutShared", keepAliveTimeoutShared);
        configMap.put("minThreadsCustom", minThreadsCustom);
        configMap.put("maxThreadsCustom", maxThreadsCustom);
        configMap.put("keepAliveTimeoutCustom", keepAliveTimeoutCustom);
        return configMap;
    }
}
