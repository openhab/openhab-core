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

/**
 * Interface defining constants for roles within ESH.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public interface Role {

    /**
     * Role of users with administrative rights
     */
    final String ADMIN = "administrator";

    /**
     * Role of a regular user without any exceptional permissions or restrictions
     */
    final String USER = "user";

}
