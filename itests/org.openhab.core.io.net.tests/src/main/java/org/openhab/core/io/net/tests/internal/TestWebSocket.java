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
package org.openhab.core.io.net.tests.internal;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * WebSocket implementer class for org.openhab.core.io.net tests.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@WebSocket
@NonNullByDefault
public class TestWebSocket {
    public final CompletableFuture<String> completable = new CompletableFuture<>();

    @OnWebSocketMessage
    public void onMessage(String message) {
        completable.complete(message);
    }
}
