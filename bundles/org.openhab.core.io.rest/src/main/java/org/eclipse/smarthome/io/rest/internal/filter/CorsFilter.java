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
package org.eclipse.smarthome.io.rest.internal.filter;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PostMatching filter used to add CORS HTTP headers on responses for requests with CORS
 * headers.
 *
 * Based on http://www.w3.org/TR/cors
 *
 * @author Antoine Besnard - Initial contribution
 */
@Provider
@Component(immediate = true, service = CorsFilter.class, configurationPid = "smarthome.cors")
public class CorsFilter implements ContainerResponseFilter {

    static final String HTTP_HEAD_METHOD = "HEAD";
    static final String HTTP_DELETE_METHOD = "DELETE";
    static final String HTTP_PUT_METHOD = "PUT";
    static final String HTTP_POST_METHOD = "POST";
    static final String HTTP_GET_METHOD = "GET";
    static final String HTTP_OPTIONS_METHOD = "OPTIONS";

    static final String CONTENT_TYPE_HEADER = HttpHeaders.CONTENT_TYPE;

    static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
    static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    static final String ORIGIN_HEADER = "Origin";

    static final String ACCEPTED_HTTP_METHODS = Stream.of(HTTP_GET_METHOD, HTTP_POST_METHOD, HTTP_PUT_METHOD,
            HTTP_DELETE_METHOD, HTTP_HEAD_METHOD, HTTP_OPTIONS_METHOD).collect(Collectors.joining(","));

    private final transient Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    private Config config;

    public CorsFilter() {
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        // Process the request only if it is different from an OPTIONS request
        if (!config.enabled() || HTTP_OPTIONS_METHOD.equals(requestContext.getMethod())) {
            return;
        }

        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        String origin = headers.getFirst(ORIGIN_HEADER);
        headers.putSingle(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, origin != null ? origin : "*");
        headers.putSingle(ACCESS_CONTROL_ALLOW_METHODS_HEADER, ACCEPTED_HTTP_METHODS);
        headers.putSingle(ACCESS_CONTROL_ALLOW_HEADERS, CONTENT_TYPE_HEADER);
    }

    @ObjectClassDefinition(description = "%cors_description", name = "%cors_name")
    @interface Config {
        boolean enabled() default false;
    }

    @Activate
    protected void activate(ComponentContext context, Config config) {
        this.config = config;
        if (config.enabled()) {
            logger.info("enabled CORS for REST API.");
        } else {
            context.disableComponent(this.getClass().getName());
        }
    }
}
