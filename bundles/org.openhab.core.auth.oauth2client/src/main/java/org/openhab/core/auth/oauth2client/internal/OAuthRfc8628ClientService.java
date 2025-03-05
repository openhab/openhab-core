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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
 * an implementation of the oAuth Rfc8628 Device Code Grant Flow authentication process.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class OAuthRfc8628ClientService extends OAuthClientServiceImpl {

    /**
     * Private class that wraps an {@link AccessTokenResponse} and re-purposes some of its fields
     * to encapsulate the data from Rfc8628 device code responses. This allows us to use the
     * same AccessTokenResponse storage mechanism to handle DeviceCodeResponses too.
     */
    protected class DeviceCodeResponse {
        private final AccessTokenResponse atr;

        public DeviceCodeResponse() {
            atr = new AccessTokenResponse();
            atr.setCreatedOn(Instant.now());
        }

        public DeviceCodeResponse(AccessTokenResponse atr) {
            this.atr = atr;
        }

        public AccessTokenResponse getAccessTokenResponse() {
            return atr;
        }

        public String getDeviceCode() {
            return atr.getAccessToken();
        }

        public int getInterval() {
            return Integer.parseInt(atr.getState());
        }

        public String getUserUri() {
            return atr.getRefreshToken();
        }

        public boolean isExpired(Instant givenTime, int tokenExpiresInBuffer) {
            return atr.isExpired(givenTime, tokenExpiresInBuffer);
        }

        public void setDeviceCode(String deviceCode) {
            atr.setAccessToken(deviceCode);
        }

        public void setExpiresIn(int expiresIn) {
            atr.setExpiresIn(expiresIn);
        }

        public void setInterval(int interval) {
            atr.setState(String.valueOf(interval));
        }

        public void setUserUri(String userUri) {
            atr.setRefreshToken(userUri);
        }
    }

    private static final String RFC_8628_SUFFIX = "#rfc-8628";
    private static final String THREADPOOL_NAME = "oAuth" + RFC_8628_SUFFIX;
    private static final String DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
    private static final String AUTH_START_FAILED = "Rfc8628Authentication process start failed";

    public static OAuthRfc8628ClientService createInstance(String handle, OAuthStoreHandler storeHandler,
            HttpClientFactory httpClientFactory, PersistedParams params) {
        OAuthRfc8628ClientService clientService = new OAuthRfc8628ClientService(handle, params.tokenExpiresInSeconds,
                httpClientFactory, null);

        clientService.storeHandler = storeHandler;
        clientService.persistedParams = params;
        storeHandler.savePersistedParams(handle, clientService.persistedParams);

        return clientService;
    }

    private final Logger logger = LoggerFactory.getLogger(OAuthRfc8628ClientService.class);
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String dcrStorageHandle;

    private @Nullable ScheduledFuture<?> rfcStep4and5Task;
    private @Nullable DeviceCodeResponse cachedDeviceCodeResponse;

    protected OAuthRfc8628ClientService(String handle, int tokenExpiresInSeconds, HttpClientFactory httpClientFactory,
            @Nullable GsonBuilder gsonBuilder) {
        super(handle, tokenExpiresInSeconds, httpClientFactory, gsonBuilder);
        this.dcrStorageHandle = handle + RFC_8628_SUFFIX;
        this.gson = (gsonBuilder != null ? gsonBuilder : new GsonBuilder()).create();
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.scheduler = ThreadPoolManager.getScheduledPool(THREADPOOL_NAME);
    }

    /**
     * Begins the Rfc8628 Device Code Grant Flow authentication process. Specifically it executes
     * the following steps as described in the article in the link below:
     * <ul>
     * <li>Step 1: create a request and POST it to the 'device authorize url'</li>
     * <li>Step 2: process the response and create a {@link DeviceCodeResponse}</li>
     * <li>Step 3: the user goes off to authenticate themselves at the 'user authentication url</li>
     * <li>Step 4: repeatedly create a request and POST it to the 'token url'</li>
     * <li>Step 5: repeatedly read the response and eventually create a {@link AccessTokenResponse}</li>
     * </ul>
     *
     * @see <a href=
     *      "https://support.tado.com/en/articles/8565472-how-do-i-authenticate-to-access-the-rest-api">Article</a>
     *
     * @return the uri that the user shall visit to authenticate, or null if no visit is required.
     *
     * @throws OAuthResponseException
     * @throws IOException
     * @throws OAuthException
     */
    public synchronized @Nullable String getRfc8628AuthenticationUserUri()
            throws OAuthException, IOException, OAuthResponseException, GeneralSecurityException {
        if (getAccessTokenResponse() != null) {
            return null; // already authenticated
        }

        DeviceCodeResponse dcr = storageLoadDeviceCodeResponse();
        if (dcr == null || dcr.isExpired(Instant.now(), dcr.getInterval())) {
            dcr = rfcStep1and2GetDeviceCodeResponse();
        }
        cachedDeviceCodeResponse = dcr;
        storageSaveDeviceCodeResponse(dcr);

        rfcStep4and5TaskCancel();

        if (dcr == null) {
            throw new OAuthException(AUTH_START_FAILED);
        }

        rfcStep4and5Task = scheduler.scheduleWithFixedDelay(() -> rfcStep4and5RepeatTask(), dcr.getInterval(),
                dcr.getInterval(), TimeUnit.SECONDS);

        return dcr.getUserUri();
    }

    /**
     * Start the first steps of the Device Code Grant Flow authentication process as follows:
     * <ul>
     * <li>Step 1: create a request and POST it to the authorization url</li>
     * <li>Step 2: process the response and create a {@link DeviceCodeResponse}</li>
     * </ul>
     *
     * @return a new {@link DeviceCodeResponse} object or null
     */
    private synchronized @Nullable DeviceCodeResponse rfcStep1and2GetDeviceCodeResponse() {
        List<String> queryParams = new ArrayList<>();
        queryParams.add(toQueryParameter("client_id", persistedParams.clientId));
        queryParams.add(toQueryParameter("scope", persistedParams.scope));

        Request request = httpClient.newRequest(persistedParams.authorizationUrl + "?" + String.join("&", queryParams)) //
                .method(HttpMethod.POST).timeout(5, TimeUnit.SECONDS);
        logger.trace("Step 1: {}", request);

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("Step 2: {}", content);
            if (response.getStatus() == HttpStatus.OK_200) {
                Map<?, ?> tokenValues = gson.fromJson(content, Map.class);
                if (tokenValues != null) {
                    String deviceCode = toString(tokenValues.get("device_code"));
                    String userUri = toString(tokenValues.get("verification_uri_complete"));
                    int expiresIn = toInt(tokenValues.get("expires_in"));
                    int interval = toInt(tokenValues.get("interval"));

                    if (deviceCode != null && userUri != null && expiresIn > 0 && interval > 0) {
                        DeviceCodeResponse dcr = new DeviceCodeResponse();
                        dcr.setDeviceCode(deviceCode);
                        dcr.setUserUri(userUri);
                        dcr.setExpiresIn(expiresIn);
                        dcr.setInterval(interval);
                        return dcr;
                    }
                }
            }
            logger.debug("Step 2: error '{}'", response);
        } catch (

        Exception e) {
            logger.debug("Step 1: error calling {}", persistedParams.authorizationUrl, e);
        }
        return null;
    }

    private synchronized void rfcStep4and5TaskCancel() {
        ScheduledFuture<?> task = rfcStep4and5Task;
        if (task != null) {
            task.cancel(false);
        }
        rfcStep4and5Task = null;
    }

    /**
     * Whilst the user is completing the Device Code Grant Flow authentication process step 3
     * we continue, in parallel, the completion of the authentication process by repeating the
     * following steps:
     * <ul>
     * <li>Step 4: repeatedly create a request and POST it to the token url</li>
     * <li>Step 5: repeatedly read the response and eventually create a {@link AccessTokenResponse}</li>
     * </ul>
     *
     * @param deviceCode the device code
     *
     * @return an {@link AccessTokenResponse} object or null
     */
    private synchronized @Nullable AccessTokenResponse rfcStep4and5GetAccessTokenResponse(String deviceCode) {
        List<String> queryParams = new ArrayList<>();
        queryParams.add(toQueryParameter("client_id", persistedParams.clientId));
        queryParams.add(toQueryParameter("grant_type", DEVICE_GRANT));
        queryParams.add(toQueryParameter("device_code", deviceCode));

        Request request = httpClient.newRequest(persistedParams.tokenUrl + "?" + String.join("&", queryParams)) //
                .method(HttpMethod.POST).timeout(5, TimeUnit.SECONDS);
        logger.trace("Step 4: {}", request);

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("Step 5: {}", content);
            if (response.getStatus() == HttpStatus.OK_200) {
                Map<?, ?> tokenValues = gson.fromJson(content, Map.class);
                if (tokenValues != null) {
                    String accessToken = toString(tokenValues.get("access_token"));
                    String accessTokenType = toString(tokenValues.get("token_type"));
                    String refreshToken = toString(tokenValues.get("refresh_token"));
                    long expiresIn = toInt(tokenValues.get("expires_in"));

                    if (accessToken != null && refreshToken != null && accessTokenType != null && expiresIn > 0) {
                        AccessTokenResponse atr = new AccessTokenResponse();
                        atr.setCreatedOn(Instant.now());
                        atr.setAccessToken(accessToken);
                        atr.setTokenType(accessTokenType);
                        atr.setExpiresIn(expiresIn);
                        atr.setRefreshToken(refreshToken);
                        return atr;
                    }
                }
                logger.debug("Step 5: error '{}'", response);
            }
        } catch (Exception e) {
            logger.debug("Step 4: error calling {}", persistedParams.tokenUrl, e);
        }
        return null;
    }

    /**
     * Runnable polling task for step 4 and 5
     */
    private synchronized void rfcStep4and5RepeatTask() {
        AccessTokenResponse atr = null;
        Instant now = Instant.now();

        DeviceCodeResponse dcr = cachedDeviceCodeResponse;
        if (dcr != null && !dcr.isExpired(now, 5)) {
            atr = rfcStep4and5GetAccessTokenResponse(dcr.getDeviceCode());
        }

        if (atr != null) {
            try {
                importAccessTokenResponse(atr);
            } catch (OAuthException e) {
                logger.warn("Step 4: unxexpected error {}", e.getMessage(), e);
            }
        }

        if (atr != null || dcr == null || dcr.isExpired(now, 0)) {
            rfcStep4and5TaskCancel();
        }
    }

    private @Nullable DeviceCodeResponse storageLoadDeviceCodeResponse() {
        try {
            AccessTokenResponse atr = storeHandler.loadAccessTokenResponse(dcrStorageHandle);
            if (atr != null) {
                return new DeviceCodeResponse(atr);
            }
        } catch (GeneralSecurityException e) {
        }
        return null;
    }

    private void storageSaveDeviceCodeResponse(@Nullable DeviceCodeResponse dcr) {
        AccessTokenResponse atr = dcr != null ? dcr.getAccessTokenResponse() : null;
        storeHandler.saveAccessTokenResponse(dcrStorageHandle, atr);
    }

    private int toInt(@Nullable Object obj) {
        return obj instanceof Integer i ? i : 0;
    }

    private String toQueryParameter(String key, String value) {
        try {
            return key + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return key + "=" + value;
        }
    }

    private @Nullable String toString(@Nullable Object obj) {
        return obj instanceof String string ? string : null;
    }
}
