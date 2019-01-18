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
package org.eclipse.smarthome.io.http.servlet;

import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for HTTP servlets.
 *
 * @author Łukasz Dywicki - Initial contribution and API
 */
public abstract class BaseSmartHomeServlet extends HttpServlet {

    private static final long serialVersionUID = 6020752826735599455L;

    /**
     * Logger bound to child class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Http service.
     */
    protected HttpService httpService;

    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    protected void activate(String alias, HttpContext httpContext) {
        try {
            logger.debug("Starting up {} at {}", getClass().getSimpleName(), alias);

            Hashtable<String, String> props = new Hashtable<String, String>();
            httpService.registerServlet(alias, this, props, httpContext);
        } catch (NamespaceException e) {
            logger.error("Error during servlet registration - alias {} already in use", alias, e);
        } catch (ServletException e) {
            logger.error("Error during servlet registration", e);
        }
    }

    protected void deactivate(String alias) {
        httpService.unregister(alias);
    }

}
