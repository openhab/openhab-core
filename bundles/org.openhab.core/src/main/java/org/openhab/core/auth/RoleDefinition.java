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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * A {@link RoleDefinition} represents a named role with a set of {@link Permission}s.
 * Roles can be built-in (provided by the system) or user-created (managed via the
 * {@link RoleRegistry}).
 * <p>
 * This is a plain POJO persisted via JSON DB using Gson's default reflection-based
 * serialization. Field names map directly to JSON property names.
 * <p>
 * Note: {@code equals}/{@code hashCode} are intentionally not overridden — the registry
 * framework uses {@link #getUID()} for identity-based lookups, matching the
 * {@link ManagedUser} convention.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public class RoleDefinition implements Identifiable<String> {

    private final String name;
    private final String description;
    private final Set<Permission> permissions;
    private final boolean builtIn;

    /**
     * Constructs a new role definition.
     *
     * @param name the unique name of the role (serves as the UID)
     * @param description a human-readable description of the role
     * @param permissions the set of permissions granted by this role
     * @param builtIn whether this role is a built-in system role
     */
    public RoleDefinition(String name, String description, Set<Permission> permissions, boolean builtIn) {
        this.name = name;
        this.description = description;
        this.permissions = Set.copyOf(permissions);
        this.builtIn = builtIn;
    }

    @Override
    public String getUID() {
        return name;
    }

    /**
     * Gets the name of this role.
     *
     * @return the role name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the description of this role.
     *
     * @return the role description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the set of permissions granted by this role.
     *
     * @return the permissions
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Returns whether this role is a built-in system role.
     *
     * @return {@code true} if this is a built-in role
     */
    public boolean isBuiltIn() {
        return builtIn;
    }

    @Override
    public String toString() {
        return name + " (" + (builtIn ? "built-in" : "custom") + ")";
    }
}
