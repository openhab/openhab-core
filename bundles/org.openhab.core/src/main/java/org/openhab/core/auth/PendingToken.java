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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The pending information used in a OAuth2 authorization flow, set after the user has authorized the client to access
 * resources, and has been redirected to the callback URI with an authorization code. The information will be used when
 * it calls the token endpoint to exchange the authorization code for a access token and a refresh token.
 *
 * The authorization code for a token is sensible information while it is valid, therefore the client is supposed to
 * call the token endpoint to perform the exchange it immediately after receiving it. The information should remain in
 * the @link {@link ManagedUser} profile for a limited time only.
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
public class PendingToken {
    private String authorizationCode;
    private String clientId;
    private String redirectUri;
    private String scope;

    /**
     * Constructs a pending token.
     *
     * @param authorizationCode the authorization code provided to the client
     * @param clientId the client ID making the request
     * @param redirectUri the provided redirect URI
     * @param scope the requested scopes
     */
    public PendingToken(String authorizationCode, String clientId, String redirectUri, String scope) {
        super();
        this.authorizationCode = authorizationCode;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    /**
     * Gets the authorization code provided to the client.
     *
     * @return the authorization code
     */
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    /**
     * Gets the ID of the client requesting the upcoming token.
     *
     * @return the client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the redirect URI of the client requesting the upcoming token.
     *
     * @return the redirect URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Gets the requested scopes for the upcoming token.
     *
     * @return the requested scopes
     */
    public String getScope() {
        return scope;
    }
}
