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

import java.io.IOException;

import javax.annotation.Priority;
import javax.security.sasl.AuthenticationException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.openhab.core.auth.Authentication;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;

/**
 * This filter is responsible for parsing a token provided with a request, and hydrating a {@link SecurityContext} from
 * the claims contained in the token.
 *
 * @author Yannick Schaus - initial contribution
 */
@PreMatching
@Component
@JaxrsExtension
@JaxrsApplicationSelect("(" + JaxrsWhiteboardConstants.JAX_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
@Priority(Priorities.AUTHENTICATION)
@Provider
public class AuthFilter implements ContainerRequestFilter {
    private static final String ALT_AUTH_HEADER = "X-OPENHAB-TOKEN";

    @Reference
    private JwtHelper jwtHelper;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            if (authHeader != null) {
                String[] authParts = authHeader.split(" ");
                if (authParts.length == 2) {
                    if ("Bearer".equals(authParts[0])) {
                        Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(authParts[1]);
                        requestContext.setSecurityContext(new JwtSecurityContext(auth));
                        return;
                    }
                }
            }

            String altTokenHeader = requestContext.getHeaderString(ALT_AUTH_HEADER);
            if (altTokenHeader != null) {
                Authentication auth = jwtHelper.verifyAndParseJwtAccessToken(altTokenHeader);
                requestContext.setSecurityContext(new JwtSecurityContext(auth));
                return;
            }
        } catch (AuthenticationException e) {
            requestContext.abortWith(JSONResponse.createErrorResponse(Status.UNAUTHORIZED, "Invalid token"));
        }
    }
}
