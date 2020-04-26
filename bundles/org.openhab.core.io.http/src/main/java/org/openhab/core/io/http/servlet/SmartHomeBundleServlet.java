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

import org.openhab.core.io.http.HttpContextFactoryService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;

/**
 * Base class for servlets which host resources using framework bundles.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
public abstract class SmartHomeBundleServlet extends BaseSmartHomeServlet {

    protected HttpContextFactoryService httpContextFactoryService;

    public void setHttpContextFactoryService(HttpContextFactoryService httpContextFactoryService) {
        this.httpContextFactoryService = httpContextFactoryService;
    }

    public void unsetHttpContextFactoryService(HttpContextFactoryService httpContextFactoryService) {
        this.httpContextFactoryService = null;
    }

    protected void activate(String alias, Bundle bundle) {
        super.activate(alias, createHttpContext(bundle));
    }

    protected void activate(String alias, BundleContext bundleContext) {
        this.activate(alias, bundleContext.getBundle());
    }

    protected final HttpContext createHttpContext(Bundle bundle) {
        return httpContextFactoryService.createDefaultHttpContext(bundle);
    }
}
