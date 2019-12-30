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
 * Credentials which represent user name and password.
 *
 * @author Łukasz Dywicki - Initial contribution
 * @author Kai Kreuzer - Added JavaDoc
 */
public class UsernamePasswordCredentials implements Credentials {

    private final String username;
    private final String password;

    /**
     * Creates a new instance
     *
     * @param username name of the user
     * @param password password of the user
     */
    public UsernamePasswordCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Retrieves the user name
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Retrieves the password
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return username + ":" + password.replaceAll(".", "*");
    }

}
