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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.DeviceCodeResponseDTO;
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

    // HTTP 400 error messages for RFC-8628
    private static final String SLOW_DOWN = "slow_down";
    private static final String ACCESS_DENIED = "access_denied";
    private static final String EXPIRED_TOKEN = "expired_token";

    // URL parameter values
    private static final String PARAM_GRANT_TYPE_VALUE = "urn:ietf:params:oauth:grant-type:device_code";

    // logger string
    private static final String LOG_NULL_ATR = "AccessTokenResponse [null]";

    private static final int HTTP_TIMEOUT_MILLISECONDS = 5000;
    private static final long DEFAULT_POLL_INTERVAL = 5;

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
    private @Nullable DeviceCodeResponseDTO dcrCached;
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
     * @param deviceCodeRequestUrl the URL that provides {@link DeviceCodeResponseDTO} responses
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
     * <li>Step 2: process the response and create a {@link DeviceCodeResponseDTO}</li>
     * <li>Step 3: the user goes off to authenticate themselves at the 'user authentication url'</li>
     * <li>Step 4: repeatedly create a request and POST it to the 'token url'</li>
     * <li>Step 5: repeatedly read the response and eventually create a {@link AccessTokenResponse}</li>
     * </ul>
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc8628">RFC-8628</a>
     *
     * @return either null or a {@link DeviceCodeResponseDTO} containing the verification uri's where
     *         users are expected authenticate themselves
     * @throws OAuthException
     */
    public synchronized @Nullable DeviceCodeResponseDTO getDeviceCodeResponse() throws OAuthException {
        /*
         * 'finally' control variable to create a poll task schedule with the given interval
         */
        long createNewAtrPollTaskScheduleSeconds = 0;
        try {
            logger.trace("getDeviceCodeResponse() start..");
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
            AccessTokenResponse atr;
            try {
                atr = oAuthClientService.getAccessTokenResponse();
            } catch (IOException | OAuthResponseException e) {
                atr = null;
            }
            logger.trace("getDeviceCodeResponse() loaded from service: {}", atr != null ? atr : LOG_NULL_ATR);

            if (atr != null) {
                /*
                 * The local AccessTokenResponse exists:
                 * => no further user authentication is needed
                 * => return null
                 */
                return null;
            }

            DeviceCodeResponseDTO dcr;
            try {
                /*
                 * Load local DeviceCodeResponse from service storage (if any) and check it is valid
                 */
                dcr = checkDeviceCodeResponse(oAuthStoreHandler.loadDeviceCodeResponse(handle));
                logger.trace("getDeviceCodeResponse() loaded from storage: {}", dcr);
            } catch (GeneralSecurityException e) {
                throw new OAuthException(e);
            } catch (OAuthException e) {
                /*
                 * The DeviceCodeResponse is not valid
                 * => try to fetch a new one from the remote server and check it is valid
                 * => note: a second OAuthException exits from this method with that exception
                 */
                dcr = checkDeviceCodeResponse(fetchDeviceCodeResponse());
                logger.trace("getDeviceCodeResponse() fetched from remote: {}", dcr);
            }

            /*
             * If we got here without exception then the (local or remote) DeviceCodeResponse is all good:
             * => try to fetch an AccessTokenResponse for it from the remote server
             * => if the fetch fails-soft (authentication not yet done) it returns a null AccessTokenResponse
             * => if the fetch fails-hard it throws an exception
             */
            try {
                atr = fetchAccessTokenResponse(dcr);
            } catch (OAuthResponseException e) {
                /*
                 * An AccessTokenResponse was not returned
                 * => so atr remains null; as it was prior to calling fetchAccessTokenResponse()
                 */
            }
            logger.trace("getDeviceCodeResponse() fetched from remote: {}", atr != null ? atr : LOG_NULL_ATR);

            if (atr != null) {
                /*
                 * The fetched AccessTokenResponse exists:
                 * => import the AccessTokenResponse into the service storage
                 * => notify service AccessTokenRefreshListeners
                 * => no further user authentication is needed
                 * => exit by returning null
                 */
                oAuthClientService.importAccessTokenResponse(atr);
                oAuthClientService.notifyAccessTokenResponse(atr);
                logger.trace("getDeviceCodeResponse() imported into service: {}", atr);
                return null;
            }

            /*
             * If we got to this point we have a good DeviceCodeResponse but AccessTokenResponse is null:
             * => cache the DeviceCodeResponse across polling cycles
             * => save the DeviceCodeResponse in the service storage
             * => schedule a new AccessTokenResponse poll task
             * => return the DeviceCodeResponse
             */
            logger.trace("getDeviceCodeResponse() service save, cache, schedule poll, return: {}", dcr);
            createNewAtrPollTaskScheduleSeconds = Objects.requireNonNull(dcr.getInterval());
            oAuthStoreHandler.saveDeviceCodeResponse(handle, dcr);
            dcrCached = dcr;
            return (DeviceCodeResponseDTO) dcr.clone();
        } catch (OAuthException e) {
            logger.debug("getDeviceCodeResponse() error: {}", e.getMessage());
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
     * Check the validity of the given {@link DeviceCodeResponseDTO}
     *
     * @param dcr the incoming {@link DeviceCodeResponseDTO} (may be null)
     * @return a fully valid {@link DeviceCodeResponseDTO} (guaranteed non null)
     *
     * @throws OAuthException
     */
    private DeviceCodeResponseDTO checkDeviceCodeResponse(@Nullable DeviceCodeResponseDTO dcr) throws OAuthException {
        if (dcr == null) {
            throw new OAuthException("DeviceCodeResponse is null");
        }
        if (dcr.isExpired(Instant.now(), 0)) {
            throw new OAuthException("DeviceCodeResponse expired");
        }
        Long interval = dcr.getInterval();
        if (interval == null || interval <= 0) {
            throw new OAuthException("DeviceCodeResponse interval invalid");
        }
        return dcr;
    }

    /**
     * Start the first steps of the Device Code Grant Flow authentication process as follows:
     *
     * <ul>
     * <li>Step 1: create a request and POST it to the authentication (device code) url</li>
     * <li>Step 2: process the response and create a {@link DeviceCodeResponseDTO}</li>
     * </ul>
     *
     * @return a new {@link DeviceCodeResponseDTO} object (non null)
     *
     * @throws OAuthException
     */
    private synchronized DeviceCodeResponseDTO fetchDeviceCodeResponse() throws OAuthException {
        Request request = createHttpClient().newRequest(deviceCodeRequestUrl);
        request.method(HttpMethod.POST);
        request.timeout(HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        request.param(PARAM_CLIENT_ID, clientIdParameter);
        request.param(PARAM_SCOPE, scopeParameter);
        logger.trace("fetchDeviceCodeResponse() request: {}", request.getURI());

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("fetchDeviceCodeResponse() response: {}", content);

            if (response.getStatus() == HttpStatus.OK_200) {
                DeviceCodeResponseDTO dcr = gson.fromJson(content, DeviceCodeResponseDTO.class);
                if (dcr != null) {
                    dcr.setCreatedOn(Instant.now());
                    // in RFC-8628 'interval' is OPTIONAL so if absent use default
                    if (dcr.getInterval() == null) {
                        dcr.setInterval(DEFAULT_POLL_INTERVAL);
                    }
                    logger.trace("fetchDeviceCodeResponse() return: {}", dcr);
                    return dcr;
                }
            }
            throw new OAuthException("fetchDeviceCodeResponse() error: " + response);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new OAuthException("fetchDeviceCodeResponse() error", e);
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
     * @param dcr the {@link DeviceCodeResponseDTO}
     *
     * @return an {@link AccessTokenResponse} object or null
     * @throws OAuthResponseException if the response content is an OAuth JSON error packet
     * @throws OAuthException for any other errors
     */
    private synchronized @Nullable AccessTokenResponse fetchAccessTokenResponse(DeviceCodeResponseDTO dcr)
            throws OAuthException, OAuthResponseException {
        Request request = createHttpClient().newRequest(accessTokenRequestUrl);
        request.method(HttpMethod.POST);
        request.timeout(HTTP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        request.param(PARAM_CLIENT_ID, clientIdParameter);
        request.param(PARAM_GRANT_TYPE, PARAM_GRANT_TYPE_VALUE);
        request.param(PARAM_DEVICE_CODE, dcr.getDeviceCode());
        logger.trace("fetchAccessTokenResponse() request: {}", request.getURI());

        try {
            ContentResponse response = request.send();
            String content = response.getContentAsString();
            logger.trace("fetchAccessTokenResponse() response: {}", content);

            switch (response.getStatus()) {
                case HttpStatus.OK_200:
                    AccessTokenResponse atr = gson.fromJson(content, AccessTokenResponse.class);
                    if (atr != null) {
                        atr.setCreatedOn(Instant.now());
                        logger.trace("fetchAccessTokenResponse() return: {}", atr);
                        return atr;
                    }
                case HttpStatus.BAD_REQUEST_400:
                    OAuthResponseException err = gson.fromJson(content, OAuthResponseException.class);
                    if (err != null) {
                        throw err;
                    }
            }

            /*
             * Return null without throwing an exception since other HTTP responses
             * may occur during AccessTokenResponse polling before the user has
             * completed the verification process
             */
            return null;
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new OAuthException("fetchAccessTokenResponse() error", e);
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
     * cached {@link DeviceCodeResponseDTO} expires.
     */
    private synchronized void atrPollTask() {
        /*
         * 'finally' control variable to cancel the poll task schedule and close the http client
         */
        boolean close = false;
        try {
            DeviceCodeResponseDTO dcr = dcrCached;
            logger.trace("atrPollTask() started with cached: {}", dcr);
            try {
                dcr = checkDeviceCodeResponse(dcr);

                /*
                 * The cached DeviceCodeResponse is still valid:
                 * => get an AccessTokenResponse from it
                 * => in case of error, cancel the polling schedule
                 */
                AccessTokenResponse atr = fetchAccessTokenResponse(dcr);
                logger.trace("atrPollTask() fetched from remote: {}", atr);

                if (atr != null) {
                    /*
                     * The AccessTokenResponse is OK:
                     * => import the AccessTokenResponse into the service
                     * => notify service AccessTokenRefreshListeners
                     * => cancel the polling schedule
                     */
                    oAuthClientService.importAccessTokenResponse(atr);
                    oAuthClientService.notifyAccessTokenResponse(atr);
                    logger.trace("atrPollTask() imported into service: {}", atr);
                    close = true;
                }
            } catch (OAuthResponseException e) {
                String error = e.getError();
                logger.trace("atrPollTask() poll response error: {}", error);
                switch (error) {
                    case ACCESS_DENIED, EXPIRED_TOKEN:
                        close = true;
                        break;

                    case SLOW_DOWN:
                        ScheduledFuture<?> future = atrPollTaskSchedule;
                        if (future != null) {
                            long priorDelay = future.getDelay(TimeUnit.SECONDS);
                            cancelAtrPollTaskSchedule();
                            createAtrPollTaskSchedule(priorDelay + 1);
                        }
                        break;
                }
            } catch (OAuthException e) {
                logger.debug("atrPollTask() error: {}", e.getMessage());
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
