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
package org.openhab.core.io.http.internal;

import java.io.IOException;
import java.net.URL;

import org.osgi.service.servlet.context.ServletContextHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Http context which does nothing but lets the delegate do its job.
 *
 * @author Łukasz Dywicki - Initial contribution.
 */
class DelegatingHttpContext extends ServletContextHelper {

    private final ServletContextHelper delegate;

    public DelegatingHttpContext(ServletContextHelper delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return delegate.handleSecurity(request, response);
    }

    @Override
    public URL getResource(String name) {
        return delegate.getResource(name);
    }

    @Override
    public String getMimeType(String name) {
        return delegate.getMimeType(name);
    }
}
