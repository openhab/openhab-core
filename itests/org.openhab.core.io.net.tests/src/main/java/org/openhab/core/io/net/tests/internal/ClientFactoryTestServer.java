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

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded jetty server used in the tests.
 *
 * Based on {@code TestServer} of the FS Internet Radio Binding.
 *
 * @author Velin Yordanov - Initial contribution
 * @author Wouter Born - Increase test coverage
 * @author Andrew Fiddian-Green - Copied to org.openhab.core.io.net
 */
public class ClientFactoryTestServer {
    private final Logger logger = LoggerFactory.getLogger(ClientFactoryTestServer.class);

    private Server server;
    private String host;
    private int port;
    private int timeout;
    private ServletHolder servletHolder;

    public ClientFactoryTestServer(String host, int port, int timeout, ServletHolder servletHolder) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.servletHolder = servletHolder;
    }

    public void startServer() {
        Thread thread = new Thread(new Runnable() {
            @Override
            @SuppressWarnings("resource")
            public void run() {
                server = new Server();
                ServletHandler handler = new ServletHandler();
                handler.addServletWithMapping(servletHolder, "/*");
                server.setHandler(handler);

                HttpConfiguration httpConfig = new HttpConfiguration();

                HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);
                /*
                 * TODO waiting on dependency on Jetty http2.server
                 * HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);
                 * ServerConnector connector = new ServerConnector(server, http11, h2c);
                 */
                ServerConnector connector = new ServerConnector(server, http11);
                connector.setHost(host);
                connector.setPort(port);
                connector.setIdleTimeout(timeout);

                server.addConnector(connector);

                try {
                    server.start();
                    server.join();
                } catch (InterruptedException ex) {
                    logger.error("Server got interrupted", ex);
                    return;
                } catch (Exception e) {
                    logger.error("Error in starting the server", e);
                    return;
                }
            }
        });

        thread.start();
    }

    public void stopServer() {
        try {
            server.stop();
        } catch (Exception e) {
            logger.error("Error in stopping the server", e);
            return;
        }
    }
}
