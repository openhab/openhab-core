/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.openhab.core.io.net.http.TrustManagerProvider;

/**
 * @author Kai Kreuzer - Initial contribution
 */
public class WebClientFactoryImplTest {

    private WebClientFactoryImpl webClientFactory;

    private static final String TEST_URL = "https://www.eclipse.org/";

    @Mock
    private TrustManagerProvider trustmanagerProvider;

    @Mock
    private ExtensibleTrustManagerImpl extensibleTrustManager;

    @Before
    public void setup() {
        initMocks(this);
        webClientFactory = new WebClientFactoryImpl(extensibleTrustManager);
        webClientFactory.setTrustmanagerProvider(trustmanagerProvider);

        webClientFactory.activate(createConfigMap(4, 200, 60, 2, 10, 60));
    }

    @After
    public void tearDown() {
        webClientFactory.deactivate();
    }

    @Test
    public void testGetClients() throws Exception {
        HttpClient httpClient = webClientFactory.getCommonHttpClient();
        WebSocketClient webSocketClient = webClientFactory.getCommonWebSocketClient();

        assertThat(httpClient, is(notNullValue()));
        assertThat(webSocketClient, is(notNullValue()));
    }

    @Test
    @Ignore("connecting to the outside world makes this test flaky")
    public void testCommonClientUsesExtensibleTrustManager() throws Exception {
        ArgumentCaptor<X509Certificate[]> certificateChainCaptor = ArgumentCaptor.forClass(X509Certificate[].class);
        ArgumentCaptor<SSLEngine> sslEngineCaptor = ArgumentCaptor.forClass(SSLEngine.class);
        HttpClient httpClient = webClientFactory.getCommonHttpClient();

        ContentResponse response = httpClient.GET(TEST_URL);
        if (response.getStatus() != 200) {
            fail("Statuscode != 200");
        }

        verify(extensibleTrustManager).checkServerTrusted(certificateChainCaptor.capture(), anyString(),
                sslEngineCaptor.capture());
        verifyNoMoreInteractions(extensibleTrustManager);
        assertThat(sslEngineCaptor.getValue().getPeerHost(), is("www.eclipse.org"));
        assertThat(sslEngineCaptor.getValue().getPeerPort(), is(443));
        assertThat(certificateChainCaptor.getValue()[0].getSubjectX500Principal().getName(),
                containsString("eclipse.org"));
    }

    @Test
    public void testGetHttpClientWithEndpoint() throws Exception {
        when(trustmanagerProvider.getTrustManagers("https://www.heise.de")).thenReturn(Stream.empty());

        HttpClient httpClient = webClientFactory.createHttpClient("consumer", TEST_URL);

        assertThat(httpClient, is(notNullValue()));
        verify(trustmanagerProvider).getTrustManagers(TEST_URL);
        httpClient.stop();
    }

    @Test
    public void testGetWebSocketClientWithEndpoint() throws Exception {
        when(trustmanagerProvider.getTrustManagers("https://www.heise.de")).thenReturn(Stream.empty());

        WebSocketClient webSocketClient = webClientFactory.createWebSocketClient("consumer", TEST_URL);

        assertThat(webSocketClient, is(notNullValue()));
        verify(trustmanagerProvider).getTrustManagers(TEST_URL);
        webSocketClient.stop();
    }

    @Ignore("connecting to the outside world makes this test flaky")
    @Test(expected = SSLHandshakeException.class)
    public void testCommonClientUsesExtensibleTrustManagerFailure() throws Throwable {
        doThrow(new CertificateException()).when(extensibleTrustManager).checkServerTrusted(
                ArgumentMatchers.any(X509Certificate[].class), anyString(), ArgumentMatchers.any(SSLEngine.class));
        HttpClient httpClient = webClientFactory.getCommonHttpClient();

        try {
            httpClient.GET(TEST_URL);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Ignore("only for manual test")
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

    @Ignore("only for manual test")
    @Test
    public void testMultiThreadedCustom() throws Exception {
        ThreadPoolExecutor workers = new ThreadPoolExecutor(20, 80, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50 * 50));

        final List<HttpClient> clients = new ArrayList<>();

        final int maxClients = 2;
        final int maxRequests = 2;

        for (int i = 0; i < maxClients; i++) {
            HttpClient httpClient = webClientFactory.createHttpClient("consumer" + i, "https://www.heise.de");
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
