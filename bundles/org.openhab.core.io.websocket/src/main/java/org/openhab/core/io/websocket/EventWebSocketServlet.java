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

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserApiTokenCredentials;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.ItemRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link EventWebSocketServlet} provides the servlet for WebSocket connections
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@HttpWhiteboardServletName(EventWebSocketServlet.SERVLET_PATH)
@HttpWhiteboardServletPattern(EventWebSocketServlet.SERVLET_PATH + "/*")
@Component(immediate = true, service = { EventSubscriber.class, Servlet.class })
public class EventWebSocketServlet extends WebSocketServlet implements EventSubscriber {
    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/ws";
    private final Gson gson = new Gson();
    private final UserRegistry userRegistry;
    private final EventPublisher eventPublisher;

    private final ItemEventUtility itemEventUtility;
    private final Set<EventWebSocket> webSockets = new CopyOnWriteArraySet<>();

    @SuppressWarnings("unused")
    private @Nullable WebSocketServerFactory importNeeded;

    @Activate
    public EventWebSocketServlet(@Reference UserRegistry userRegistry, @Reference EventPublisher eventPublisher,
            @Reference ItemRegistry itemRegistry) throws ServletException, NamespaceException {
        this.userRegistry = userRegistry;
        this.eventPublisher = eventPublisher;

        itemEventUtility = new ItemEventUtility(gson, itemRegistry);
    }

    @Override
    public void configure(@NonNullByDefault({}) WebSocketServletFactory webSocketServletFactory) {
        webSocketServletFactory.getPolicy().setIdleTimeout(10000);
        webSocketServletFactory.setCreator(new EventWebSocketCreator());
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(EventSubscriber.ALL_EVENT_TYPES);
    }

    @Override
    public void receive(Event event) {
        webSockets.forEach(ws -> ws.processEvent(event));
    }

    public void registerListener(EventWebSocket eventWebSocket) {
        webSockets.add(eventWebSocket);
    }

    public void unregisterListener(EventWebSocket eventWebSocket) {
        webSockets.remove(eventWebSocket);
    }

    private class EventWebSocketCreator implements WebSocketCreator {
        private static final String API_TOKEN_PREFIX = "oh.";

        private final Logger logger = LoggerFactory.getLogger(EventWebSocketCreator.class);

        @Override
        public @Nullable Object createWebSocket(@Nullable ServletUpgradeRequest servletUpgradeRequest,
                @Nullable ServletUpgradeResponse servletUpgradeResponse) {
            if (servletUpgradeRequest == null) {
                return null;
            }

            Map<String, List<String>> parameterMap = servletUpgradeRequest.getParameterMap();
            List<String> accessToken = parameterMap.getOrDefault("accessToken", List.of());
            if (accessToken.size() == 1 && authenticateAccessToken(accessToken.get(0))) {
                return new EventWebSocket(gson, EventWebSocketServlet.this, itemEventUtility, eventPublisher);
            } else {
                logger.warn("Unauthenticated request to create a websocket from {}.",
                        servletUpgradeRequest.getRemoteAddress());
            }

            return null;
        }

        private boolean authenticateAccessToken(String token) {
            Credentials credentials = null;
            if (token.startsWith(API_TOKEN_PREFIX)) {
                credentials = new UserApiTokenCredentials(token);
            } else {
                // try BasicAuthentication
                String[] decodedParts = new String(Base64.getDecoder().decode(token)).split(":");
                if (decodedParts.length == 2) {
                    credentials = new UsernamePasswordCredentials(decodedParts[0], decodedParts[1]);
                }
            }

            if (credentials != null) {
                try {
                    Authentication auth = userRegistry.authenticate(credentials);
                    User user = userRegistry.get(auth.getUsername());
                    return user != null
                            && (user.getRoles().contains(Role.USER) || user.getRoles().contains(Role.ADMIN));
                } catch (AuthenticationException ignored) {
                }
            }

            return false;
        }
    }
}
