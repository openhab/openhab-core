/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.test;

import java.util.concurrent.CompletableFuture;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded jetty server used in the tests.
 *
 * @author Velin Yordanov - Initial contribution
 * @author Henning Treu - provide in base test bundle
 */
public class TestServer {
    private final Logger logger = LoggerFactory.getLogger(TestServer.class);

    private Server server;
    private final String host;
    private final int port;
    private final int timeout;
    private final ServletHolder servletHolder;

    /**
     * Creates a new {@link TestServer}. The server is started by {@link #startServer()} and stopped by
     * {@link #stopServer()}, preferably in the tests setup & tearDown methods.
     *
     * @param host the host this server runs on.
     * @param port the port this server runs on. Use {@link TestPortUtil} to find a random free port.
     * @param timeout the idle timeout when receiving new messages on a connection in milliseconds.
     * @param servletHolder a {@link ServletHolder} which holds the {@link Servlet} content will be served from.
     */
    public TestServer(String host, int port, int timeout, ServletHolder servletHolder) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.servletHolder = servletHolder;
    }

    /**
     * Starts the server and returns a {@link CompletableFuture}. The {@link CompletableFuture} gets completed as soon
     * as the server is ready to accept connections.
     *
     * @return a {@link CompletableFuture} which completes as soon as the server is ready to accept connections.
     */
    public CompletableFuture<Boolean> startServer() {
        final CompletableFuture<Boolean> serverStarted = new CompletableFuture<>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                server = new Server();
                ServletHandler handler = new ServletHandler();
                handler.addServletWithMapping(servletHolder, "/*");
                server.setHandler(handler);

                // HTTP connector
                ServerConnector http = new ServerConnector(server);
                http.setHost(host);
                http.setPort(port);
                http.setIdleTimeout(timeout);

                server.addConnector(http);

                try {
                    server.start();
                    serverStarted.complete(true);
                    server.join();
                } catch (InterruptedException ex) {
                    logger.error("Server got interrupted", ex);
                    serverStarted.completeExceptionally(ex);
                    return;
                } catch (Exception e) {
                    logger.error("Error in starting the server", e);
                    serverStarted.completeExceptionally(e);
                    return;
                }
            }
        });

        thread.start();

        return serverStarted;
    }

    /**
     * Stops the server.
     */
    public void stopServer() {
        try {
            server.stop();
        } catch (Exception e) {
            logger.error("Error in stopping the server", e);
            return;
        }
    }
}
