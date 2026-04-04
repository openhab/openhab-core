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
package org.openhab.core.io.websocket;

import java.io.IOException;
import java.io.Serial;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.auth.AuthFilter;
import org.openhab.core.io.websocket.event.EventWebSocketAdapter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;

/**
 * The {@link CommonWebSocketServlet} provides the servlet for WebSocket connections.
 *
 * <p>
 * Clients can authorize in two ways:
 * <ul>
 * <li>By setting <code>org.openhab.ws.accessToken.base64.</code> + base64-encoded access token and the
 * {@link CommonWebSocketServlet#WEBSOCKET_PROTOCOL_DEFAULT} in the <code>Sec-WebSocket-Protocol</code> header.</li>
 * <li>By providing the access token as query parameter <code>accessToken</code>.</li>
 * </ul>
 *
 * @author Jan N. Klug - Initial contribution
 * @author Miguel Álvarez Díez - Refactor into a common servlet
 * @author Florian Hotze - Support passing access token through Sec-WebSocket-Protocol header
 */
@NonNullByDefault
@HttpWhiteboardServletName(CommonWebSocketServlet.SERVLET_PATH)
@HttpWhiteboardServletPattern(CommonWebSocketServlet.SERVLET_PATH + "/*")
@Component(immediate = true, service = { Servlet.class })
public class CommonWebSocketServlet extends JettyWebSocketServlet {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String SEC_WEBSOCKET_PROTOCOL_HEADER = "Sec-WebSocket-Protocol";
    public static final String WEBSOCKET_PROTOCOL_DEFAULT = "org.openhab.ws.protocol.default";
    private static final Pattern WEBSOCKET_ACCESS_TOKEN_PATTERN = Pattern
            .compile("org.openhab.ws.accessToken.base64.(?<base64>[A-Za-z0-9+/]*)");

    public static final String SERVLET_PATH = "/ws";

    public static final String DEFAULT_ADAPTER_ID = EventWebSocketAdapter.ADAPTER_ID;

    private final Map<String, WebSocketAdapter> connectionHandlers = new ConcurrentHashMap<>();
    private final AuthFilter authFilter;

    @Activate
    public CommonWebSocketServlet(@Reference AuthFilter authFilter) throws ServletException {
        this.authFilter = authFilter;
    }

    @Override
    public void configure(@NonNullByDefault({}) JettyWebSocketServletFactory webSocketServletFactory) {
        webSocketServletFactory.setIdleTimeout(Duration.ofMillis(10000));
        webSocketServletFactory.setCreator(new CommonWebSocketCreator());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addWebSocketAdapter(WebSocketAdapter wsAdapter) {
        this.connectionHandlers.put(wsAdapter.getId(), wsAdapter);
    }

    protected void removeWebSocketAdapter(WebSocketAdapter wsAdapter) {
        this.connectionHandlers.remove(wsAdapter.getId());
    }

    private class CommonWebSocketCreator implements JettyWebSocketCreator {
        private final Logger logger = LoggerFactory.getLogger(CommonWebSocketCreator.class);

        @Override
        public @Nullable Object createWebSocket(@Nullable JettyServerUpgradeRequest servletUpgradeRequest,
                @Nullable JettyServerUpgradeResponse servletUpgradeResponse) {
            if (servletUpgradeRequest == null || servletUpgradeResponse == null) {
                return null;
            }

            String accessToken = null;
            String secWebSocketProtocolHeader = servletUpgradeRequest.getHeader(SEC_WEBSOCKET_PROTOCOL_HEADER);
            if (secWebSocketProtocolHeader != null) { // if the client sends the Sec-WebSocket-Protocol header
                // respond with the default protocol
                servletUpgradeResponse.setHeader(SEC_WEBSOCKET_PROTOCOL_HEADER, WEBSOCKET_PROTOCOL_DEFAULT);
                // extract the base64 encoded access token from the requested protocols
                Matcher matcher = WEBSOCKET_ACCESS_TOKEN_PATTERN.matcher(secWebSocketProtocolHeader);
                if (matcher.find() && matcher.group("base64") != null) {
                    String base64 = matcher.group("base64");
                    try {
                        accessToken = new String(Base64.getDecoder().decode(base64));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid base64 encoded access token in Sec-WebSocket-Protocol header from {}.",
                                servletUpgradeRequest.getRemoteSocketAddress());
                        return null;
                    }
                } else {
                    logger.warn("Invalid use of Sec-WebSocket-Protocol header from {}.",
                            servletUpgradeRequest.getRemoteSocketAddress());
                    return null;
                }
            }

            if (accessToken != null ? isAuthorizedRequest(accessToken) : isAuthorizedRequest(servletUpgradeRequest)) {
                String requestPath = servletUpgradeRequest.getRequestURI().getPath();
                String pathPrefix = SERVLET_PATH + "/";
                boolean useDefaultAdapter = requestPath.equals(pathPrefix) || !requestPath.startsWith(pathPrefix);
                WebSocketAdapter wsAdapter;
                if (!useDefaultAdapter) {
                    String adapterId = requestPath.substring(pathPrefix.length());
                    wsAdapter = connectionHandlers.get(adapterId);
                    if (wsAdapter == null) {
                        logger.warn("Missing WebSocket adapter for path {}", adapterId);
                        return null;
                    }
                } else {
                    wsAdapter = connectionHandlers.get(DEFAULT_ADAPTER_ID);
                    if (wsAdapter == null) {
                        logger.warn("Default WebSocket adapter is missing");
                        return null;
                    }
                }
                logger.debug("New connection handled by {}", wsAdapter.getId());
                return wsAdapter.createWebSocket(servletUpgradeRequest, servletUpgradeResponse);
            } else {
                logger.warn("Unauthenticated request to create a websocket from {}.",
                        servletUpgradeRequest.getRemoteSocketAddress());
            }
            return null;
        }

        private boolean isAuthorizedRequest(String bearerToken) {
            try {
                var securityContext = authFilter.getSecurityContext(bearerToken);
                return securityContext != null
                        && (securityContext.isUserInRole(Role.USER) || securityContext.isUserInRole(Role.ADMIN));
            } catch (AuthenticationException e) {
                logger.warn("Error handling WebSocket authorization", e);
                return false;
            }
        }

        private boolean isAuthorizedRequest(JettyServerUpgradeRequest servletUpgradeRequest) {
            try {
                var securityContext = authFilter.getSecurityContext(servletUpgradeRequest.getHttpServletRequest(),
                        true);
                return securityContext != null
                        && (securityContext.isUserInRole(Role.USER) || securityContext.isUserInRole(Role.ADMIN));
            } catch (AuthenticationException | IOException e) {
                logger.warn("Error handling WebSocket authorization", e);
                return false;
            }
        }
    }
}
