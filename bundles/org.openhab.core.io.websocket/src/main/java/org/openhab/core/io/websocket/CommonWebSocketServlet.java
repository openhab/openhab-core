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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.Role;
import org.openhab.core.io.rest.auth.internal.AnonymousUserSecurityContext;
import org.openhab.core.io.rest.auth.internal.AuthFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CommonWebSocketServlet} provides the servlet for WebSocket connections
 *
 * @author Jan N. Klug - Initial contribution
 * @author Miguel Álvarez Díez - Refactor into a common servlet
 */
@NonNullByDefault
@HttpWhiteboardServletName(CommonWebSocketServlet.SERVLET_PATH)
@HttpWhiteboardServletPattern(CommonWebSocketServlet.SERVLET_PATH + "/*")
@Component(immediate = true, service = { Servlet.class })
public class CommonWebSocketServlet extends WebSocketServlet {
    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/ws";

    public static final String DEFAULT_HANDLER_ID = EventWebSocketHandler.HANDLER_ID;

    private final Map<String, WebSocketHandler> connectionHandlers = new HashMap<>();
    private final AuthFilter authFilter;

    @SuppressWarnings("unused")
    private @Nullable WebSocketServerFactory importNeeded;

    @Activate
    public CommonWebSocketServlet(@Reference AuthFilter authFilter) throws ServletException, NamespaceException {
        this.authFilter = authFilter;
    }

    @Override
    public void configure(@NonNullByDefault({}) WebSocketServletFactory webSocketServletFactory) {
        webSocketServletFactory.getPolicy().setIdleTimeout(10000);
        webSocketServletFactory.setCreator(new CommonWebSocketCreator());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addWebSocketHandler(WebSocketHandler wsConnectionHandler) {
        this.connectionHandlers.put(wsConnectionHandler.getId(), wsConnectionHandler);
    }

    protected void removeWebSocketHandler(WebSocketHandler wsConnectionHandler) {
        this.connectionHandlers.remove(wsConnectionHandler.getId());
    }

    private class CommonWebSocketCreator implements WebSocketCreator {
        private final Logger logger = LoggerFactory.getLogger(CommonWebSocketCreator.class);

        @Override
        public @Nullable Object createWebSocket(@Nullable ServletUpgradeRequest servletUpgradeRequest,
                @Nullable ServletUpgradeResponse servletUpgradeResponse) {
            if (servletUpgradeRequest == null) {
                return null;
            }
            if (isAuthorizedRequest(servletUpgradeRequest)) {
                String requestPath = servletUpgradeRequest.getRequestURI().getPath();
                String handlerPrefix = SERVLET_PATH + "/";
                boolean useDefaultHandler = requestPath.equals(handlerPrefix) || !requestPath.startsWith(handlerPrefix);
                WebSocketHandler wsHandler;
                if (!useDefaultHandler) {
                    String handlerId = requestPath.substring(handlerPrefix.length());
                    wsHandler = connectionHandlers.get(handlerId);
                    if (wsHandler == null) {
                        logger.warn("Missing WebSocket handler for path {}", handlerId);
                        return null;
                    }
                } else {
                    wsHandler = connectionHandlers.get(DEFAULT_HANDLER_ID);
                    if (wsHandler == null) {
                        logger.warn("Default WebSocket handler is missing");
                        return null;
                    }
                }
                logger.debug("New connection handled by {}", wsHandler.getId());
                return wsHandler.createWebSocket(servletUpgradeRequest, servletUpgradeResponse);
            } else {
                logger.warn("Unauthenticated request to create a websocket from {}.",
                        servletUpgradeRequest.getRemoteAddress());
            }
            return null;
        }

        private boolean isAuthorizedRequest(ServletUpgradeRequest servletUpgradeRequest) {
            Map<String, List<String>> parameterMap = servletUpgradeRequest.getParameterMap();
            List<String> accessToken = parameterMap.getOrDefault("accessToken", List.of());
            SecurityContext securityContext = null;
            if (accessToken.isEmpty() && authFilter.isImplicitUserRole(servletUpgradeRequest.getHttpServletRequest())) {
                securityContext = new AnonymousUserSecurityContext();
            } else if (accessToken.size() == 1) {
                String token = accessToken.get(0);
                try {
                    securityContext = authFilter.authenticateBearerToken(token);
                } catch (AuthenticationException ignored) {
                }
                if (securityContext == null && authFilter.isBasicAuthAllowed()) {
                    try {
                        securityContext = authFilter.authenticateBasicAuth(token);
                    } catch (AuthenticationException ignored) {
                    }
                }
            }
            return securityContext != null
                    && (securityContext.isUserInRole(Role.USER) || securityContext.isUserInRole(Role.ADMIN));
        }
    }
}
