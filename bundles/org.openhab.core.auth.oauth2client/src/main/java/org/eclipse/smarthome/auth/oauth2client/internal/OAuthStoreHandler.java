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
package org.eclipse.smarthome.auth.oauth2client.internal;

import java.security.GeneralSecurityException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.auth.client.oauth2.AccessTokenResponse;

/**
 * This is for OAuth client internal use.
 *
 * @author Gary Tse - Initial Contribution
 *
 */
@NonNullByDefault
public interface OAuthStoreHandler {

    /**
     * Get an AccessTokenResponse from the store. The access token and refresh token are encrypted
     * and therefore will be decrypted before returning.
     *
     * If the storage is not available, it is still possible to get the AccessTokenResponse from memory cache.
     * However, the last-used statistics will be broken. It is a measured risk to take.
     *
     * @param handle the handle given by the call
     *            {@code OAuthFactory#createOAuthClientService(String, String, String, String, String, Boolean)}
     * @return AccessTokenResponse if available, null if not.
     * @throws GeneralSecurityException when the token cannot be decrypted.
     */
    @Nullable
    AccessTokenResponse loadAccessTokenResponse(String handle) throws GeneralSecurityException;

    /**
     * Save the {@code AccessTokenResponse} by the handle
     *
     * @param handle unique string used as a handle/ reference to the OAuth client service, and the underlying
     *            access tokens, configs.
     * @param accessTokenResponse This can be null, which explicitly removes the AccessTokenResponse from store.
     */
    void saveAccessTokenResponse(String handle, @Nullable AccessTokenResponse accessTokenResponse);

    /**
     * Remove the token for the given handler. No exception is thrown in all cases
     *
     * @param handle unique string used as a handle/ reference to the OAuth client service, and the underlying
     *            access tokens, configs.
     */
    void remove(String handle);

    /**
     * Remove all data in the oauth store, !!!use with caution!!!
     */
    void removeAll();

    /**
     * Save the {@code PersistedParams} into the store
     *
     * @param handle unique string used as a handle/ reference to the OAuth client service, and the underlying
     *            access tokens, configs.
     * @param persistedParams These parameters are static with respect to the oauth provider and thus can be persisted.
     */
    void savePersistedParams(String handle, @Nullable PersistedParams persistedParams);

    /**
     * Load the {@code PersistedParams} from the store
     *
     * @param handle unique string used as a handle/ reference to the OAuth client service, and the underlying
     *            access tokens, configs.
     * @return PersistedParams when available, null if not exist
     */
    @Nullable
    PersistedParams loadPersistedParams(String handle);
}
