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
package org.openhab.core.karaf.internal.jaas;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;

/**
 * A Karaf backing engine for the {@link UserRegistry}
 *
 * @author Yannick Schaus - initial contribution
 */
public class ManagedUserBackingEngine implements BackingEngine {

    UserRegistry userRegistry;

    public ManagedUserBackingEngine(UserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    @Override
    public void addUser(String username, String password) {
        userRegistry.register(username, password, new HashSet<String>(Set.of(Role.USER)));
    }

    @Override
    public void deleteUser(String username) {
        userRegistry.remove(username);
    }

    @Override
    public List<UserPrincipal> listUsers() {
        return userRegistry.getAll().stream().map(u -> new UserPrincipal(u.getName())).collect(Collectors.toList());
    }

    @Override
    public UserPrincipal lookupUser(String username) {
        User user = userRegistry.get(username);
        if (user != null) {
            return new UserPrincipal(user.getName());
        }
        return null;
    }

    @Override
    public List<GroupPrincipal> listGroups(UserPrincipal user) {
        return Collections.emptyList();
    }

    @Override
    public Map<GroupPrincipal, String> listGroups() {
        return Collections.emptyMap();
    }

    @Override
    public void addGroup(String username, String group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createGroup(String group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteGroup(String username, String group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<RolePrincipal> listRoles(Principal principal) {
        User user = userRegistry.get(principal.getName());
        if (user != null) {
            return user.getRoles().stream().map(r -> new RolePrincipal(r)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void addRole(String username, String role) {
        User user = userRegistry.get(username);
        if (user instanceof ManagedUser) {
            ManagedUser managedUser = (ManagedUser) user;
            managedUser.getRoles().add(role);
            userRegistry.update(managedUser);
        }
    }

    @Override
    public void deleteRole(String username, String role) {
        User user = userRegistry.get(username);
        if (user instanceof ManagedUser) {
            ManagedUser managedUser = (ManagedUser) user;
            managedUser.getRoles().remove(role);
            userRegistry.update(managedUser);
        }
    }

    @Override
    public void addGroupRole(String group, String role) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteGroupRole(String group, String role) {
        throw new UnsupportedOperationException();
    }

}
