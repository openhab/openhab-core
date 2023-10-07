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
package org.openhab.core.io.websocket;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.ServletException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.io.rest.auth.AnonymousUserSecurityContext;
import org.openhab.core.io.rest.auth.AuthFilter;
import org.osgi.service.http.NamespaceException;

/**
 * The {@link CommonWebSocketServletTest} contains tests for the {@link EventWebSocket}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CommonWebSocketServletTest {
    private final String testAdapterId = "test-adapter-id";

    private @NonNullByDefault({}) CommonWebSocketServlet servlet;
    private @Mock @NonNullByDefault({}) AuthFilter authFilter;
    private @Mock @NonNullByDefault({}) WebSocketServletFactory factory;
    private @Mock @NonNullByDefault({}) WebSocketAdapter testDefaultWsAdapter;
    private @Mock @NonNullByDefault({}) WebSocketAdapter testWsAdapter;

    private @Mock @NonNullByDefault({}) WebSocketPolicy wsPolicy;
    private @Mock @NonNullByDefault({}) ServletUpgradeRequest request;
    private @Mock @NonNullByDefault({}) ServletUpgradeResponse response;
    private @Captor @NonNullByDefault({}) ArgumentCaptor<WebSocketCreator> webSocketCreatorAC;

    @BeforeEach
    public void setup() throws ServletException, NamespaceException, AuthenticationException, IOException {
        servlet = new CommonWebSocketServlet(authFilter);
        when(factory.getPolicy()).thenReturn(wsPolicy);
        servlet.configure(factory);
        verify(factory).setCreator(webSocketCreatorAC.capture());
        when(request.getParameterMap()).thenReturn(Map.of());
        when(authFilter.getSecurityContext(any(), anyBoolean())).thenReturn(new AnonymousUserSecurityContext());
        when(testDefaultWsAdapter.getId()).thenReturn(CommonWebSocketServlet.DEFAULT_ADAPTER_ID);
        when(testWsAdapter.getId()).thenReturn(testAdapterId);
        servlet.addWebSocketAdapter(testDefaultWsAdapter);
        servlet.addWebSocketAdapter(testWsAdapter);
    }

    @Test
    public void createWebsocketUsingDefaultAdapterPath() throws URISyntaxException {
        when(request.getRequestURI()).thenReturn(new URI("http://127.0.0.1:8080/ws"));
        webSocketCreatorAC.getValue().createWebSocket(request, response);
        verify(testDefaultWsAdapter, times(1)).createWebSocket(request, response);
    }

    @Test
    public void createWebsocketUsingAdapterPath() throws URISyntaxException {
        when(request.getRequestURI()).thenReturn(new URI("http://127.0.0.1:8080/ws/" + testAdapterId));
        webSocketCreatorAC.getValue().createWebSocket(request, response);
        verify(testWsAdapter, times(1)).createWebSocket(request, response);
    }
}
