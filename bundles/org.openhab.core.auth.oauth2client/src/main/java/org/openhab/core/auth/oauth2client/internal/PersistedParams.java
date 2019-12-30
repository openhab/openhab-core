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
package org.openhab.core.auth.oauth2client.internal;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Params that need to be persisted.
 *
 * @author Michael Bock - Initial contribution
 * @author Gary Tse - Initial contribution
 * @author Hilbrand Bouwkamp - Moved class to it's own file and added hashCode and equals methods
 */
class PersistedParams {
    String handle;
    String tokenUrl;
    String authorizationUrl;
    String clientId;
    String clientSecret;
    String scope;
    Boolean supportsBasicAuth;
    String state;
    String redirectUri;
    int tokenExpiresInSeconds = 60;

    /**
     * Default constructor needed for json serialization.
     */
    public PersistedParams() {
    }

    /**
     * Constructor.
     *
     * @param handle the handle to the oauth service
     * @param tokenUrl the token url of the oauth provider. This is used for getting access token.
     * @param authorizationUrl the authorization url of the oauth provider. This is used purely for generating
     *            authorization code/ url.
     * @param clientId the client id
     * @param clientSecret the client secret (optional)
     * @param scope the desired scope
     * @param supportsBasicAuth whether the OAuth provider supports basic authorization or the client id and client
     *            secret should be passed as form params. true - use http basic authentication, false - do not use http
     *            basic authentication, null - unknown (default to do not use)
     * @param tokenExpiresInSeconds Positive integer; a small time buffer in seconds. It is used to calculate the expiry
     *            of the access tokens. This allows the access token to expire earlier than the
     *            official stated expiry time; thus prevents the caller obtaining a valid token at the time of invoke,
     *            only to find the token immediately expired.
     */
    public PersistedParams(String handle, String tokenUrl, String authorizationUrl, String clientId,
            String clientSecret, String scope, Boolean supportsBasicAuth, int tokenExpiresInSeconds) {
        this.handle = handle;
        this.tokenUrl = tokenUrl;
        this.authorizationUrl = authorizationUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.supportsBasicAuth = supportsBasicAuth;
        this.tokenExpiresInSeconds = tokenExpiresInSeconds;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((authorizationUrl == null) ? 0 : authorizationUrl.hashCode());
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result + ((clientSecret == null) ? 0 : clientSecret.hashCode());
        result = prime * result + ((handle == null) ? 0 : handle.hashCode());
        result = prime * result + ((redirectUri == null) ? 0 : redirectUri.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + ((supportsBasicAuth == null) ? 0 : supportsBasicAuth.hashCode());
        result = prime * result + tokenExpiresInSeconds;
        result = prime * result + ((tokenUrl == null) ? 0 : tokenUrl.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PersistedParams other = (PersistedParams) obj;
        if (authorizationUrl == null) {
            if (other.authorizationUrl != null) {
                return false;
            }
        } else if (!authorizationUrl.equals(other.authorizationUrl)) {
            return false;
        }
        if (clientId == null) {
            if (other.clientId != null) {
                return false;
            }
        } else if (!clientId.equals(other.clientId)) {
            return false;
        }
        if (clientSecret == null) {
            if (other.clientSecret != null) {
                return false;
            }
        } else if (!clientSecret.equals(other.clientSecret)) {
            return false;
        }
        if (handle == null) {
            if (other.handle != null) {
                return false;
            }
        } else if (!handle.equals(other.handle)) {
            return false;
        }
        if (redirectUri == null) {
            if (other.redirectUri != null) {
                return false;
            }
        } else if (!redirectUri.equals(other.redirectUri)) {
            return false;
        }
        if (scope == null) {
            if (other.scope != null) {
                return false;
            }
        } else if (!scope.equals(other.scope)) {
            return false;
        }
        if (state == null) {
            if (other.state != null) {
                return false;
            }
        } else if (!state.equals(other.state)) {
            return false;
        }
        if (supportsBasicAuth == null) {
            if (other.supportsBasicAuth != null) {
                return false;
            }
        } else if (!supportsBasicAuth.equals(other.supportsBasicAuth)) {
            return false;
        }
        if (tokenExpiresInSeconds != other.tokenExpiresInSeconds) {
            return false;
        }
        if (tokenUrl == null) {
            if (other.tokenUrl != null) {
                return false;
            }
        } else if (!tokenUrl.equals(other.tokenUrl)) {
            return false;
        }
        return true;
    }

}
