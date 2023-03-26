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

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.openhab.core.io.net.tests.ClientFactoryTest;

/**
 * WebSocket for org.openhab.core.io.net tests.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class TestWebSocketAdapter extends WebSocketAdapter {

    @Override
    public void onWebSocketConnect(@Nullable Session session) {
        super.onWebSocketConnect(session);
        try {
            getRemote().sendString(ClientFactoryTest.RESPONSE);
        } catch (IOException e) {
        }
    }
}
