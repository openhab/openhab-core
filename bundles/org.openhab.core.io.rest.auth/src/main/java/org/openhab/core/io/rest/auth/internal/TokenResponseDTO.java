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

import org.openhab.core.auth.User;

/**
 * A DTO object for a successful token endpoint response, as per RFC 6749, Section 5.1.
 *
 * {@linkplain https://tools.ietf.org/html/rfc6749#section-5.1}
 *
 * @author Yannick Schaus - initial contribution
 */
public class TokenResponseDTO {
    public String access_token;
    public String token_type;
    public Integer expires_in;
    public String refresh_token;
    public String scope;
    public UserDTO user;

    /**
     * Builds a successful response containing token information.
     *
     * @param access_token the access token
     * @param token_type the type of the token, normally "bearer"
     * @param expires_in the expiration time of the access token in seconds
     * @param refresh_token the refresh token which can be used to get additional tokens
     * @param scope the request scope
     * @param user the user object, an additional parameter not part of the specification
     */
    public TokenResponseDTO(String access_token, String token_type, Integer expires_in, String refresh_token,
            String scope, User user) {
        super();
        this.access_token = access_token;
        this.token_type = token_type;
        this.expires_in = expires_in;
        this.refresh_token = refresh_token;
        this.scope = scope;
        this.user = new UserDTO(user);
    }
}
