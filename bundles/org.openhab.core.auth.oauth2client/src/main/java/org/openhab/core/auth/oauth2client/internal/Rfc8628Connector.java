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
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link Rfc8628Connector} is an analog of {link OAuthConnector} that provides
 * an implementation of the oAuth RFC-8628 Device Code Grant Flow authentication process.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8628">RFC-8628</a>
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class Rfc8628Connector implements AutoCloseable {

    private static final String RFC_8628_SUFFIX = "-rfc8628";
    private static final String RESOURCE_ID = "oAuth" + RFC_8628_SUFFIX;
    private static final int TIMEOUT_MILLISECONDS = 5000;

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

    private final Logger logger = LoggerFactory.getLogger(Rfc8628Connector.class);

    private final OAuthClientService oAuthClientService;
    private final OAuthStoreHandler oAuthStoreHandler;
    private final HttpClientFactory httpClientFactory;
    private final ScheduledExecutorService scheduler;
    private final String handle;
    private final Gson gson;

    private final String accesTokenRequestUrl;
    private final String deviceCodeRequestUrl;
    private final String clientIdParameter;
    private final String scopeParameter;

    private @Nullable ScheduledFuture<?> accessTokenResponsePollSchedule;
    private @Nullable DeviceCodeResponse loopDeviceCodeResponse;
    private @Nullable HttpClient httpClient;

    /**
     * Create an analog of the {link OAuthConnector} that implements the oAuth RFC-8628 Device Code
     * Grant Flow authentication process. The parameters are as follows (whereby (*) means that the
     * parameters would usually be those of the calling {@link OAuthClientService}:
     *
     * @param oAuthClientService the calling {@link OAuthClientService}
     * @param handle an oAuth storage handle (*)
     * @param oAuthStoreHandler a {@link OAuthStoreHandler} (*)
     * @param httpClientFactory a {@link HttpClientFactory} (*)
     * @param gsonBuilder a {@link GsonBuilder} (may be null) (*)
     * @param accessTokenRequestUrl the URL that provides {@link AccessTokenResponse} responses
     * @param deviceCodeRequestUrl the URL that provides {@link DeviceCodeResponse} responses
     * @param clientId the RFC-8628 request client id parameter
     * @param scope the RFC-8628 request scope parameter
     * @throws OAuthException
     */
    public Rfc8628Connector(OAuthClientService oAuthClientService, String handle, OAuthStoreHandler oAuthStoreHandler,
            HttpClientFactory httpClientFactory, @Nullable GsonBuilder gsonBuilder,
            @Nullable String accessTokenRequestUrl, @Nullable String deviceCodeRequestUrl, @Nullable String clientId,
            @Nullable String scope) throws OAuthException {

        if (accessTokenRequestUrl == null) {
            throw new OAuthException("Missing accessTokenRequestUrl");
        }
        if (deviceCodeRequestUrl == null) {
            throw new OAuthException("Missing deviceCodeRequestUrl");
        }
        if (clientId == null) {
            throw new OAuthException("Missing client ID");
        }
        if (scope == null) {
            throw new OAuthException("Missing scope");
        }

        this.handle = handle + RFC_8628_SUFFIX;
        this.oAuthClientService = oAuthClientService;
        this.oAuthStoreHandler = oAuthStoreHandler;
        this.httpClientFactory = httpClientFactory;
        this.gson = gsonBuilder != null ? gsonBuilder.create() : new Gson();
        this.scheduler = ThreadPoolManager.getScheduledPool(RESOURCE_ID);
        this.deviceCodeRequestUrl = deviceCodeRequestUrl;
        this.accesTokenRequestUrl = accessTokenRequestUrl;
        this.clientIdParameter = clientId;
        this.scopeParameter = scope;
    }

    /**
     * Begins the RFC-8628 Device Code Grant Flow authentication process.
     * Specifically it executes the following steps as described in the article in the link below:
     *
     * <ul>
     * <li>Step 1: create a request and POST it to the 'device authorize url'</li>
     * <li>Step 2: process the response and create a {@link DeviceCodeResponse}</li>
     * <li>Step 3: the user goes off to authenticate themselves at the 'user authentication url</li>
     * <li>Step 4: repeatedly create a request and POST it to the 'token url'</li>
     * <li>Step 5: repeatedly read the response and eventually create a {@link AccessTokenResponse}</li>
     * </ul>
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc8628">RFC-8628</a>
     *
     * @return if a non null URI is returned it means the user is expected to open it and authenticate themselves.
     *
     * @throws OAuthException
     * @throws IOException
     * @throws OAuthResponseException
     */
    public synchronized @Nullable String getUserAuthenticationUri()
            throws OAuthException, IOException, OAuthResponseException {
        try {
            if (oAuthClientService.isClosed()) {
                /*
                 * The oAuth service is closed:
                 * => throw an exception
                 */
                throw new OAuthException("OAuthClientService closed");
            }

            if (oAuthClientService.getAccessTokenResponse() == null) {
                /*
                 * The AccessTokenResponse exists:
                 * => no further user authentication is needed
                 * => return null
                 */
                return null;
            }

            /*
             * Prepare for a new polling cycle:
             * => cancel any prior scheduler task
             */
            stopAccessTokenResponsePollSchedule();

            /*
             * Retrieve the prior DeviceCodeResponse from the oAuth service storage (if any)
             */
            DeviceCodeResponse dcr;
            dcr = DeviceCodeResponse.createFromAccessTokenResponse(oAuthStoreHandler.loadAccessTokenResponse(handle));

            if (dcr != null) {
                /*
                 * The DeviceCodeResponse exists:
                 * => try to get the AccessTokenResponse from it
                 */
                AccessTokenResponse atr = getAccessTokenResponse(dcr);

                if (atr != null) {
                    /*
                     * The AccessTokenResponse exists:
                     * => import the AccessTokenResponse into the oAuth service storage
                     * => no further user authentication is needed
                     * => return null
                     */
                    oAuthClientService.importAccessTokenResponse(atr);
                    return null;
                }

                /*
                 * The AccessTokenResponse is missing:
                 * => probably the prior DeviceCodeResponse was bad
                 * => try to get a new DeviceCodeResponse
                 */
                dcr = getDeviceCodeResponse();
            }

            if (dcr == null || dcr.isExpired(Instant.now(), 0)) {
                /*
                 * The (eventually new) DeviceCodeResponse is (also) not valid:
                 * => throw an exception
                 */
                throw new OAuthException("DeviceCodeResponse missing or expired");
            }

            /*
             * The DeviceCodeResponse is OK:
             * => cache the DeviceCodeResponse across polling cycles in 'loopDeviceCodeResponse'
             * => save the DeviceCodeResponse in the oAuth service storage
             * => schedule a repeated task to poll for an AccessTokenResponse from the DeviceCodeResponse
             * => return the user URI from the DeviceCodeResponse
             */
            loopDeviceCodeResponse = dcr;
            oAuthStoreHandler.saveAccessTokenResponse(handle, DeviceCodeResponse.getAccessTokenResponse(dcr));
            accessTokenResponsePollSchedule = scheduler.scheduleWithFixedDelay(() -> accessTokenResponsePoll(),
                    dcr.getInterval(), dcr.getInterval(), TimeUnit.SECONDS);
            return dcr.getUserAuthenticationUri();
        } catch (GeneralSecurityException e) {
            logger.debug("getUserAuthenticationUri() error {}", e.getMessage(), e);
            throw new OAuthException(e);
        } catch (OAuthException | IOException | OAuthResponseException e) {
            logger.debug("getUserAuthenticationUri() error {}", e.getMessage(), e);
            throw e;
        }
    }

    private synchronized void stopAccessTokenResponsePollSchedule() {
        ScheduledFuture<?> scheduledFuture = accessTokenResponsePollSchedule;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        accessTokenResponsePollSchedule = null;
    }

    /**
     * Start the first steps of the Device Code Grant Flow authentication process as follows:
     *
     * <ul>
     * <li>Step 1: create a request and POST it to the authorization url</li>
     * <li>Step 2: process the response and create a {@link DeviceCodeResponse}</li>
     * </ul>
     *
     * @return a new {@link DeviceCodeResponse} object (non null)
     *
     * @throws OAuthException
     */
    private synchronized DeviceCodeResponse getDeviceCodeResponse() throws OAuthException {
        Request request = getHttpClient().newRequest(deviceCodeRequestUrl);
        request.method(HttpMethod.POST);
        request.timeout(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        request.param(PARAM_CLIENT_ID, clientIdParameter);
        request.param(PARAM_ID_SCOPE, scopeParameter);
        logger.trace("getDeviceCodeResponse() => {}", request);

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("getDeviceCodeResponse() <= {}", content);

            if (response.getStatus() == HttpStatus.OK_200) {
                Map<?, ?> jsonMap = gson.fromJson(content, Map.class);
                if (jsonMap != null) {
                    String userAuthenticationUri = asString(jsonMap.get(JSON_USER_AUTH_URI));
                    String deviceCode = asString(jsonMap.get(JSON_DEVICE_CODE));
                    int expiresIn = asInt(jsonMap.get(JSON_EXPIRES_IN));
                    int interval = asInt(jsonMap.get(JSON_INTERVAL));

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
        Request request = getHttpClient().newRequest(accesTokenRequestUrl);
        request.method(HttpMethod.POST);
        request.timeout(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        request.param(PARAM_CLIENT_ID, clientIdParameter);
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
                    String accessToken = asString(jsonMap.get(JSON_ACCESS_TOKEN));
                    String accessTokenType = asString(jsonMap.get(JSON_TOKEN_TYPE));
                    String refreshToken = asString(jsonMap.get(JSON_REFRESH_TOKEN));
                    int expiresIn = asInt(jsonMap.get(JSON_EXPIRES_IN));

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

            /*
             * Return null without throwing a exception since other HTTP responses
             * are allowed during AccessTokenResponse polling before the user has
             * completed the verification process
             */
            return null;
        } catch (Exception e) {
            throw new OAuthException("getAccessTokenResponse() error", e);
        }
    }

    private HttpClient getHttpClient() throws OAuthException {
        HttpClient httpClient = this.httpClient;
        if (httpClient == null) {
            httpClient = httpClientFactory.createHttpClient(RESOURCE_ID);
            try {
                httpClient.setConnectTimeout(TIMEOUT_MILLISECONDS);
                httpClient.start();
            } catch (Exception e) {
                throw new OAuthException("getHttpClient() error " + e.getMessage());
            }
        }
        return httpClient;
    }

    private void stopHttpClient() {
        try {
            if (httpClient != null) {
                httpClient.stop();
            }
        } catch (Exception e) {
            // nothing to do
        }
        httpClient = null;
    }

    @Override
    public void close() {
        stopAccessTokenResponsePollSchedule();
        stopHttpClient();
    }

    /**
     * This method is called repeatedly on the scheduler, until the 'stopPolling' loop control
     * variable becomes true, in order to poll for an {@link AccessTokenResponse}
     */
    private synchronized void accessTokenResponsePoll() {
        boolean stopPolling = false;

        Instant now = Instant.now();
        DeviceCodeResponse dcr = loopDeviceCodeResponse;

        AccessTokenResponse atr = null;
        if (dcr != null && !dcr.isExpired(now, 0)) {
            /*
             * The DeviceCodeResponse is OK:
             * => get an AccessTokenResponse from it
             * => in case of error, stop polling
             */
            try {
                atr = getAccessTokenResponse(dcr);
            } catch (OAuthException e) {
                logger.debug("accessTokenResponsePoll() error {}", e.getMessage(), e);
                stopPolling = true;
            }
        }

        if (atr != null) {
            /*
             * The AccessTokenResponse is OK:
             * => import the AccessTokenResponse into the oAuth service
             * => stop polling
             */
            try {
                oAuthClientService.importAccessTokenResponse(atr);
            } catch (OAuthException e) {
                logger.debug("accessTokenResponsePoll() error {}", e.getMessage(), e);
            }
            stopPolling = true;
        } else

        if (dcr == null || dcr.isExpired(now, 0)) {
            /*
             * The DeviceCodeResponse has expired:
             * => stop polling
             */
            stopPolling = true;
        }

        if (stopPolling) {
            /*
             * Polling shall be stopped:
             * => stop the AccessTokenResponse polling schedule
             * => stop the HTTP client
             */
            close();
        } else {
            /*
             * Cache the DeviceCodeResponse for the next poll
             */
            loopDeviceCodeResponse = dcr;
        }
    }

    private int asInt(@Nullable Object obj) {
        return obj instanceof Number number ? number.intValue() : 0;
    }

    private @Nullable String asString(@Nullable Object obj) {
        return obj instanceof String string ? string : null;
    }
}
