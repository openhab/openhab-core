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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;

/**
 * An interface for a generic {@link Registry} of {@link User} entities. User registries can also be used as
 * {@link AuthenticationProvider}.
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
public interface UserRegistry extends Registry<User, String>, AuthenticationProvider {

    /**
     * Adds a new {@link User} in this registry. The implementation receives the clear text credentials and is
     * responsible for their secure storage (for instance by hashing the password), then return the newly created
     * {@link User} instance.
     *
     * @param username the username of the new user
     * @param password the user password
     * @param roles the roles attributed to the new user
     * @return the new registered {@link User} instance
     */
    public User register(String username, String password, Set<String> roles);

    /**
     * Change the password for an {@link User} in this registry. The implementation receives the new password and is
     * responsible for their secure storage (for instance by hashing the password).
     *
     * @param username the username of the existing user
     * @param newPassword the new password
     */
    public void changePassword(User user, String newPassword);

    /**
     * Adds a new session to the user profile
     *
     * @param user the user
     * @param session the session to add
     */
    public void addUserSession(User user, UserSession session);

    /**
     * Removes the specified session from the user profile
     *
     * @param user the user
     * @param session the session to remove
     */
    public void removeUserSession(User user, UserSession session);

    /**
     * Clears all sessions from the user profile
     *
     * @param user the user
     */
    public void clearSessions(User user);

    /**
     * Adds a new API token to the user profile. The implementation is responsible for storing the token in a secure way
     * (for instance by hashing it).
     *
     * @param user the user
     * @param name the name of the API token to create
     * @param scope the scope this API token will be valid for
     * @return the string that can be used as a Bearer token to match the new API token
     */
    public String addUserApiToken(User user, String name, String scope);

    /**
     * Removes the specified API token from the user profile
     *
     * @param user the user
     * @param apiToken the API token
     */
    public void removeUserApiToken(User user, UserApiToken apiToken);
}
