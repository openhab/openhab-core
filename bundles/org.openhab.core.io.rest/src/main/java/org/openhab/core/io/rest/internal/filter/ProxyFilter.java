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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter used to update both base and request URIs in Jersey's request
 * context if proxy headers are detected.
 *
 * @author Ivan Iliev - Initial contribution
 */
@Provider
@PreMatching
@Component(configurationPid = "org.openhab.proxyfilter", immediate = true, service = ProxyFilter.class)
public class ProxyFilter implements ContainerRequestFilter {

    static final String PROTO_PROXY_HEADER = "x-forwarded-proto";

    static final String HOST_PROXY_HEADER = "x-forwarded-host";

    private final transient Logger logger = LoggerFactory.getLogger(ProxyFilter.class);

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String host = getValue(ctx.getHeaders(), HOST_PROXY_HEADER);
        String scheme = getValue(ctx.getHeaders(), PROTO_PROXY_HEADER);

        // if our request does not have neither headers end right here
        if (scheme == null && host == null) {
            return;
        }

        UriInfo uriInfo = ctx.getUriInfo();
        URI requestUri = uriInfo.getRequestUri();

        UriBuilder baseBuilder = uriInfo.getBaseUriBuilder();
        UriBuilder requestBuilder = uriInfo.getRequestUriBuilder();

        // if only one of our headers is missing replace it with default value
        if (scheme == null) {
            scheme = requestUri.getScheme();
        }

        if (host == null) {
            host = requestUri.getHost();

            int port = requestUri.getPort();
            if (port != -1) {
                host += (":" + port);
            }
        }

        // create a new URI from the current scheme + host in order to validate
        // it
        String uriString = scheme + "://" + host;

        URI newBaseUri = null;
        try {
            newBaseUri = new URI(uriString);
        } catch (URISyntaxException e) {
            logger.error("Invalid X-Forwarded-Proto + X-Forwarded-Host header combination: {}", uriString, e);
            return;
        }

        // URI is valid replace base and request builder parts with ones
        // obtained from the given headers
        host = newBaseUri.getHost();
        if (host != null) {
            baseBuilder.host(host);
            requestBuilder.host(host);
        }

        int port = newBaseUri.getPort();
        baseBuilder.port(port);
        requestBuilder.port(port);

        scheme = newBaseUri.getScheme();
        if (scheme != null) {
            baseBuilder.scheme(scheme);
            requestBuilder.scheme(scheme);
        }

        ctx.setRequestUri(baseBuilder.build(), requestBuilder.build());
    }

    private String getValue(MultivaluedMap<String, String> headers, String header) {
        List<String> values = headers.get(header);
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }
}
