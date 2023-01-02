/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.auth.UserRegistry;

/**
 * The {@link AuthFilterTest} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthFilterTest {

    @InjectMocks
    private @NonNullByDefault({}) AuthFilter authFilter;

    // These mocks are inject into authFilter during setup
    public @Mock @NonNullByDefault({}) JwtHelper jwtHelperMock;
    public @Mock @NonNullByDefault({}) UserRegistry userRegistryMock;

    private @Mock @NonNullByDefault({}) ContainerRequestContext containerRequestContext;
    private @Mock @NonNullByDefault({}) HttpServletRequest servletRequest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(servletRequest.getRemoteAddr()).thenReturn("192.168.0.100");
    }

    @Test
    public void implicitUserRoleAllowsAccess() throws IOException {
        authFilter.activate(Map.of()); // implicit user role is true by default
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext).setSecurityContext(any());
    }

    @Test
    public void noImplicitUserRoleDeniesAccess() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false));
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext, never()).setSecurityContext(any());
    }

    @Test
    public void trustedNetworkAllowsAccessIfForwardedHeaderMatches() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_TRUSTED_NETWORKS,
                "192.168.1.0/24"));
        when(containerRequestContext.getHeaderString("x-forwarded-for")).thenReturn("192.168.1.100");
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext).setSecurityContext(any());
    }

    @Test
    public void trustedNetworkDeniesAccessIfForwardedHeaderDoesNotMatch() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_TRUSTED_NETWORKS,
                "192.168.1.0/24"));
        when(containerRequestContext.getHeaderString("x-forwarded-for")).thenReturn("192.168.2.100");
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext, never()).setSecurityContext(any());
    }

    @Test
    public void trustedNetworkAllowsAccessIfRemoteAddressMatches() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_TRUSTED_NETWORKS,
                "192.168.0.0/24"));
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext).setSecurityContext(any());
    }

    @Test
    public void trustedNetworkDeniesAccessIfRemoteAddressDoesNotMatch() throws IOException {
        authFilter.activate(Map.of(AuthFilter.CONFIG_IMPLICIT_USER_ROLE, false, AuthFilter.CONFIG_TRUSTED_NETWORKS,
                "192.168.1.0/24"));
        authFilter.filter(containerRequestContext);

        verify(containerRequestContext, never()).setSecurityContext(any());
    }
}
