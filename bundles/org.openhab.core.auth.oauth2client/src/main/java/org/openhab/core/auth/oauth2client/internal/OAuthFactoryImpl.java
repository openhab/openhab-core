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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link OAuthFactory}.
 *
 * @author Michael Bock - Initial contribution
 * @author Gary Tse - ESH adaptation
 * @author Hilbrand Bouwkamp - Changed implementation of createOAuthClientService
 */
@NonNullByDefault
@Component
public class OAuthFactoryImpl implements OAuthFactory {

    private final Logger logger = LoggerFactory.getLogger(OAuthFactoryImpl.class);

    private final OAuthStoreHandler oAuthStoreHandler;
    private final HttpClientFactory httpClientFactory;

    private int tokenExpiresInBuffer = OAuthClientServiceImpl.DEFAULT_TOKEN_EXPIRES_IN_BUFFER_SECOND;

    private final Map<String, OAuthClientService> oauthClientServiceCache = new ConcurrentHashMap<>();

    @Activate
    public OAuthFactoryImpl(final @Reference HttpClientFactory httpClientFactory,
            final @Reference OAuthStoreHandler oAuthStoreHandler) {
        this.httpClientFactory = httpClientFactory;
        this.oAuthStoreHandler = oAuthStoreHandler;
    }

    @Deactivate
    public void deactivate() {
        // close each service
        for (OAuthClientService clientServiceImpl : oauthClientServiceCache.values()) {
            clientServiceImpl.close();
        }
        oauthClientServiceCache.clear();
    }

    @Override
    public OAuthClientService createOAuthClientService(String handle, String tokenUrl,
            @Nullable String authorizationUrl, String clientId, @Nullable String clientSecret, @Nullable String scope,
            @Nullable Boolean supportsBasicAuth) {
        PersistedParams params = oAuthStoreHandler.loadPersistedParams(handle);
        PersistedParams newParams = new PersistedParams(handle, tokenUrl, authorizationUrl, clientId, clientSecret,
                scope, supportsBasicAuth, tokenExpiresInBuffer, null);
        OAuthClientService clientImpl = null;

        // If parameters in storage and parameters are the same as arguments passed get the client from storage
        if (params != null && params.equals(newParams)) {
            clientImpl = getOAuthClientService(handle);
        }
        // If no client with parameters or with different parameters create or update (if parameters are different)
        // client in storage.
        if (clientImpl == null) {
            clientImpl = OAuthClientServiceImpl.createInstance(handle, oAuthStoreHandler, httpClientFactory, newParams);
            oauthClientServiceCache.put(handle, clientImpl);
        }
        return clientImpl;
    }

    @Override
    public @Nullable OAuthClientService getOAuthClientService(String handle) {
        OAuthClientService clientImpl = oauthClientServiceCache.get(handle);

        if (clientImpl == null || clientImpl.isClosed()) {
            // This happens after reboot, or client was closed without factory knowing; create a new client
            // the store has the handle/config data
            clientImpl = OAuthClientServiceImpl.getInstance(handle, oAuthStoreHandler, tokenExpiresInBuffer,
                    httpClientFactory);
            if (clientImpl == null) {
                return null;
            }
            oauthClientServiceCache.put(handle, clientImpl);
        }
        return clientImpl;
    }

    @SuppressWarnings("null")
    @Override
    public void ungetOAuthService(String handle) {
        OAuthClientService clientImpl = oauthClientServiceCache.get(handle);

        if (clientImpl == null) {
            logger.debug("{} handle not found. Cannot unregisterOAuthServie", handle);
            return;
        }
        clientImpl.close();
        oauthClientServiceCache.remove(handle);
    }

    @Override
    public void deleteServiceAndAccessToken(String handle) {
        OAuthClientService clientImpl = oauthClientServiceCache.get(handle);

        if (clientImpl != null) {
            try {
                clientImpl.remove();
            } catch (OAuthException e) {
                // client was already closed, does not matter
            }
            oauthClientServiceCache.remove(handle);
        }
        oAuthStoreHandler.remove(handle);
    }

    public int getTokenExpiresInBuffer() {
        return tokenExpiresInBuffer;
    }

    public void setTokenExpiresInBuffer(int bufferInSeconds) {
        tokenExpiresInBuffer = bufferInSeconds;
    }
}
