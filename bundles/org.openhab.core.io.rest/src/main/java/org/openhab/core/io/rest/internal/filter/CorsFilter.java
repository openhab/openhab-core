/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.rest.RESTConstants;
import org.openhab.core.io.rest.internal.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
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
 * @author Markus Rathgeb - Migrated to JAX-RS Whiteboard Specification
 */
@Component(property = {
        "service.pid=org.openhab.core.cors" }, configurationPid = "org.openhab.cors", configurationPolicy = ConfigurationPolicy.REQUIRE)
@JaxrsExtension
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@NonNullByDefault
public class CorsFilter implements ContainerResponseFilter {

    static final String HTTP_HEAD_METHOD = "HEAD";
    static final String HTTP_DELETE_METHOD = "DELETE";
    static final String HTTP_PUT_METHOD = "PUT";
    static final String HTTP_POST_METHOD = "POST";
    static final String HTTP_GET_METHOD = "GET";
    static final String HTTP_OPTIONS_METHOD = "OPTIONS";

    static final String CONTENT_TYPE_HEADER = HttpHeaders.CONTENT_TYPE;
    static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION;

    static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
    static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    static final String ORIGIN_HEADER = "Origin";
    static final String VARY_HEADER = "Vary";

    static final String VARY_HEADER_WILDCARD = "*";
    static final String HEADERS_SEPARATOR = ",";

    static final List<String> ACCEPTED_HTTP_METHODS_LIST = List.of(HTTP_GET_METHOD, HTTP_POST_METHOD, HTTP_PUT_METHOD,
            HTTP_DELETE_METHOD, HTTP_HEAD_METHOD, HTTP_OPTIONS_METHOD);

    static final String ACCEPTED_HTTP_METHODS = String.join(HEADERS_SEPARATOR, ACCEPTED_HTTP_METHODS_LIST);

    private final transient Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    private boolean isEnabled;

    public CorsFilter() {
        // Disable the filter by default
        this.isEnabled = false;
    }

    @Override
    public void filter(@NonNullByDefault({}) ContainerRequestContext requestContext,
            @NonNullByDefault({}) ContainerResponseContext responseContext) throws IOException {
        if (isEnabled && !processPreflight(requestContext, responseContext)) {
            processRequest(requestContext, responseContext);
        }
    }

    /**
     * Process the CORS request and response.
     *
     * @param requestContext
     * @param responseContext
     */    /**
     * Process the CORS request and response.
     *
     * @param requestContext The request context
     * @param responseContext The response context
     */
    private void processRequest(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Process the request only if if is an acceptable request method and if it is different from an OPTIONS request
        // (OPTIONS requests are not processed here)
        if (ACCEPTED_HTTP_METHODS_LIST.contains(requestContext.getMethod())
                && !HTTP_OPTIONS_METHOD.equals(requestContext.getMethod())) {
            String origin = getValue(requestContext.getHeaders(), ORIGIN_HEADER);
            if (origin != null && !origin.isBlank() && isOriginAllowed(origin)) {
                // Only add CORS headers if the origin is allowed
                if (anyOriginAllowed) {
                    responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
                } else {
                    responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, origin);
                }
                
                // Add the Vary header
                appendVaryHeader(responseContext);
            }
        }
    }

    /**
     * Process a preflight CORS request.
     *
     * @param requestContext
     * @param responseContext
     * @return true if it is a preflight request that has been processed.
     */    /**
     * Configuration property for allowed origins
     */
    private static final String ALLOWED_ORIGINS_PROPERTY = "allowed.origins";
    
    /**
     * Set of origins allowed to make cross-origin requests
     */
    private final Set<String> allowedOrigins = new HashSet<>();
    
    /**
     * Whether any origin is allowed to make CORS requests
     */
    private boolean anyOriginAllowed = false;
    
    /**
     * Process a preflight CORS request.
     *
     * @param requestContext The request context
     * @param responseContext The response context
     * @return true if it is a preflight request that has been processed.
     */
    private boolean processPreflight(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        boolean isCorsPreflight = false;

        if (HTTP_OPTIONS_METHOD.equals(requestContext.getMethod())) {
            // Look for the mandatory CORS preflight request headers
            String origin = getValue(requestContext.getHeaders(), ORIGIN_HEADER);
            String realRequestMethod = getValue(requestContext.getHeaders(), ACCESS_CONTROL_REQUEST_METHOD);
            isCorsPreflight = origin != null && !origin.isBlank() && realRequestMethod != null
                    && !realRequestMethod.isBlank();

            if (isCorsPreflight) {
                // Validate the origin before setting CORS headers
                if (isOriginAllowed(origin)) {
                    // Validate the requested method is allowed
                    if (ACCEPTED_HTTP_METHODS_LIST.contains(realRequestMethod)) {
                        // Add CORS headers conditionally based on origin validation
                        if (anyOriginAllowed) {
                            responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
                        } else {
                            responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, origin);
                        }
                        
                        responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_METHODS_HEADER, ACCEPTED_HTTP_METHODS);
                        responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, CONTENT_TYPE_HEADER);
                        responseContext.getHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, AUTHORIZATION_HEADER);

                        // Add the accepted request headers
                        appendVaryHeader(responseContext);
                    }
                }
            }
        }

        return isCorsPreflight;
    }
    
    /**
     * Determines if the specified origin is allowed to make CORS requests.
     * 
     * @param origin The origin.
     * @return {@code true} if the origin is allowed; {@code false} otherwise.
     */
    private boolean isOriginAllowed(final String origin) {
        if (anyOriginAllowed) {
            return true;
        }

        // If 'origin' is null, deny the request
        if (origin == null) {
            return false;
        }

        // Check against the allowed origins
        return allowedOrigins.contains(origin);
    }

    /**
     * Get the first value of a header which may contains several values.
     *
     * @param headers
     * @param header
     * @return The first value from the given header or null if the header is
     *         not found.
     */
    private @Nullable String getValue(MultivaluedMap<String, String> headers, String header) {
        List<String> values = headers.get(header);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }

    /**
     * Append the Vary header if necessary to the response.
     *
     * @param responseContext
     */
    private void appendVaryHeader(ContainerResponseContext responseContext) {
        String varyHeader = getValue(responseContext.getStringHeaders(), VARY_HEADER);
        if (varyHeader == null || varyHeader.isBlank()) {
            // If the Vary header is not present, just add it.
            responseContext.getHeaders().add(VARY_HEADER, ORIGIN_HEADER);
        } else if (!VARY_HEADER_WILDCARD.equals(varyHeader)) {
            // If it is already present and its value is not the Wildcard, append the Origin header.
            responseContext.getHeaders().putSingle(VARY_HEADER, varyHeader + HEADERS_SEPARATOR + ORIGIN_HEADER);
        }
    }    @Activate
    protected void activate(@Nullable Map<String, Object> properties) {
        if (properties != null) {
            String corsPropertyValue = (String) properties.get(Constants.CORS_PROPERTY);
            this.isEnabled = "true".equalsIgnoreCase(corsPropertyValue);
            
            // Process allowed origins configuration
            String allowedOriginsValue = (String) properties.get(ALLOWED_ORIGINS_PROPERTY);
            processAllowedOrigins(allowedOriginsValue);
        }

        if (this.isEnabled) {
            if (anyOriginAllowed) {
                logger.info("enabled CORS for REST API with any origin allowed.");
            } else {
                logger.info("enabled CORS for REST API with {} allowed origins.", allowedOrigins.size());
            }
        }
    }
    
    /**
     * Process the allowed origins configuration.
     * 
     * @param allowedOriginsValue Comma-separated list of allowed origins or "*" for any origin
     */
    private void processAllowedOrigins(String allowedOriginsValue) {
        allowedOrigins.clear();
        anyOriginAllowed = false;
        
        if (allowedOriginsValue == null || allowedOriginsValue.trim().isEmpty()) {
            // Default to localhost origins for security if not configured
            allowedOrigins.add("http://localhost");
            allowedOrigins.add("https://localhost");
            return;
        }
        
        if ("*".equals(allowedOriginsValue.trim())) {
            anyOriginAllowed = true;
            logger.warn("SECURITY WARNING: CORS is configured to allow requests from any origin!");
            return;
        }
        
        // Process comma-separated list of origins
        for (String origin : allowedOriginsValue.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                allowedOrigins.add(trimmed);
            }
        }
    }
}
