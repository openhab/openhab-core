/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.io.net.http;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * Implementation of {@link HttpContext} which adds Basic-Authentication
 * functionality to openHAB.
 *
 * @author Thomas.Eichstaedt-Engelen
 */
public class SecureHttpContext implements HttpContext {

    private HttpContext defaultContext = null;

    public SecureHttpContext() {
    }

    public SecureHttpContext(HttpContext defaultContext, final String realm) {
        this.defaultContext = defaultContext;
    }

    /**
     * <p>
     * 
     * @{inheritDoc}
     *               </p>
     *               <p>
     *               Delegates to <code>defaultContext.getMimeType()</code>
     */
    @Override
    public String getMimeType(String name) {
        return this.defaultContext.getMimeType(name);
    }

    /**
     * <p>
     * 
     * @{inheritDoc}
     *               </p>
     *               <p>
     *               Delegates to <code>defaultContext.getResource()</code>
     */
    @Override
    public URL getResource(String name) {
        return this.defaultContext.getResource(name);
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return true;
    }

}
