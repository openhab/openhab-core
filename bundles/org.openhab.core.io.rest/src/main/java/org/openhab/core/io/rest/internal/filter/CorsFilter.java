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
package org.openhab.core.io.rest.internal.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.openhab.core.io.rest.internal.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PostMatching filter used to add CORS HTTP headers on responses for requests with CORS
 * headers.
 *
 * Based on http://www.w3.org/TR/cors
 *
 * This implementation does not allow specific request/response headers nor cookies (allowCredentials).
 *
 * @author Antoine Besnard - Initial contribution
 */
@Provider
@Component(immediate = true, property = {
        "service.pid=org.openhab.core.cors" }, configurationPid = "org.openhab.cors", configurationPolicy = ConfigurationPolicy.REQUIRE)
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
    static final String VARY_HEADER = "Vary";

    static final String VARY_HEADER_WILDCARD = "*";
    static final String HEADERS_SEPARATOR = ",";

    static final List<String> ACCEPTED_HTTP_METHODS_LIST = Arrays.asList(HTTP_GET_METHOD, HTTP_POST_METHOD,
            HTTP_PUT_METHOD, HTTP_DELETE_METHOD, HTTP_HEAD_METHOD, HTTP_OPTIONS_METHOD);

    static final String ACCEPTED_HTTP_METHODS = ACCEPTED_HTTP_METHODS_LIST.stream()
            .collect(Collectors.joining(HEADERS_SEPARATOR));

    private final transient Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    private boolean isEnabled;

    public CorsFilter() {
        // Disable the filter by default
        this.isEnabled = false;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        if (isEnabled && !processPreflight(requestContext, responseContext)) {
            processRequest(requestContext, responseContext);
        }
    }

    /**
     * Process the CORS request and response.
     *
     * @param requestContext
     * @param responseContext
     */
    private void processRequest(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Process the request only if if is an acceptable request method and if it is different from an OPTIONS request
        // (OPTIONS requests are not processed here)
        if (ACCEPTED_HTTP_METHODS_LIST.contains(requestContext.getMethod())
                && !HTTP_OPTIONS_METHOD.equals(requestContext.getMethod())) {
            String origin = getValue(requestContext.getHeaders(), ORIGIN_HEADER);
            if (StringUtils.isNotBlank(origin)) {
                responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, origin);
            }
        }
    }

    /**
     * Process a preflight CORS request.
     *
     * @param requestContext
     * @param responseContext
     * @return true if it is a preflight request that has been processed.
     */
    private boolean processPreflight(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        boolean isCorsPreflight = false;

        if (HTTP_OPTIONS_METHOD.equals(requestContext.getMethod())) {
            // Look for the mandatory CORS preflight request headers
            String origin = getValue(requestContext.getHeaders(), ORIGIN_HEADER);
            String realRequestMethod = getValue(requestContext.getHeaders(), ACCESS_CONTROL_REQUEST_METHOD);
            isCorsPreflight = StringUtils.isNotBlank(origin) && StringUtils.isNotBlank(realRequestMethod);

            if (isCorsPreflight) {
                responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, origin);
                responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_METHODS_HEADER, ACCEPTED_HTTP_METHODS);
                responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, CONTENT_TYPE_HEADER);

                // Add the accepted request headers
                appendVaryHeader(responseContext);
            }
        }

        return isCorsPreflight;
    }

    /**
     * Get the first value of a header which may contains several values.
     *
     * @param headers
     * @param header
     * @return The first value from the given header or null if the header is
     *         not found.
     *
     */
    private String getValue(MultivaluedMap<String, String> headers, String header) {
        List<String> values = headers.get(header);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0).toString();
    }

    /**
     * Append the Vary header if necessary to the response.
     *
     * @param responseContext
     */
    private void appendVaryHeader(ContainerResponseContext responseContext) {
        String varyHeader = getValue(responseContext.getStringHeaders(), VARY_HEADER);
        if (StringUtils.isBlank(varyHeader)) {
            // If the Vary header is not present, just add it.
            responseContext.getHeaders().add(VARY_HEADER, ORIGIN_HEADER);
        } else if (!VARY_HEADER_WILDCARD.equals(varyHeader)) {
            // If it is already present and its value is not the Wildcard, append the Origin header.
            responseContext.getHeaders().putSingle(VARY_HEADER, varyHeader + HEADERS_SEPARATOR + ORIGIN_HEADER);
        }
    }

    @Activate
    protected void activate(Map<String, Object> properties) {
        if (properties != null) {
            String corsPropertyValue = (String) properties.get(Constants.CORS_PROPERTY);
            this.isEnabled = "true".equalsIgnoreCase(corsPropertyValue);
        }

        if (this.isEnabled) {
            logger.info("enabled CORS for REST API.");
        }
    }

}
