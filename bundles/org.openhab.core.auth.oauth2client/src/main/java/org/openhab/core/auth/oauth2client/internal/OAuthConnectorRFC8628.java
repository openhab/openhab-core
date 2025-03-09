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

    private static final String OAUTH_RFC8628 = "oauth-rfc8628";

    // URL parameter names
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_DEVICE_CODE = "device_code";
    private static final String PARAM_GRANT_TYPE = "grant_type";

    // URL parameter values
    private static final String PARAM_GRANT_TYPE_VALUE = "urn:ietf:params:oauth:grant-type:device_code";

    // logger strings
    private static final String LOG_NULL_ATR = "AccessTokenResponse [null]";
    private static final Object LOG_NULL_DCR = "DeviceCodeResponse [null]";

    private static final int HTTP_TIMEOUT_MILLISECONDS = 5000;

    private final Logger logger = LoggerFactory.getLogger(OAuthConnectorRFC8628.class);

    private final OAuthClientService oAuthClientService;
    private final OAuthStoreHandler oAuthStoreHandler;
    private final ScheduledExecutorService scheduler;
    private final String handle;

    private final String accessTokenRequestUrl;
    private final String deviceCodeRequestUrl;
    private final String clientIdParameter;
    private final String scopeParameter;

    private @Nullable ScheduledFuture<?> atrPollTaskSchedule;
    private @Nullable DeviceCodeResponse dcrCached;
    private @Nullable HttpClient httpClient;

    /**
     * Create an extension of the {link OAuthConnector} that implements the oAuth RFC-8628 Device Code
     * Grant Flow authentication process. The parameters are as follows -- whereby (*) means that the
     * parameters would usually be from the private fields of the calling {@link OAuthClientService}.
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
        this.scheduler = ThreadPoolManager.getScheduledPool(OAUTH_RFC8628);
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
        /*
         * 'finally' control variable to create a poll task schedule with the given interval
         */
        long createNewAtrPollTaskScheduleSeconds = 0;
        Instant now = Instant.now();

        try {
            logger.trace("getUserAuthenticationUri() start..");
            if (oAuthClientService.isClosed()) {
                /*
                 * The service is closed:
                 * => throw an exception
                 */
                throw new OAuthException("OAuthClientService closed");
            }

            /*
             * Retrieve local AccessTokenResponse from service (if any)
             */
            AccessTokenResponse atr = oAuthClientService.getAccessTokenResponse();
            logger.trace("getUserAuthenticationUri() loaded from service: {}", atr != null ? atr : LOG_NULL_ATR);

            if (atr != null) {
                /*
                 * The local AccessTokenResponse exists:
                 * => no further user authentication is needed
                 * => return null
                 */
                return null;
            }

            /*
             * Load local DeviceCodeResponse from service storage (if any)
             */
            DeviceCodeResponse dcr = oAuthStoreHandler.loadDeviceCodeResponse(handle);
            logger.trace("getUserAuthenticationUri() loaded from storage: {}", dcr != null ? dcr : LOG_NULL_DCR);

            if (dcr == null || dcr.isExpired(now, 0)) {
                /*
                 * The local DeviceCodeResponse is missing or expired:
                 * => fetch a DeviceCodeResponse from the remote server
                 * => note: if the call fails it throws an exception
                 */
                dcr = getDeviceCodeResponse();
                logger.trace("getUserAuthenticationUri() fetched from remote: {}", dcr);
            }

            /*
             * If we got here without exception then the (local or remote) DeviceCodeResponse exists:
             * => try to fetch an AccessTokenResponse for it from the remote server
             * => if the fetch fails-soft (authentication not yet done) it returns a null AccessTokenResponse
             * => if the fetch fails-hard it throws an exception
             */
            atr = getAccessTokenResponse(dcr);
            logger.trace("getUserAuthenticationUri() fetched from remote: {}", atr != null ? atr : LOG_NULL_ATR);

            if (atr == null) {
                /*
                 * The fetched AccessTokenResponse is missing:
                 * => perhaps the DeviceCodeResponse was bad
                 * => make one more try to fetch a new DeviceCodeResponse
                 * => note: if the fetch fails it throws an exception
                 */
                dcr = getDeviceCodeResponse();
                logger.trace("getUserAuthenticationUri() fetched from remote (retry): {}", dcr);
            } else {
                /*
                 * The fetched AccessTokenResponse exists:
                 * => import the AccessTokenResponse into the service storage
                 * => no further user authentication is needed
                 * => exit by returning null
                 */
                oAuthClientService.importAccessTokenResponse(atr);
                logger.trace("getUserAuthenticationUri() imported into service: {}", atr);
                return null;
            }

            /*
             * If we got to this point the (local or remote) DeviceCodeResponse exists:
             * => confirm that it is valid
             */
            if (dcr.isExpired(now, 0)) {
                /*
                 * The (local or remote) DeviceCodeResponse is expired:
                 * => exit by throwing an exception
                 */
                throw new OAuthException("DeviceCodeResponse expired");
            }

            if (dcr.getInterval() <= 0) {
                /*
                 * The (local or remote) DeviceCodeResponse interval is not valid:
                 * => exit by throwing an exception
                 */
                throw new OAuthException("DeviceCodeResponse interval invalid");
            }

            /*
             * If we got to this point the (local or remote) DeviceCodeResponse is OK:
             * => cache the DeviceCodeResponse across polling cycles
             * => save the DeviceCodeResponse in the service storage
             * => schedule a new AccessTokenResponse poll task
             * => return the user URI from the DeviceCodeResponse
             */
            logger.trace("getUserAuthenticationUri() service save, cache, schedule poll, return URI of: {}", dcr);
            createNewAtrPollTaskScheduleSeconds = dcr.getInterval();
            oAuthStoreHandler.saveDeviceCodeResponse(handle, dcr);
            dcrCached = dcr;
            return dcr.getVerificationUriComplete();
        } catch (GeneralSecurityException e) {
            logger.debug("getUserAuthenticationUri() error: {}", e.getMessage());
            createNewAtrPollTaskScheduleSeconds = 0;
            throw new OAuthException(e);
        } catch (OAuthException | IOException | OAuthResponseException e) {
            logger.debug("getUserAuthenticationUri() error: {}", e.getMessage());
            createNewAtrPollTaskScheduleSeconds = 0;
            throw e;
        } finally {
            if (createNewAtrPollTaskScheduleSeconds > 0) {
                cancelAtrPollTaskSchedule();
                createAtrPollTaskSchedule(createNewAtrPollTaskScheduleSeconds);
            } else {
                close();
            }
        }
    }

    /**
     * Start the first steps of the Device Code Grant Flow authentication process as follows:
     *
     * <ul>
     * <li>Step 1: create a request and POST it to the authentication (device code) url</li>
     * <li>Step 2: process the response and create a {@link DeviceCodeResponse}</li>
     * </ul>
     *
     * @return a new {@link DeviceCodeResponse} object (non null)
     *
     * @throws OAuthException
     */
    private synchronized DeviceCodeResponse getDeviceCodeResponse() throws OAuthException {
        Request request = createHttpClient().newRequest(deviceCodeRequestUrl);
        request.method(HttpMethod.POST);
        request.timeout(HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        request.param(PARAM_CLIENT_ID, clientIdParameter);
        request.param(PARAM_SCOPE, scopeParameter);
        logger.trace("getDeviceCodeResponse() request: {}", request.getURI());

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("getDeviceCodeResponse() response: {}", content);

            if (response.getStatus() == HttpStatus.OK_200) {
                DeviceCodeResponse dcr = gson.fromJson(content, DeviceCodeResponse.class);
                if (dcr != null) {
                    dcr.setCreatedOn(Instant.now());
                    logger.trace("getDeviceCodeResponse() return: {}", dcr);
                    return dcr;
                }
            }
            throw new OAuthException("getDeviceCodeResponse() error: " + response);
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
     * <li>Step 4: repeatedly create a request and POST it to the access token url</li>
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
        Request request = createHttpClient().newRequest(accessTokenRequestUrl);
        request.method(HttpMethod.POST);
        request.timeout(HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        request.param(PARAM_CLIENT_ID, clientIdParameter);
        request.param(PARAM_GRANT_TYPE, PARAM_GRANT_TYPE_VALUE);
        request.param(PARAM_DEVICE_CODE, dcr.getDeviceCode());
        logger.trace("getAccessTokenResponse() request: {}", request.getURI());

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("getAccessTokenResponse() response: {}", content);

            if (response.getStatus() == HttpStatus.OK_200) {
                AccessTokenResponse atr = gson.fromJson(content, AccessTokenResponse.class);
                if (atr != null) {
                    atr.setCreatedOn(Instant.now());
                    logger.trace("getAccessTokenResponse() return: {}", atr);
                    return atr;
                }
            }

            /*
             * Return null without throwing an exception since other HTTP responses
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
        dcrCached = null;
        cancelAtrPollTaskSchedule();
        closeHttpClient();
    }

    private synchronized void cancelAtrPollTaskSchedule() {
        ScheduledFuture<?> future = atrPollTaskSchedule;
        if (future != null) {
            future.cancel(false);
            logger.trace("cancelAtrPollTaskSchedule() cancelled schedule of poll tasks");
        }
        atrPollTaskSchedule = null;
    }

    private void closeHttpClient() {
        shutdownQuietly(httpClient);
        httpClient = null;
    }

    private void createAtrPollTaskSchedule(long seconds) {
        atrPollTaskSchedule = scheduler.scheduleWithFixedDelay(() -> atrPollTask(), seconds, seconds, TimeUnit.SECONDS);
        logger.trace("createAtrPollTaskSchedule() created schedule of poll tasks every {}s", seconds);
    }

    private HttpClient createHttpClient() throws OAuthException {
        HttpClient httpClient = this.httpClient;
        if (httpClient == null) {
            httpClient = createHttpClient(OAUTH_RFC8628);
            this.httpClient = httpClient;
        }
        return httpClient;
    }

    /**
     * This method is called repeatedly on the scheduler to poll for an {@link AccessTokenResponse}.
     * It cancels its own scheduler when either a) an AccessTokenResponse is returned, or b) the
     * cached {@link DeviceCodeResponse} expires.
     */
    private synchronized void atrPollTask() {
        /*
         * 'finally' control variable to cancel the poll task schedule and close the http client
         */
        boolean close = false;
        try {
            DeviceCodeResponse dcr = dcrCached;
            logger.trace("atrPollTask() started with cached: {}", dcr);

            if (dcr != null && !dcr.isExpired(Instant.now(), 0)) {
                /*
                 * The cached DeviceCodeResponse is still valid:
                 * => get an AccessTokenResponse from it
                 * => in case of error, cancel the polling schedule
                 */
                try {
                    AccessTokenResponse atr = getAccessTokenResponse(dcr);
                    logger.trace("atrPollTask() fetched from remote: {}", atr != null ? atr : LOG_NULL_ATR);

                    if (atr != null) {
                        /*
                         * The AccessTokenResponse is OK:
                         * => import the AccessTokenResponse into the service
                         * => cancel the polling schedule
                         */
                        oAuthClientService.importAccessTokenResponse(atr);
                        logger.debug("atrPollTask() imported into service: {}", atr);
                        close = true;
                    }
                } catch (OAuthException e) {
                    logger.debug("atrPollTask() error: {}", e.getMessage());
                    close = true;
                }
            } else {
                /*
                 * The cached DeviceCodeResponse is missing or has expired:
                 * => cancel the polling schedule
                 */
                close = true;
            }
        } finally {
            if (close) {
                close();
            }
            logger.trace("atrPollTask() done");
        }
    }
}
