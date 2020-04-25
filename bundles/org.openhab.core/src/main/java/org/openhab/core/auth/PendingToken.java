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
import org.eclipse.jdt.annotation.Nullable;

/**
 * The pending information used in a OAuth2 authorization flow, set after the user has authorized the client to access
 * resources, and has been redirected to the callback URI with an authorization code. The information will be used when
 * it calls the token endpoint to exchange the authorization code for a access token and a refresh token.
 *
 * The authorization code for a token is sensible information while it is valid, therefore the client is supposed to
 * call the token endpoint to perform the exchange it immediately after receiving it. The information should remain in
 * the @link {@link ManagedUser} profile for a limited time only.
 *
 * Additionally, and optionally, information about the code challenge as specified by PKCE (RFC 7636) can be stored
 * along with the code.
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

    @Nullable
    private String codeChallenge;
    @Nullable
    private String codeChallengeMethod;

    /**
     * Constructs a pending token.
     *
     * @param authorizationCode the authorization code provided to the client
     * @param clientId the client ID making the request
     * @param redirectUri the provided redirect URI
     * @param scope the requested scopes
     * @param codeChallenge the code challenge (optional)
     * @param codeChallengeMethod the code challenge method (optional)
     */
    public PendingToken(String authorizationCode, String clientId, String redirectUri, String scope,
            @Nullable String codeChallenge, @Nullable String codeChallengeMethod) {
        super();
        this.authorizationCode = authorizationCode;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
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

    /**
     * Gets the code challenge
     *
     * @return the code challenge
     */
    public @Nullable String getCodeChallenge() {
        return codeChallenge;
    }

    /**
     * Gets the code challenge method
     *
     * @return the code challenge method
     */
    public @Nullable String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }
}
