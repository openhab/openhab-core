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
package org.openhab.core.auth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Definition of authentication given to username after verification of credentials by authentication provider.
 *
 * Each authentication must at least point to some identity (username) and roles.
 *
 * @author Łukasz Dywicki - Initial contribution
 * @author Kai Kreuzer - Added JavaDoc and switched from array to Set
 */
public class Authentication {

    private String username;
    private Set<String> roles;

    /**
     * no-args constructor required by gson
     */
    protected Authentication() {
        this.username = null;
        this.roles = null;
    }

    /**
     * Creates a new instance
     *
     * @param username name of the user associated to this authentication instance
     * @param roles a variable list of roles that the user possesses.
     */
    public Authentication(String username, String... roles) {
        this.username = username;
        this.roles = new HashSet<>(Arrays.asList(roles));
    }

    /**
     * Retrieves the name of the authenticated user
     *
     * @return user name
     */
    public String getUsername() {
        return username;
    }

    /**
     * Retrieves the roles of the authenticated user
     *
     * @return a set of roles
     */
    public Set<String> getRoles() {
        return roles;
    }

}
