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

import java.security.Principal;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;

/**
 * A user represents an individual, physical person using the system.
 *
 * @author Yannick Schaus - initial contribution
 */
@NonNullByDefault
public interface User extends Principal, Identifiable<String> {

    /**
     * Gets the roles attributed to the user.
     *
     * @see Role
     * @return role attributed to the user
     */
    public Set<String> getRoles();
}
