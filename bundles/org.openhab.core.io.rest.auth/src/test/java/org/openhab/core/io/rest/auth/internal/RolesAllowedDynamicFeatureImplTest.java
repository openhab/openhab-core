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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.security.Principal;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.auth.Role;

/**
 * Tests for {@link RolesAllowedDynamicFeatureImpl}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RolesAllowedDynamicFeatureImplTest {

    private @NonNullByDefault({}) RolesAllowedDynamicFeatureImpl feature;

    private @Mock @NonNullByDefault({}) ResourceInfo resourceInfo;
    private @Mock @NonNullByDefault({}) FeatureContext featureContext;
    private @Mock @NonNullByDefault({}) ContainerRequestContext requestContext;
    private @Mock @NonNullByDefault({}) SecurityContext securityContext;

    @BeforeEach
    public void setup() {
        feature = new RolesAllowedDynamicFeatureImpl();
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
    }

    // --- Helper methods and test resource classes ---

    private ContainerRequestFilter captureRegisteredFilter() {
        ArgumentCaptor<ContainerRequestFilter> captor = ArgumentCaptor.forClass(ContainerRequestFilter.class);
        verify(featureContext).register(captor.capture());
        return captor.getValue();
    }

    private void configureMethod(Class<?> resourceClass, String methodName) throws NoSuchMethodException {
        Method method = resourceClass.getMethod(methodName);
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(resourceInfo.getResourceClass()).thenReturn((Class) resourceClass);
    }

    // --- Test resource classes with various annotations ---

    @SuppressWarnings("unused")
    public static class AdminOnlyResource {
        @RolesAllowed({ Role.ADMIN })
        public void adminMethod() {
        }
    }

    @SuppressWarnings("unused")
    public static class UserAndAdminResource {
        @RolesAllowed({ Role.USER, Role.ADMIN })
        public void userOrAdminMethod() {
        }
    }

    @SuppressWarnings("unused")
    public static class DenyAllResource {
        @DenyAll
        public void deniedMethod() {
        }
    }

    @SuppressWarnings("unused")
    public static class PermitAllResource {
        @PermitAll
        public void openMethod() {
        }
    }

    @SuppressWarnings("unused")
    @RolesAllowed({ Role.ADMIN })
    public static class ClassLevelRolesResource {
        public void inheritedMethod() {
        }

        @PermitAll
        public void permitAllMethod() {
        }

        @RolesAllowed({ Role.USER })
        public void overriddenMethod() {
        }
    }

    @SuppressWarnings("unused")
    public static class NoAnnotationResource {
        public void unannotatedMethod() {
        }
    }

    // --- @RolesAllowed({ Role.ADMIN }) tests ---

    @Test
    public void adminOnlyMethodAllowsAdminRole() throws Exception {
        configureMethod(AdminOnlyResource.class, "adminMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        when(securityContext.isUserInRole(Role.ADMIN)).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(mock(Principal.class));

        filter.filter(requestContext);

        // Should not abort — admin is allowed
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    public void adminOnlyMethodBlocksUserRole() throws Exception {
        configureMethod(AdminOnlyResource.class, "adminMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        when(securityContext.isUserInRole(Role.ADMIN)).thenReturn(false);
        when(securityContext.getUserPrincipal()).thenReturn(mock(Principal.class));

        filter.filter(requestContext);

        // Should abort with 403 — authenticated but wrong role
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        assertEquals(403, responseCaptor.getValue().getStatus());
    }

    @Test
    public void adminOnlyMethodReturns401ForUnauthenticated() throws Exception {
        configureMethod(AdminOnlyResource.class, "adminMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        when(securityContext.isUserInRole(Role.ADMIN)).thenReturn(false);
        when(securityContext.getUserPrincipal()).thenReturn(null);

        filter.filter(requestContext);

        // Should abort with 401 — not authenticated
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        assertEquals(401, responseCaptor.getValue().getStatus());
    }

    // --- @RolesAllowed({ Role.USER, Role.ADMIN }) tests ---

    @Test
    public void userOrAdminMethodAllowsAdminRole() throws Exception {
        configureMethod(UserAndAdminResource.class, "userOrAdminMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        when(securityContext.isUserInRole(Role.ADMIN)).thenReturn(true);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    public void userOrAdminMethodAllowsUserRole() throws Exception {
        configureMethod(UserAndAdminResource.class, "userOrAdminMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        when(securityContext.isUserInRole(Role.USER)).thenReturn(true);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    public void userOrAdminMethodDeniesUnknownRole() throws Exception {
        configureMethod(UserAndAdminResource.class, "userOrAdminMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        when(securityContext.isUserInRole(anyString())).thenReturn(false);
        when(securityContext.getUserPrincipal()).thenReturn(mock(Principal.class));

        filter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        assertEquals(403, responseCaptor.getValue().getStatus());
    }

    // --- @DenyAll tests ---

    @Test
    public void denyAllReturns401ForUnauthenticated() throws Exception {
        configureMethod(DenyAllResource.class, "deniedMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        when(securityContext.getUserPrincipal()).thenReturn(null);

        filter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        assertEquals(401, responseCaptor.getValue().getStatus());
    }

    @Test
    public void denyAllReturns403ForAuthenticated() throws Exception {
        configureMethod(DenyAllResource.class, "deniedMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        when(securityContext.getUserPrincipal()).thenReturn(mock(Principal.class));

        filter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        assertEquals(403, responseCaptor.getValue().getStatus());
    }

    // --- @PermitAll tests ---

    @Test
    public void permitAllDoesNotRegisterFilter() throws Exception {
        configureMethod(PermitAllResource.class, "openMethod");
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, never()).register(any(ContainerRequestFilter.class));
    }

    // --- Class-level annotation tests ---

    @Test
    public void classLevelRolesAllowedAppliesToUnannotatedMethod() throws Exception {
        configureMethod(ClassLevelRolesResource.class, "inheritedMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        // Admin role matches class-level @RolesAllowed
        when(securityContext.isUserInRole(Role.ADMIN)).thenReturn(true);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    public void classLevelRolesAllowedBlocksWrongRole() throws Exception {
        configureMethod(ClassLevelRolesResource.class, "inheritedMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        when(securityContext.isUserInRole(Role.ADMIN)).thenReturn(false);
        when(securityContext.getUserPrincipal()).thenReturn(mock(Principal.class));

        filter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        assertEquals(403, responseCaptor.getValue().getStatus());
    }

    @Test
    public void permitAllOnMethodOverridesClassLevelRoles() throws Exception {
        configureMethod(ClassLevelRolesResource.class, "permitAllMethod");
        feature.configure(resourceInfo, featureContext);

        // @PermitAll on method should prevent filter registration
        verify(featureContext, never()).register(any(ContainerRequestFilter.class));
    }

    @Test
    public void methodLevelRolesAllowedOverridesClassLevel() throws Exception {
        configureMethod(ClassLevelRolesResource.class, "overriddenMethod");
        feature.configure(resourceInfo, featureContext);

        ContainerRequestFilter filter = captureRegisteredFilter();

        // Method says @RolesAllowed(USER), so USER should be allowed even though class says ADMIN
        when(securityContext.isUserInRole(Role.USER)).thenReturn(true);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    // --- No annotation tests ---

    @Test
    public void noAnnotationDoesNotRegisterFilter() throws Exception {
        configureMethod(NoAnnotationResource.class, "unannotatedMethod");
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, never()).register(any(ContainerRequestFilter.class));
    }
}
