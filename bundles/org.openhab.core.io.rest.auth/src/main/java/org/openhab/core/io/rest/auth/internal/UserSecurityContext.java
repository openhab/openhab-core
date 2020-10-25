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

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.User;

/**
 * This {@link SecurityContext} contains information about a user, roles and authorizations granted to a client
 * from a {@link User} instance.
 *
 * @author Yannick Schaus - initial contribution
 */
@NonNullByDefault
public class UserSecurityContext implements AuthenticationSecurityContext {

    private User user;
    private Authentication authentication;
    private String authenticationScheme;

    /**
     * Constructs a security context from an instance of {@link User}
     *
     * @param user the user
     * @param the related {@link Authentication}
     * @param authenticationScheme the scheme that was used to authenticate the user, e.g. "Basic"
     */
    public UserSecurityContext(User user, Authentication authentication, String authenticationScheme) {
        this.user = user;
        this.authentication = authentication;
        this.authenticationScheme = authenticationScheme;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(@Nullable String role) {
        return user.getRoles().contains(role);
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    @Override
    public Authentication getAuthentication() {
        return authentication;
    }
}
