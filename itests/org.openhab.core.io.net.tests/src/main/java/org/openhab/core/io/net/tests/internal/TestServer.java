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

import java.net.URI;

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
    private static final String SERVLET_PATH = "/servlet";
    private static final String WEBSOCKET_PATH = "/ws";

    private final String host;
    private final int port;
    private final Server server;

    public TestServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.server = new Server();
    }

    public String getHost() {
        return host;
    }

    public URI getHttpUri() {
        return URI.create("http://" + host + ":" + port + SERVLET_PATH);
    }

    public int getPort() {
        return port;
    }

    public URI getWebSocketUri() {
        return URI.create("ws://" + host + ":" + port + WEBSOCKET_PATH);
    }

    public void startServer() throws Exception {
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(new ServletHolder(new TestHttpServlet()), SERVLET_PATH);
        handler.addServletWithMapping(new ServletHolder(new TestWebSocketServlet()), WEBSOCKET_PATH);
        server.setHandler(handler);

        HttpConfiguration httpConfig = new HttpConfiguration();
        HttpConnectionFactory h1 = new HttpConnectionFactory(httpConfig);
        HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

        ServerConnector connector = new ServerConnector(server, h1, h2c);
        connector.setHost(host);
        connector.setPort(port);
        server.addConnector(connector);

        server.start();
    }

    public void stopServer() throws Exception {
        server.stop();
    }
}
