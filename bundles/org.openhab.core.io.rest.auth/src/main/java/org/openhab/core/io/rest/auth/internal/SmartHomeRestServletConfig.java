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
package org.openhab.core.io.rest.auth.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import com.eclipsesource.jaxrs.publisher.ServletConfiguration;

/**
 * Custom servlet configuration for jaxrs handler.
 *
 * This extension doesn't do much, just forces usage of http context created somewhere else.
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@Component
public class SmartHomeRestServletConfig implements ServletConfiguration {

    private HttpContext httpContext;

    @Override
    public HttpContext getHttpContext(HttpService httpService, String rootPath) {
        return httpContext;
    }

    @Override
    public Dictionary<String, String> getInitParams(HttpService httpService, String rootPath) {
        return new Hashtable<>();
    }

    @Reference
    public void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    public void unsetHttpContext(HttpContext httpContext) {
        this.httpContext = null;
    }
}
