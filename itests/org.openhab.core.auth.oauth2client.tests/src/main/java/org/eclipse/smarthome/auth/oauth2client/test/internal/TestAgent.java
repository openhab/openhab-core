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
package org.eclipse.smarthome.auth.oauth2client.test.internal;

import java.io.IOException;

import org.eclipse.smarthome.core.auth.client.oauth2.AccessTokenResponse;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthClientService;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthException;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthResponseException;

/**
 * For testing
 *
 * @author Gary Tse - initial contribution
 *
 */
public interface TestAgent {

    OAuthClientService testCreateClient();

    AccessTokenResponse testGetAccessTokenByResourceOwnerPasswordCredentials()
            throws OAuthException, IOException, OAuthResponseException;

    OAuthClientService testGetClient(String handle) throws OAuthException;

    AccessTokenResponse testGetAccessTokenByAuthorizationCode(String code)
            throws OAuthException, IOException, OAuthResponseException;

    AccessTokenResponse testRefreshToken() throws OAuthException, IOException, OAuthResponseException;

    AccessTokenResponse testGetCachedAccessToken() throws OAuthException, IOException, OAuthResponseException;

    String testGetAuthorizationUrl(String state) throws OAuthException;

    void close();

    void delete(String handle);

}
