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

import org.osgi.service.http.HttpContext;

/**
 * Base class for HTTP servlets which share certain {@link HttpContext} instance.
 *
 * @author Łukasz Dywicki - initial implementation.
 */
public abstract class SmartHomeServlet extends BaseSmartHomeServlet {

    private static final long serialVersionUID = 6854521240046714164L;

    /**
     * Http context.
     */
    protected HttpContext httpContext;

    protected void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    protected void unsetHttpContext(HttpContext httpContext) {
        this.httpContext = null;
    }

    protected void activate(String alias) {
        super.activate(alias, httpContext);
    }

}
