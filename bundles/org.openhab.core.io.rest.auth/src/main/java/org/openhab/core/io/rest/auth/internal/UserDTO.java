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
package org.openhab.core.io.rest.auth.internal;

import java.util.Collection;

import org.openhab.core.auth.User;

/**
 * A DTO representing a {@link User}.
 *
 * @author Yannick Schaus - initial contribution
 */
public class UserDTO {
    String name;
    Collection<String> roles;

    public UserDTO(User user) {
        super();
        this.name = user.getName();
        this.roles = user.getRoles();
    }
}
