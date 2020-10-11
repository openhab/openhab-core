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
import java.util.Base64;

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
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;

/**
 * This filter is responsible for parsing credentials provided with a request, and hydrating a {@link SecurityContext}
 * from these credentials if they are valid.
 *
 * @author Yannick Schaus - initial contribution
 * @author Yannick Schaus - Allow basic authentication
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

    @Reference
    private UserRegistry userRegistry;

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
                    } else if ("Basic".equals(authParts[0])) {
                        try {
                            String[] decodedCredentials = new String(Base64.getDecoder().decode(authParts[1]), "UTF-8")
                                    .split(":");
                            if (decodedCredentials.length != 2) {
                                throw new AuthenticationException("Invalid Basic authentication credential format");
                            }
                            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                                    decodedCredentials[0], decodedCredentials[1]);
                            Authentication auth = userRegistry.authenticate(credentials);
                            User user = userRegistry.get(auth.getUsername());
                            if (user == null) {
                                throw new org.openhab.core.auth.AuthenticationException("User not found in registry");
                            }
                            requestContext.setSecurityContext(new UserSecurityContext(user, "Basic"));
                            return;
                        } catch (org.openhab.core.auth.AuthenticationException e) {
                            throw new AuthenticationException("Invalid user name or password");
                        }
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
            requestContext.abortWith(JSONResponse.createErrorResponse(Status.UNAUTHORIZED, "Invalid credentials"));
        }
    }
}
