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
import java.util.Objects;
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

import com.google.gson.GsonBuilder;

/**
 * The {@link OAuthConnectorRFC8628} extends {@link OAuthConnector} to implement
 * the oAuth RFC-8628 Device Code Grant Flow authentication process.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8628">RFC-8628</a>
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class OAuthConnectorRFC8628 extends OAuthConnector implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(OAuthConnectorRFC8628.class);

    // URL parameter names
    private static final String PARAM_ID_SCOPE = "scope";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_DEVICE_CODE = "device_code";
    private static final String PARAM_GRANT_TYPE = "grant_type";

    // URL parameter values
    private static final String PARAM_GRANT_TYPE_VALUE = "urn:ietf:params:oauth:grant-type:device_code";

    private static final String SCHEDULER_POOL_NAME = "oauth-rfc8628";
    private static final int HTTP_TIMEOUT_MILLISECONDS = 5000;

    private final OAuthClientService oAuthClientService;
    private final OAuthStoreHandler oAuthStoreHandler;
    private final ScheduledExecutorService scheduler;
    private final String handle;

    private final String accessTokenRequestUrl;
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
     *
     * @throws OAuthException
     */
    public OAuthConnectorRFC8628(OAuthClientService oAuthClientService, String handle,
            OAuthStoreHandler oAuthStoreHandler, HttpClientFactory httpClientFactory, @Nullable GsonBuilder gsonBuilder,
            String accessTokenRequestUrl, String deviceCodeRequestUrl, String clientId, String scope)
            throws OAuthException {
        super(httpClientFactory, null, gsonBuilder != null ? gsonBuilder : new GsonBuilder());
        this.oAuthClientService = oAuthClientService;
        this.oAuthStoreHandler = oAuthStoreHandler;
        this.scheduler = ThreadPoolManager.getScheduledPool(SCHEDULER_POOL_NAME);
        this.deviceCodeRequestUrl = deviceCodeRequestUrl;
        this.accessTokenRequestUrl = accessTokenRequestUrl;
        this.clientIdParameter = clientId;
        this.scopeParameter = scope;
        this.handle = handle;
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
            logger.trace("getUserAuthenticationUri() start..");
            if (oAuthClientService.isClosed()) {
                /*
                 * The oAuth service is closed:
                 * => throw an exception
                 */
                throw new OAuthException("OAuthClientService closed");
            }

            /*
             * Retrieve existing AccessTokenResponse from oAuth service (if any)
             */
            AccessTokenResponse atr = oAuthClientService.getAccessTokenResponse();
            logger.trace("getUserAuthenticationUri() got token:\n{}", atr);

            if (atr != null) {
                /*
                 * The AccessTokenResponse exists:
                 * => no further user authentication is needed
                 * => return null
                 */
                return null;
            }

            /*
             * Prepare for a new polling cycle:
             * => stop any prior AccessTokenResponse polling schedule
             * => close any prior HTTP client
             * => create a new HTTP client
             */
            close();
            httpClient = createHttpClient(accessTokenRequestUrl);

            /*
             * Retrieve existing DeviceCodeResponse from oAuth service storage (if any)
             */
            DeviceCodeResponse dcr = oAuthStoreHandler.loadDeviceCodeResponse(handle);
            logger.trace("getUserAuthenticationUri() loaded dcr:\n{}", dcr);

            if (dcr == null || dcr.isExpired(Instant.now(), 0)) {
                /*
                 * The DeviceCodeResponse from storage is missing or invalid:
                 * => try to get a new DeviceCodeResponse
                 * => note: this causes an exit by thrown an exception if the call fails
                 */
                dcr = getDeviceCodeResponse();
            }

            /*
             * If we got here without exception then the DeviceCodeResponse exists:
             * => try to get the AccessTokenResponse from it
             */
            atr = getAccessTokenResponse(dcr);
            logger.trace("getUserAuthenticationUri() dcr => acr:\n{}\n{}", dcr, atr);

            if (atr == null) {
                /*
                 * The AccessTokenResponse is missing:
                 * => probably the prior DeviceCodeResponse was bad
                 * => make one more try to get a new DeviceCodeResponse
                 * => note: this causes an exit by a thrown an exception if the call fails
                 */
                dcr = getDeviceCodeResponse();
            } else {
                /*
                 * The AccessTokenResponse exists:
                 * => import the AccessTokenResponse into the oAuth service storage
                 * => no further user authentication is needed
                 * => exit by returning null
                 */
                oAuthClientService.importAccessTokenResponse(atr);
                return null;
            }

            /*
             * If we got to this point the DeviceCodeResponse exists:
             * => confirm that it is valid
             */
            logger.trace("getUserAuthenticationUri() check expired:\n{}", dcr);
            if (dcr.isExpired(Instant.now(), 0)) {
                /*
                 * The DeviceCodeResponse is not valid:
                 * => exit by throwing an exception
                 */
                throw new OAuthException("DeviceCodeResponse expired");
            }

            /*
             * If we got to this point the DeviceCodeResponse is OK:
             * => cache the DeviceCodeResponse across polling cycles in 'loopDeviceCodeResponse'
             * => save the DeviceCodeResponse in the oAuth service storage
             * => schedule a repeated task to poll for an AccessTokenResponse from the DeviceCodeResponse
             * => return the user URI from the DeviceCodeResponse
             */
            logger.trace("getUserAuthenticationUri() store and poll start:\n{}", dcr);
            loopDeviceCodeResponse = dcr;
            oAuthStoreHandler.saveDeviceCodeResponse(handle, dcr);
            accessTokenResponsePollSchedule = scheduler.scheduleWithFixedDelay(() -> accessTokenResponsePoll(),
                    dcr.getInterval(), dcr.getInterval(), TimeUnit.SECONDS);
            return dcr.getVerificationUriComplete();
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
        Request request = Objects.requireNonNull(httpClient).newRequest(deviceCodeRequestUrl);
        request.method(HttpMethod.POST);
        request.timeout(HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        request.param(PARAM_CLIENT_ID, clientIdParameter);
        request.param(PARAM_ID_SCOPE, scopeParameter);
        logger.trace("getDeviceCodeResponse() request: {}", request);

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("getDeviceCodeResponse() response:\n{}", content);

            if (response.getStatus() == HttpStatus.OK_200) {
                DeviceCodeResponse dcr = gson.fromJson(content, DeviceCodeResponse.class);
                if (dcr != null) {
                    logger.trace("getDeviceCodeResponse() return:\n{}", dcr);
                    return dcr;
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
        Request request = Objects.requireNonNull(httpClient).newRequest(accessTokenRequestUrl);
        request.method(HttpMethod.POST);
        request.timeout(HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        request.param(PARAM_CLIENT_ID, clientIdParameter);
        request.param(PARAM_GRANT_TYPE, PARAM_GRANT_TYPE_VALUE);
        request.param(PARAM_DEVICE_CODE, dcr.getDeviceCode());
        logger.trace("getAccessTokenResponse() request: {}", request);

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("getAccessTokenResponse() response:\n{}", content);

            if (response.getStatus() == HttpStatus.OK_200) {
                AccessTokenResponse atr = gson.fromJson(content, AccessTokenResponse.class);
                if (atr != null) {
                    logger.trace("getAccessTokenResponse() return:\n{}", atr);
                    return atr;
                }
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

    @Override
    public void close() {
        stopAccessTokenResponsePollSchedule();
        shutdownQuietly(httpClient);
        httpClient = null;
    }

    /**
     * This method is called repeatedly on the scheduler, until the 'stopPolling' loop control
     * variable becomes true, in order to poll for an {@link AccessTokenResponse}
     */
    private synchronized void accessTokenResponsePoll() {
        DeviceCodeResponse dcr = loopDeviceCodeResponse;
        logger.trace("accessTokenResponsePoll() start:\n{}", dcr);

        Instant now = Instant.now();
        boolean stopPolling = false;
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
                logger.debug("accessTokenResponsePoll() error: {}", e.getMessage(), e);
                stopPolling = true;
            }
        }

        logger.trace("accessTokenResponsePoll() continue:\n{}\n{}", dcr, atr);
        if (atr != null) {
            /*
             * The AccessTokenResponse is OK:
             * => import the AccessTokenResponse into the oAuth service
             * => stop polling
             */
            try {
                oAuthClientService.importAccessTokenResponse(atr);
            } catch (OAuthException e) {
                logger.debug("accessTokenResponsePoll() error: {}", e.getMessage(), e);
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
             * => close the HTTP client
             */
            close();
        } else {
            /*
             * Cache the DeviceCodeResponse for the next iteration
             */
            loopDeviceCodeResponse = dcr;
        }

        logger.trace("accessTokenResponsePoll() done:\n{}\n{}\nstopPolling [{}]", dcr, atr, stopPolling);
    }
}
