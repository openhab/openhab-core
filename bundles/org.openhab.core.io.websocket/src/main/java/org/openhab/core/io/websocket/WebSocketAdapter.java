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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;

/**
 * The {@link WebSocketAdapter} can be implemented to register an adapter for a websocket connection.
 * It will be accessible on the path /ws/ADAPTER_ID of your server.
 * Security is handled by the {@link CommonWebSocketServlet}.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public interface WebSocketAdapter {
    /**
     * The adapter id.
     * In combination with the base path {@link CommonWebSocketServlet#SERVLET_PATH} defines the adapter path.
     * 
     * @return the adapter id.
     */
    String getId();

    /**
     * Creates a websocket instance.
     * It should use the {@code org.eclipse.jetty.websocket.api.annotations} or implement
     * {@link org.eclipse.jetty.websocket.api.WebSocketListener}.
     * 
     * @return a websocket instance.
     */
    Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse);
}
