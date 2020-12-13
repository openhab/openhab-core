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
package org.openhab.core.io.http.servlet;

import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for HTTP servlets.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@NonNullByDefault
public abstract class BaseOpenHABServlet extends HttpServlet {

    private static final long serialVersionUID = 6020752826735599455L;

    /**
     * Logger bound to child class.
     */
    protected final Logger logger = LoggerFactory.getLogger(BaseOpenHABServlet.class);

    /**
     * Http service.
     */
    protected final HttpService httpService;

    public BaseOpenHABServlet(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void activate(String alias, HttpContext httpContext) {
        try {
            logger.debug("Starting up {} at {}", getClass().getSimpleName(), alias);

            Hashtable<String, String> props = new Hashtable<>();
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
