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
import org.openhab.core.auth.Role;

/**
 * This {@link SecurityContext} can be used to give anonymous users (i.e. unauthenticated requests) the "user" role.
 *
 * @author Yannick Schaus - initial contribution
 */
@NonNullByDefault
public class AnonymousUserSecurityContext implements SecurityContext {

    @Override
    public @Nullable Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isUserInRole(@Nullable String role) {
        return role == null || Role.USER.equals(role);
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public @Nullable String getAuthenticationScheme() {
        return null;
    }
}
