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
package org.eclipse.smarthome.io.http.internal;

import org.eclipse.smarthome.io.http.HttpContextFactoryService;
import org.eclipse.smarthome.io.http.WrappingHttpContext;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Create {@link HttpContext} instances when registering servlets, resources or filters using the
 * {@link HttpService#registerServlet} and corresponding methods.
 * The resulting {@link HttpContext} complies with the OSGi specification when it comes to resource resolving.
 *
 * @author Henning Treu - initial contribution and API
 *
 */
@Component(service = HttpContextFactoryService.class)
public class HttpContextFactoryServiceImpl implements HttpContextFactoryService {

    private WrappingHttpContext httpContext;

    @Override
    public HttpContext createDefaultHttpContext(Bundle bundle) {
        return httpContext.wrap(bundle);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    public void setHttpContext(WrappingHttpContext httpContext) {
        this.httpContext = httpContext;
    }

    public void unsetHttpContext(WrappingHttpContext httpContext) {
        this.httpContext = null;
    }

}
