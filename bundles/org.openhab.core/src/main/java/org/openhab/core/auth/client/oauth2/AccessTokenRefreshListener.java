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

/**
 * Handler to act up on changes of the access token.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@NonNullByDefault
public interface AccessTokenRefreshListener {

    /**
     * Notifies of a successful token response from {@link OAuthClientService#refreshToken()}.
     *
     * @param tokenResponse token response
     */
    void onAccessTokenResponse(AccessTokenResponse tokenResponse);
}
