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

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Credentials which represent a user API token.
 *
 * @author Yannick Schaus - Initial contribution
 */
@NonNullByDefault
public class UserApiTokenCredentials implements Credentials {

    // All access must be guarded by "this"
    private final char[] userApiToken;

    /**
     * Creates a new instance
     *
     * @deprecated Tokens should not be stored as {@link String}s, use {@link #UserApiTokenCredentials(char[])}
     *             instead.
     *
     * @param userApiToken the user API token
     */
    @Deprecated
    public UserApiTokenCredentials(String userApiToken) {
        this.userApiToken = userApiToken.toCharArray();
    }

    /**
     * Creates a new instance
     *
     * @param userApiToken the user API token
     */
    public UserApiTokenCredentials(char[] userApiToken) {
        this.userApiToken = userApiToken;
    }

    /**
     * Retrieves the user API token
     *
     * @return the token
     */
    public synchronized char[] getApiToken() {
        return userApiToken;
    }

    @Override
    public synchronized void dispose() {
        Arrays.fill(userApiToken, Character.MIN_VALUE);
    }
}
