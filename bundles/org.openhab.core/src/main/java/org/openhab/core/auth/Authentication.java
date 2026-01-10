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
package org.openhab.core.auth;

import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Definition of authentication given to username after verification of credentials by authentication provider.
 *
 * Each authentication must at least point to some identity (username), roles, and may also be valid for a specific
 * scope only.
 *
 * @author Łukasz Dywicki - Initial contribution
 * @author Kai Kreuzer - Added JavaDoc and switched from array to Set
 * @author Yannick Schaus - Add scope
 */
@NonNullByDefault
public class Authentication {

    private @Nullable String username;
    private Set<String> roles;
    private @Nullable String scope;

    /**
     * no-args constructor required by gson
     */
    protected Authentication() {
        this.username = null;
        this.roles = Set.of();
        this.scope = null;
    }

    /**
     * Creates a new instance
     *
     * @param username name of the user associated to this authentication instance
     * @param roles a variable list of roles that the user possesses.
     */
    public Authentication(String username, String... roles) {
        this.username = username;
        this.roles = Set.of(roles);
    }

    /**
     * Creates a new instance with a specific scope
     *
     * @param username name of the user associated to this authentication instance
     * @param roles a variable list of roles that the user possesses.
     * @param scope a scope this authentication is valid for
     */
    public Authentication(String username, String[] roles, String scope) {
        this(username, roles);
        this.scope = scope;
    }

    /**
     * Retrieves the name of the authenticated user
     *
     * @return user name
     */
    public String getUsername() {
        return Objects.requireNonNull(username);
    }

    /**
     * Retrieves the roles of the authenticated user
     *
     * @return a set of roles
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Retrieves the scope this authentication is valid for
     *
     * @return a scope
     */
    public @Nullable String getScope() {
        return scope;
    }
}
