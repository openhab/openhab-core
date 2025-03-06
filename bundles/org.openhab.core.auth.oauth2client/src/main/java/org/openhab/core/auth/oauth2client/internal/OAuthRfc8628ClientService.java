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
package org.openhab.core.auth.oauth2client.internal;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.DeviceCodeResponse;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link OAuthRfc8628ClientService} extends {@link OAuthClientServiceImpl} to provide
 * an implementation of the oAuth RFC-8628 Device Code Grant Flow authentication process.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class OAuthRfc8628ClientService extends OAuthClientServiceImpl {

    private static final String RFC_8628_SUFFIX = "#rfc8628";
    private static final String RESOURCE_ID = "oAuth" + RFC_8628_SUFFIX;

    // URL parameter names
    private static final String PARAM_ID_SCOPE = "scope";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_DEVICE_CODE = "device_code";
    private static final String PARAM_GRANT_TYPE = "grant_type";

    // URL parameter values
    private static final String PARAM_GRANT_TYPE_VALUE = "urn:ietf:params:oauth:grant-type:device_code";

    // JSON element names
    private static final String JSON_ACCESS_TOKEN = "access_token";
    private static final String JSON_REFRESH_TOKEN = "refresh_token";
    private static final String JSON_TOKEN_TYPE = "token_type";
    private static final String JSON_INTERVAL = "interval";
    private static final String JSON_EXPIRES_IN = "expires_in";
    private static final String JSON_USER_AUTH_URI = "verification_uri_complete";
    private static final String JSON_DEVICE_CODE = PARAM_DEVICE_CODE;

    private final Logger logger = LoggerFactory.getLogger(OAuthRfc8628ClientService.class);

    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String dcrStorageHandle;

    private @Nullable ScheduledFuture<?> scheduledAccessTokenResponsePolling;
    private @Nullable DeviceCodeResponse pollingDeviceCodeResponse;

    protected OAuthRfc8628ClientService(String handle, int tokenExpiresInSeconds, HttpClientFactory httpClientFactory,
            @Nullable GsonBuilder gsonBuilder) {
        super(handle, tokenExpiresInSeconds, httpClientFactory, gsonBuilder);

        this.scheduler = ThreadPoolManager.getScheduledPool(RESOURCE_ID);
        this.httpClient = httpClientFactory.createHttpClient(RESOURCE_ID);
        this.gson = (gsonBuilder != null ? gsonBuilder : new GsonBuilder()).create();
        this.dcrStorageHandle = handle + RFC_8628_SUFFIX;
    }

    /**
     * It should only be used internally, thus the access is package level
     *
     * @param handle The handle produced previously from
     *            {@link org.openhab.core.auth.client.oauth2.OAuthFactory#createOAuthRfc8628ClientService}
     * @param storeHandler Storage handler
     * @param httpClientFactory Http client factory
     * @param params These parameters are static with respect to the OAuth provider and thus can be persisted.
     * @return OAuthRfc8628ClientService (extends OAuthClientServiceImpl) an instance
     */
    static OAuthRfc8628ClientService createInstance(String handle, OAuthStoreHandler storeHandler,
            HttpClientFactory httpClientFactory, PersistedParams params) {
        OAuthRfc8628ClientService clientService = new OAuthRfc8628ClientService(handle, params.tokenExpiresInSeconds,
                httpClientFactory, null);

        clientService.storeHandler = storeHandler;
        clientService.persistedParams = params;
        storeHandler.savePersistedParams(handle, clientService.persistedParams);

        return clientService;
    }

    /**
     * It should only be used internally, thus the access is package level
     *
     * @param handle The handle produced previously from
     *            {@link org.openhab.core.auth.client.oauth2.OAuthFactory#createOAuthClientService}
     * @param storeHandler Storage handler
     * @param tokenExpiresInSeconds Positive integer; a small time buffer in seconds. It is used to calculate the expiry
     *            of the access tokens. This allows the access token to expire earlier than the
     *            official stated expiry time; thus prevents the caller obtaining a valid token at the time of invoke,
     *            only to find the token immediately expired.
     * @param httpClientFactory Http client factory
     * @return new instance of OAuthClientServiceImpl or null if it doesn't exist
     * @throws IllegalStateException if store is not available.
     */
    static @Nullable OAuthRfc8628ClientService getInstance(String handle, OAuthStoreHandler storeHandler,
            int tokenExpiresInSeconds, HttpClientFactory httpClientFactory) {
        // Load parameters from Store
        PersistedParams persistedParamsFromStore = storeHandler.loadPersistedParams(handle);
        if (persistedParamsFromStore == null) {
            return null;
        }
        OAuthRfc8628ClientService clientService = new OAuthRfc8628ClientService(handle, tokenExpiresInSeconds,
                httpClientFactory, null);
        clientService.storeHandler = storeHandler;
        clientService.persistedParams = persistedParamsFromStore;

        return clientService;
    }

    @Override
    public synchronized @Nullable String getUserAuthenticationUri()
            throws OAuthException, IOException, OAuthResponseException {
        try {
            if (getAccessTokenResponse() != null) {
                // the AccessTokenResponse is OK; return null (authentication not required)
                return null;
            }

            httpClientStart();

            // retrieve the DeviceCodeResponse from storage
            DeviceCodeResponse dcr;
            dcr = DeviceCodeResponse
                    .createFromAccessTokenResponse(storeHandler.loadAccessTokenResponse(dcrStorageHandle));

            if (dcr != null) {
                // the DeviceCodeResponse is OK; get the AccessTokenResponse from it
                AccessTokenResponse atr = getAccessTokenResponse(dcr);

                if (atr != null) {
                    // the AccessTokenResponse is OK; store it and return null (authentication not required)
                    importAccessTokenResponse(atr);
                    return null;
                }

                // no AccessTokenResponse; replace the prior (i.e. invalid) DeviceCodeResponse
                dcr = getDeviceCodeResponse();
            }

            if (dcr == null || dcr.isExpired(Instant.now(), 0)) {
                // the DeviceCodeResponse is bad; replace it
                dcr = getDeviceCodeResponse();
            }

            // cancel any prior AccessTokenResponse polling task
            cancelAccessTokenResponsePolling();

            // the DeviceCodeResponse is OK; cache it locally, store it, and start polling for an AccessTokenResponse
            pollingDeviceCodeResponse = dcr;
            storeHandler.saveAccessTokenResponse(dcrStorageHandle, DeviceCodeResponse.getAccessTokenResponse(dcr));

            scheduledAccessTokenResponsePolling = scheduler.scheduleWithFixedDelay(() -> pollAccessTokenResponse(),
                    dcr.getInterval(), dcr.getInterval(), TimeUnit.SECONDS);

            return dcr.getUserAuthenticationUri();
        } catch (GeneralSecurityException e) {
            logger.debug("getUserAuthenticationUri() error {}", e.getMessage(), e);
            throw new OAuthException(e);
        } catch (OAuthException | IOException | OAuthResponseException e) {
            logger.debug("getUserAuthenticationUri() error {}", e.getMessage(), e);
            throw e;
        } finally {
            httpClientStop();
        }
    }

    private synchronized void cancelAccessTokenResponsePolling() {
        ScheduledFuture<?> task = scheduledAccessTokenResponsePolling;
        if (task != null) {
            task.cancel(false);
        }
        scheduledAccessTokenResponsePolling = null;
    }

    @Override
    public void close() {
        cancelAccessTokenResponsePolling();
        httpClientStop();
        scheduler.close();
        super.close();
    }

    /**
     * Start the first steps of the Device Code Grant Flow authentication process as follows:
     *
     * <ul>
     * <li>Step 1: create a request and POST it to the authorization url</li>
     * <li>Step 2: process the response and create a {@link DeviceCodeResponse}</li>
     * </ul>
     *
     * @return a new {@link DeviceCodeResponse} object or null
     *
     * @throws OAuthException
     */
    private synchronized DeviceCodeResponse getDeviceCodeResponse() throws OAuthException {
        String url = persistedParams.authorizationUrl;
        Request request = httpClient.newRequest(url).method(HttpMethod.POST).timeout(5, TimeUnit.SECONDS);
        request.param(PARAM_CLIENT_ID, persistedParams.clientId);
        request.param(PARAM_ID_SCOPE, persistedParams.scope);
        logger.trace("getDeviceCodeResponse() => {}", request);

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("getDeviceCodeResponse() <= {}", content);

            if (response.getStatus() == HttpStatus.OK_200) {
                Map<?, ?> jsonMap = gson.fromJson(content, Map.class);
                if (jsonMap != null) {
                    String userAuthenticationUri = toString(jsonMap.get(JSON_USER_AUTH_URI));
                    String deviceCode = toString(jsonMap.get(JSON_DEVICE_CODE));
                    int expiresIn = toInt(jsonMap.get(JSON_EXPIRES_IN));
                    int interval = toInt(jsonMap.get(JSON_INTERVAL));

                    if (deviceCode != null && userAuthenticationUri != null && expiresIn > 0 && interval > 0) {
                        DeviceCodeResponse dcr = new DeviceCodeResponse();
                        dcr.setDeviceCode(deviceCode);
                        dcr.setUserAuthenticationUri(userAuthenticationUri);
                        dcr.setExpiresIn(expiresIn);
                        dcr.setInterval(interval);
                        logger.trace("getDeviceCodeResponse() {}", dcr);
                        return dcr;
                    }
                }
            }
            throw new OAuthException("getDeviceCodeResponse() error " + response);
        } catch (Exception e) {
            throw new OAuthException("getDeviceCodeResponse() error", e);
        }
    }

    /**
     * Whilst the user is completing the Device Code Grant Flow authentication process step 3
     * we continue, in parallel, the completion of the authentication process by repeating the
     * following steps:
     *
     * <ul>
     * <li>Step 4: repeatedly create a request and POST it to the token url</li>
     * <li>Step 5: repeatedly read the response and eventually create a {@link AccessTokenResponse}</li>
     * </ul>
     *
     * @param dcr the {@link DeviceCodeResponse}
     *
     * @return an {@link AccessTokenResponse} object or null
     * @throws OAuthException
     */
    private synchronized @Nullable AccessTokenResponse getAccessTokenResponse(DeviceCodeResponse dcr)
            throws OAuthException {
        String url = persistedParams.tokenUrl;
        Request request = httpClient.newRequest(url).method(HttpMethod.POST).timeout(5, TimeUnit.SECONDS);
        request.param(PARAM_CLIENT_ID, persistedParams.clientId);
        request.param(PARAM_GRANT_TYPE, PARAM_GRANT_TYPE_VALUE);
        request.param(PARAM_DEVICE_CODE, dcr.getDeviceCode());
        logger.trace("getAccessTokenResponse() => {}", request);

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("getAccessTokenResponse() <= {}", content);

            if (response.getStatus() == HttpStatus.OK_200) {
                Map<?, ?> jsonMap = gson.fromJson(content, Map.class);
                if (jsonMap != null) {
                    String accessToken = toString(jsonMap.get(JSON_ACCESS_TOKEN));
                    String accessTokenType = toString(jsonMap.get(JSON_TOKEN_TYPE));
                    String refreshToken = toString(jsonMap.get(JSON_REFRESH_TOKEN));
                    int expiresIn = toInt(jsonMap.get(JSON_EXPIRES_IN));

                    if (accessToken != null && refreshToken != null && accessTokenType != null && expiresIn > 0) {
                        AccessTokenResponse atr = new AccessTokenResponse();
                        atr.setCreatedOn(Instant.now());
                        atr.setAccessToken(accessToken);
                        atr.setTokenType(accessTokenType);
                        atr.setExpiresIn(expiresIn);
                        atr.setRefreshToken(refreshToken);
                        logger.trace("getAccessTokenResponse() {}", atr);
                        return atr;
                    }
                }
                throw new OAuthException("getAccessTokenResponse() error");
            }
            // return without exception since other HTTP responses are allowed during AccessTokenResponse polling
            return null;
        } catch (Exception e) {
            throw new OAuthException("getAccessTokenResponse() error", e);
        }
    }

    private void httpClientStart() throws OAuthException {
        if (!httpClient.isStarted()) {
            try {
                httpClient.start();
            } catch (Exception e) {
                throw new OAuthException("httpClientStart() error " + e.getMessage());
            }
        }
    }

    private void httpClientStop() {
        try {
            httpClient.stop();
        } catch (Exception e) {
        }
    }

    /**
     * Runnable task for polling for an {@link AccessTokenResponse}
     */
    private synchronized void pollAccessTokenResponse() {
        Instant now = Instant.now();
        DeviceCodeResponse dcr = pollingDeviceCodeResponse;

        // the DeviceCodeResponse is OK; get the AccessTokenResponse
        AccessTokenResponse atr = null;
        if (dcr != null && !dcr.isExpired(now, 0)) {
            try {
                atr = getAccessTokenResponse(dcr);
            } catch (OAuthException e) {
                logger.debug("pollAccessTokenResponse() error {}", e.getMessage(), e);
                cancelAccessTokenResponsePolling();
            }
        }

        // the AccessTokenResponse is OK; store it, and cancel the AccessTokenResponse polling
        if (atr != null) {
            try {
                importAccessTokenResponse(atr);
            } catch (OAuthException e) {
                logger.debug("pollAccessTokenResponse() error {}", e.getMessage(), e);
            }
            cancelAccessTokenResponsePolling();
        } else

        // the DeviceCodeResponse has expired; cancel the AccessTokenResponse polling
        if (dcr == null || dcr.isExpired(now, 0)) {
            cancelAccessTokenResponsePolling();
        }

        // getting here means the AccessTokenResponse polling will continue
    }

    private int toInt(@Nullable Object obj) {
        return obj instanceof Number number ? number.intValue() : 0;
    }

    private @Nullable String toString(@Nullable Object obj) {
        return obj instanceof String string ? string : null;
    }
}
