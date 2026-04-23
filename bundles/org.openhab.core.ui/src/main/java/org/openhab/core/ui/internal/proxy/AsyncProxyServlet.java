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
package org.openhab.core.ui.internal.proxy;

import java.io.Serial;

/**
 * This version of the proxy servlet uses the blocking proxy implementation.
 * The Jetty 12 async proxy servlet API is not available as an EE10 servlet;
 * this class is retained for API compatibility and delegates to {@link BlockingProxyServlet}.
 *
 * @author John Cocula - Initial contribution
 */
public class AsyncProxyServlet extends BlockingProxyServlet {

    @Serial
    private static final long serialVersionUID = -4716754591953017795L;

    AsyncProxyServlet(ProxyServletService service) {
        super(service);
    }

    @Override
    public String getServletInfo() {
        return "Proxy (async)";
    }
}
