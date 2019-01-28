/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.ui.internal.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A blocking version of the proxy servlet that complies with Servlet API 2.4.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Svilen Valkanov - Replaced Apache HttpClient with Jetty
 * @author John Cocula - refactored to support alternate implementation
 */
public class BlockingProxyServlet extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(BlockingProxyServlet.class);

    private static final long serialVersionUID = -4716754591953017794L;

    private final ProxyServletService service;

    private static HttpClient httpClient = new HttpClient(new SslContextFactory());

    /** Timeout for HTTP requests in ms */
    private static final int TIMEOUT = 15000;

    BlockingProxyServlet(ProxyServletService service) {
        super();
        this.service = service;
        if (!httpClient.isStarted()) {
            try {
                httpClient.start();
            } catch (Exception e) {
                logger.warn("Cannot start HttpClient!", e);
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "Proxy (blocking)";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        URI uri = service.uriFromRequest(request);

        if (uri == null) {
            service.sendError(request, response);
        } else {
            Request httpRequest = httpClient.newRequest(uri);

            service.maybeAppendAuthHeader(uri, httpRequest);

            InputStreamResponseListener listener = new InputStreamResponseListener();

            // do the client request
            try {
                httpRequest.send(listener);
                // wait for the response headers to arrive or the timeout to expire
                Response httpResponse = listener.get(TIMEOUT, TimeUnit.MILLISECONDS);

                // get response headers
                HttpFields headers = httpResponse.getHeaders();
                Iterator<HttpField> iterator = headers.iterator();

                // copy all headers
                while (iterator.hasNext()) {
                    HttpField header = iterator.next();
                    response.setHeader(header.getName(), header.getValue());
                }
            } catch (Exception e) {
                if (e instanceof TimeoutException) {
                    logger.warn("Proxy servlet failed to stream content due to a timeout");
                    response.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT);
                } else {
                    logger.warn("Proxy servlet failed to stream content: {}", e.getMessage());
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                }
                return;
            }
            // now copy/stream the body content
            try (InputStream responseContent = listener.getInputStream()) {
                IOUtils.copy(responseContent, response.getOutputStream());
            }
        }
    }
}
