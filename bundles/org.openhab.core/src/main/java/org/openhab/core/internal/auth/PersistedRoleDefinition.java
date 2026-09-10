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
package org.openhab.core.internal.auth;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A Gson-serializable DTO for {@link org.openhab.core.auth.RoleDefinition}. Uses
 * {@link PersistedPermission} instances with plain string fields to avoid Gson
 * interface deserialization issues.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public class PersistedRoleDefinition {

    public String name = "";
    public String description = "";
    public List<PersistedPermission> permissions = new ArrayList<>();
    public boolean builtIn;

    public PersistedRoleDefinition() {
    }

    public PersistedRoleDefinition(String name, String description, List<PersistedPermission> permissions,
            boolean builtIn) {
        this.name = name;
        this.description = description;
        this.permissions = permissions;
        this.builtIn = builtIn;
    }
}
