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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;

/**
 * A {@link Registry} for {@link RoleDefinition} entities. Aggregates roles from all
 * {@link RoleProvider}s (both built-in and user-managed).
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public interface RoleRegistry extends Registry<RoleDefinition, String> {

    /**
     * Returns whether the given role can be edited (i.e., it is managed by a
     * {@link org.openhab.core.common.registry.ManagedProvider}).
     *
     * @param role the role to check
     * @return {@code true} if the role is editable
     */
    boolean isEditable(RoleDefinition role);

    /**
     * Returns whether the given role is a default (built-in) role.
     *
     * @param role the role to check
     * @return {@code true} if the role is a built-in default role
     */
    boolean isDefault(RoleDefinition role);
}
