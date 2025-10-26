/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.karaf.internal.jaas;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;

/**
 * A Karaf backing engine for the {@link UserRegistry}
 *
 * @author Yannick Schaus - initial contribution
 */
@NonNullByDefault
public class ManagedUserBackingEngine implements BackingEngine {

    private final UserRegistry userRegistry;

    public ManagedUserBackingEngine(UserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    @Override
    public void addUser(@Nullable String username, @Nullable String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        userRegistry.register(username, password, new HashSet<>(Set.of(Role.USER)));
    }

    @Override
    public void deleteUser(@Nullable String username) {
        Objects.requireNonNull(username);
        userRegistry.remove(username);
    }

    @Override
    @NonNullByDefault({})
    public List<UserPrincipal> listUsers() {
        return userRegistry.getAll().stream().map(u -> new UserPrincipal(u.getName())).toList();
    }

    @Override
    public @Nullable UserPrincipal lookupUser(@Nullable String username) {
        Objects.requireNonNull(username);
        User user = userRegistry.get(username);
        if (user != null) {
            return new UserPrincipal(user.getName());
        }
        return null;
    }

    @Override
    @NonNullByDefault({})
    public List<GroupPrincipal> listGroups(@Nullable UserPrincipal user) {
        return List.of();
    }

    @Override
    @NonNullByDefault({})
    public Map<GroupPrincipal, String> listGroups() {
        return Map.of();
    }

    @Override
    public void addGroup(@Nullable String username, @Nullable String group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createGroup(@Nullable String group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteGroup(@Nullable String username, @Nullable String group) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNullByDefault({})
    public List<RolePrincipal> listRoles(@Nullable Principal principal) {
        User user = userRegistry.get(principal.getName());
        if (user != null) {
            return user.getRoles().stream().map(r -> new RolePrincipal(r)).toList();
        }
        return List.of();
    }

    @Override
    public void addRole(@Nullable String username, @Nullable String role) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(role);
        User user = userRegistry.get(username);
        if (user instanceof ManagedUser managedUser) {
            managedUser.getRoles().add(role);
            userRegistry.update(managedUser);
        }
    }

    @Override
    public void deleteRole(@Nullable String username, @Nullable String role) {
        Objects.requireNonNull(username);
        User user = userRegistry.get(username);
        if (user instanceof ManagedUser managedUser) {
            managedUser.getRoles().remove(role);
            userRegistry.update(managedUser);
        }
    }

    @Override
    public void addGroupRole(@Nullable String group, @Nullable String role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteGroupRole(@Nullable String group, @Nullable String role) {
        throw new UnsupportedOperationException();
    }
}
