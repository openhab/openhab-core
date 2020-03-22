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
import org.openhab.core.auth.GenericUser;

/**
 * This {@link SecurityContext} contains information about a user, roles and authorizations granted to a client as
 * parsed from the contents of a JSON Web Token
 *
 * @author Yannick Schaus - initial contribution
 */
@NonNullByDefault
public class JwtSecurityContext implements SecurityContext {

    Authentication authentication;

    public JwtSecurityContext(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public Principal getUserPrincipal() {
        return new GenericUser(authentication.getUsername());
    }

    @Override
    public boolean isUserInRole(@Nullable String role) {
        return authentication.getRoles().contains(role);
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getAuthenticationScheme() {
        return "JWT";
    }
}
