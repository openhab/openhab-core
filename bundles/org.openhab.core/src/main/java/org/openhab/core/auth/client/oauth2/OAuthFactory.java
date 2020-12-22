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
package org.openhab.core.auth.client.oauth2;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The OAuth Factory interface
 *
 * @author Michael Bock - Initial contribution
 * @author Gary Tse - ESH Adaptation
 * @author Hilbrand Bouwkamp - Change create to have a handle as parameter.
 */
@NonNullByDefault
public interface OAuthFactory {

    /**
     * Creates a new oauth service. Use this method only once to obtain a handle and store
     * this handle for further in a persistent storage container.
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
     * @return the oauth service
     */
    OAuthClientService createOAuthClientService(String handle, String tokenUrl, @Nullable String authorizationUrl,
            String clientId, @Nullable String clientSecret, @Nullable String scope,
            @Nullable Boolean supportsBasicAuth);

    /**
     * Gets the oauth service for a given handle
     *
     * @param handle the handle to the oauth service
     * @return the oauth service or null if it doesn't exist
     */
    @Nullable
    OAuthClientService getOAuthClientService(String handle);

    /**
     * Unget an oauth service, this unget/unregister the service, and frees the resources.
     * The existing tokens/ configurations (persisted parameters) are still saved
     * in the store. It will internally call {@code OAuthClientService#close()}.
     *
     * Best practise: unget, and close the OAuth service with this method!
     *
     * If OAuth service is closed directly, without using {@code #ungetOAuthService(String)},
     * then a small residual footprint is left in the cache.
     *
     * @param handle the handle to the oauth service
     */
    void ungetOAuthService(String handle);

    /**
     * This method is for unget/unregister the service,
     * then <strong>DELETE</strong> access token, configuration data from the store
     *
     * @param handle
     */
    void deleteServiceAndAccessToken(String handle);
}
