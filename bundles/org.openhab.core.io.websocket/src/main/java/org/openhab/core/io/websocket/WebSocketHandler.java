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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;

/**
 * The {@link WebSocketHandler} can be implemented to register a handler for a websocket connection.
 * It will be accessible on the path /ws/HANDLER_ID of your server.
 * Security is handled by the {@link CommonWebSocketServlet}.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
public interface WebSocketHandler {
    /**
     * The handler id.
     * In combination with the base path {@link CommonWebSocketServlet#SERVLET_PATH} defines the handler path.
     * 
     * @return the handler id.
     */
    String getId();

    /**
     * Creates a websocket connection handler.
     * It should use the {@link org.eclipse.jetty.websocket.api.annotations} or implement
     * {@link org.eclipse.jetty.websocket.api.WebSocketListener}.
     * 
     * @return a websocket instance.
     */
    Object createWebSocket(@Nullable ServletUpgradeRequest servletUpgradeRequest,
            @Nullable ServletUpgradeResponse servletUpgradeResponse);
}
