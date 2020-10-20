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
 * Credentials which represent a user API token.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class UserApiTokenCredentials implements Credentials {

    private final String userApiToken;

    /**
     * Creates a new instance
     *
     * @param userApiToken the user API token
     */
    public UserApiTokenCredentials(String userApiToken) {
        this.userApiToken = userApiToken;
    }

    /**
     * Retrieves the user API token
     *
     * @return the token
     */
    public String getApiToken() {
        return userApiToken;
    }
}
