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

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A persistent session for a {@link ManagedUser}, which holds a refresh token used by a client to get short-lived
 * access tokens for API requests authorization.
 *
 * @author Yannick Schaus - initial contribution
 *
 */
@NonNullByDefault
public class UserSession {
    String sessionId;
    String refreshToken;
    Date createdTime;
    Date lastRefreshTime;
    String clientId;
    String redirectUri;
    String scope;
    boolean sessionCookie;

    /**
     * Constructs a new session.
     *
     * @param sessionId an unique ID for the session
     * @param refreshToken the refresh token associated to the session
     * @param clientId the client ID associated to the session
     * @param redirectUri the callback URI provided when the client was authorized by the user
     * @param scope the granted scope provided when the client was authorized by the user
     */
    public UserSession(String sessionId, String refreshToken, String clientId, String redirectUri, String scope) {
        super();
        this.sessionId = sessionId;
        this.refreshToken = refreshToken;
        this.createdTime = new Date();
        this.lastRefreshTime = new Date();
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    /**
     * Gets the ID of the session.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the refresh token for the session.
     *
     * @return the refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Gets the creation time of the session.
     *
     * @return the creation time
     */
    public Date getCreatedTime() {
        return createdTime;
    }

    /**
     * Gets the time when the refresh token was last used to get a new access token.
     *
     * @return the last refresh time
     */
    public Date getLastRefreshTime() {
        return lastRefreshTime;
    }

    /**
     * Sets the time when the refresh token was last used to get a new access token.
     *
     * @param lastRefreshTime the last refresh time
     */
    public void setLastRefreshTime(Date lastRefreshTime) {
        this.lastRefreshTime = lastRefreshTime;
    }

    /**
     * Gets the scope requested when authorizing this session.
     *
     * @return the session scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * Gets the ID of the client this session was created for
     *
     * @return the client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the redirect URI which was used to perform the authorization flow.
     *
     * @return the redirect URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Specifies whether this session is supported by a session cookie, to mitigate the impact of refresh token
     * leakage.
     *
     * @return whether or not a cookie has been set
     */
    public boolean hasSessionCookie() {
        return sessionCookie;
    }

    /**
     * Sets the session cookie flag for this session.
     *
     * @param sessionCookie the cookie flag
     */
    public void setSessionCookie(boolean sessionCookie) {
        this.sessionCookie = sessionCookie;
    }
}
