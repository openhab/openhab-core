/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.auth;

/**
 * Interface defining constants for roles within ESH.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
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
