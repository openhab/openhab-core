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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A {@link User} sourced from a managed {@link UserProvider}.
 *
 * @author Yannick Schaus - initial contribution
 */
@NonNullByDefault
public class ManagedUser implements AuthenticatedUser {

    private String name;
    private String passwordHash;
    private String passwordSalt;
    private Set<String> roles = new HashSet<>();
    private @Nullable PendingToken pendingToken = null;
    private List<UserSession> sessions = new ArrayList<>();
    private List<UserApiToken> apiTokens = new ArrayList<>();

    /**
     * Constructs a user with a password hash and salt provided by the caller.
     *
     * @param name the username (account name)
     * @param passwordSalt the salt to compute the password hash
     * @param passwordHash the result of the hashing of the salted password
     */
    public ManagedUser(String name, String passwordSalt, String passwordHash) {
        this.name = name;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the password hash.
     *
     * @return the password hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Alters the password salt.
     *
     * @param passwordSalt the new password salt
     */
    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    /**
     * Alters the password hash.
     *
     * @param passwordHash the new password hash
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the password salt.
     *
     * @return the password salt
     */
    public String getPasswordSalt() {
        return passwordSalt;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Alters the user's account name
     *
     * @param name the new account name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUID() {
        return name;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Alters the user's set of roles.
     *
     * @param roles the new roles
     */
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @Override
    public @Nullable PendingToken getPendingToken() {
        return pendingToken;
    }

    @Override
    public void setPendingToken(@Nullable PendingToken pendingToken) {
        this.pendingToken = pendingToken;
    }

    @Override
    public List<UserSession> getSessions() {
        return sessions;
    }

    @Override
    public void setSessions(List<UserSession> sessions) {
        this.sessions = sessions;
    }

    @Override
    public List<UserApiToken> getApiTokens() {
        return apiTokens;
    }

    @Override
    public void setApiTokens(List<UserApiToken> apiTokens) {
        this.apiTokens = apiTokens;
    }

    @Override
    public String toString() {
        return name + " (" + String.join(", ", roles.stream().toArray(String[]::new)) + ")";
    }
}
