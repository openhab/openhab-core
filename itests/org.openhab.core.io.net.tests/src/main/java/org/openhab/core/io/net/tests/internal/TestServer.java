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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Embedded jetty server used in the tests.
 *
 * Based on {@code TestServer} of the FS Internet Radio Binding.
 *
 * @author Velin Yordanov - Initial contribution
 * @author Wouter Born - Increase test coverage
 * @author Andrew Fiddian-Green - Adapted for org.openhab.core.io.net.tests
 */
@NonNullByDefault
public class TestServer {
    private final String host;
    private final int port;
    private final int timeout;
    private final Server server;

    public TestServer(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.server = new Server();
    }

    public void startServer() throws Exception {
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(new ServletHolder(new TestHttpServlet()), "/http1");
        handler.addServletWithMapping(new ServletHolder(new TestHttpServlet()), "/http2");
        handler.addServletWithMapping(new ServletHolder(new TestWebSocketServlet()), "/ws");
        server.setHandler(handler);

        HttpConfiguration httpConfig = new HttpConfiguration();
        HttpConnectionFactory h1 = new HttpConnectionFactory(httpConfig);
        HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

        ServerConnector connector = new ServerConnector(server, h1, h2c);
        connector.setHost(host);
        connector.setPort(port);
        connector.setIdleTimeout(timeout);
        server.addConnector(connector);

        server.start();
    }

    public void stopServer() {
        try {
            server.stop();
        } catch (Exception e) {
        }
    }
}
