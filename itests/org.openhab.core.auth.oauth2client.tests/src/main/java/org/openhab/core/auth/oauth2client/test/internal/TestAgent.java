/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.auth.oauth2client.test.internal;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;

/**
 * For testing
 *
 * @author Gary Tse - Initial contribution
 */
@NonNullByDefault
public interface TestAgent {

    OAuthClientService testCreateClient();

    AccessTokenResponse testGetAccessTokenByResourceOwnerPasswordCredentials()
            throws OAuthException, IOException, OAuthResponseException;

    @Nullable
    OAuthClientService testGetClient(@Nullable String handle) throws OAuthException;

    AccessTokenResponse testGetAccessTokenByAuthorizationCode(String code)
            throws OAuthException, IOException, OAuthResponseException;

    AccessTokenResponse testRefreshToken() throws OAuthException, IOException, OAuthResponseException;

    AccessTokenResponse testGetCachedAccessToken() throws OAuthException, IOException, OAuthResponseException;

    @Nullable
    String testGetAuthorizationUrl(@Nullable String state) throws OAuthException;

    void close();

    void delete(String handle);
}
