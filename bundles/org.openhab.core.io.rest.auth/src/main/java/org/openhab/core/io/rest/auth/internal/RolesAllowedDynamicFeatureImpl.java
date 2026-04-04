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
package org.openhab.core.io.rest.auth.internal;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.openhab.core.io.rest.JSONResponse;
import org.openhab.core.io.rest.RESTConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsApplicationSelect;
import org.osgi.service.jakartars.whiteboard.propertytypes.JakartarsExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

/**
 * A {@link DynamicFeature} supporting the {@code javax.annotation.security.RolesAllowed},
 * {@code javax.annotation.security.PermitAll} and {@code javax.annotation.security.DenyAll}
 * on resource methods and sub-resource methods.
 *
 * Ported from the Jersey {@code RolesAllowedDynamicFeature} class with modifications.
 *
 * @author Paul Sandoz - initial contribution
 * @author Martin Matula - initial contribution
 * @author Yannick Schaus - port to openHAB with modifications
 */
@Provider
@Component
@JakartarsExtension
@JakartarsApplicationSelect("(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME + "=" + RESTConstants.JAX_RS_NAME + ")")
public class RolesAllowedDynamicFeatureImpl implements DynamicFeature {
    private final Logger logger = LoggerFactory.getLogger(RolesAllowedDynamicFeatureImpl.class);

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext configuration) {
        final Method am = resourceInfo.getResourceMethod();
        try {
            // DenyAll on the method take precedence over RolesAllowed and PermitAll
            if (am.isAnnotationPresent(DenyAll.class)) {
                configuration.register(new RolesAllowedRequestFilter());
                return;
            }

            // RolesAllowed on the method takes precedence over PermitAll
            Optional<Annotation> ra = Arrays.stream(am.getAnnotations())
                    .filter(a -> a.annotationType().getName().equals(RolesAllowed.class.getName())).findFirst();
            if (ra.isPresent()) {
                configuration.register(new RolesAllowedRequestFilter(((RolesAllowed) ra.get()).value()));
                return;
            }

            // PermitAll takes precedence over RolesAllowed on the class
            if (am.isAnnotationPresent(PermitAll.class)) {
                // Do nothing.
                return;
            }

            // DenyAll can't be attached to classes

            // RolesAllowed on the class takes precedence over PermitAll
            ra = Arrays.stream(resourceInfo.getResourceClass().getAnnotations())
                    .filter(a -> a.annotationType().getName().equals(RolesAllowed.class.getName())).findFirst();
            if (ra.isPresent()) {
                configuration.register(new RolesAllowedRequestFilter(((RolesAllowed) ra.get()).value()));
            }
        } catch (Exception e) {
            logger.error("Error while configuring the roles", e);
        }
    }

    @Priority(Priorities.AUTHORIZATION) // authorization filter - should go after any authentication filters
    private static class RolesAllowedRequestFilter implements ContainerRequestFilter {

        private final boolean denyAll;
        private final String[] rolesAllowed;

        RolesAllowedRequestFilter() {
            this.denyAll = true;
            this.rolesAllowed = null;
        }

        RolesAllowedRequestFilter(final String[] rolesAllowed) {
            this.denyAll = false;
            this.rolesAllowed = (rolesAllowed != null) ? rolesAllowed : new String[] {};
        }

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            if (!denyAll) {
                if (rolesAllowed.length == 0) {
                    return;
                }

                for (final String role : rolesAllowed) {
                    if (requestContext.getSecurityContext().isUserInRole(role)) {
                        return;
                    }
                }
            }

            if (!isAuthenticated(requestContext)) {
                requestContext
                        .abortWith(JSONResponse.createErrorResponse(Status.UNAUTHORIZED, "Authentication required"));
                return;
            }

            requestContext.abortWith(JSONResponse.createErrorResponse(Status.FORBIDDEN, "Access denied"));
        }

        private static boolean isAuthenticated(final ContainerRequestContext requestContext) {
            return requestContext.getSecurityContext().getUserPrincipal() != null;
        }
    }
}
