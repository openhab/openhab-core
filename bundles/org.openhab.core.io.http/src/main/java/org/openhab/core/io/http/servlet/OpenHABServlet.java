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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Base class for HTTP servlets which share certain {@link HttpContext} instance.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@NonNullByDefault
public abstract class OpenHABServlet extends BaseOpenHABServlet {

    private static final long serialVersionUID = 6854521240046714164L;

    /**
     * Http context.
     */
    protected final HttpContext httpContext;

    public OpenHABServlet(HttpService httpService, HttpContext httpContext) {
        super(httpService);
        this.httpContext = httpContext;
    }

    protected void activate(String alias) {
        super.activate(alias, httpContext);
    }
}
