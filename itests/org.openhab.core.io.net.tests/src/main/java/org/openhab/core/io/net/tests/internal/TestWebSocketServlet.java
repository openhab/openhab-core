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
package org.openhab.core.io.net.tests.internal;

import java.io.Serial;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;

/**
 * Servlet for org.openhab.core.io.net tests.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class TestWebSocketServlet extends JettyWebSocketServlet {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void configure(@Nullable JettyWebSocketServletFactory factory) {
        factory.setCreator((req, resp) -> new TestWebSocketAdapter());
    }
}
