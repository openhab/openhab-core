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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.DeviceCodeResponseDTO;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.library.types.DateTimeType;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

/**
 * JUnit tests for {@link OAuthConnectorRFC8628}
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
class OAuthRFC8628ClientTest {

    private final Gson gson = new GsonBuilder().setDateFormat(DateTimeType.DATE_PATTERN_JSON_COMPAT)
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (date, type,
                    jsonSerializationContext) -> new JsonPrimitive(date.toString()))
            .create();

    /**
     * Private wrapper class for test purposes
     */
    private static class OAuthConnectorRFC8628Ext extends OAuthConnectorRFC8628 {

        public OAuthConnectorRFC8628Ext(OAuthClientService oAuthClientService, String handle,
                OAuthStoreHandler oAuthStoreHandler, HttpClientFactory httpClientFactory,
                @Nullable GsonBuilder gsonBuilder, String accessTokenRequestUrl, String deviceCodeRequestUrl,
                String clientId, String scope) throws OAuthException {
            super(oAuthClientService, handle, oAuthStoreHandler, httpClientFactory, gsonBuilder, accessTokenRequestUrl,
                    deviceCodeRequestUrl, clientId, scope);
        }

        @Override
        public void close() {
            // this suppresses the life-cycle errors that otherwise appear in the test log
        }
    }

    @Test
    void testDeviceCodeResponseGoodWhenLoadedFromRemote() {
        OAuthClientService oAuthClientService = mock(OAuthClientService.class);
        OAuthStoreHandler oAuthStoreHandler = mock(OAuthStoreHandler.class);
        HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);

        try (OAuthConnectorRFC8628 oAuthConnectorRFC8628 = new OAuthConnectorRFC8628Ext(oAuthClientService, "handle",
                oAuthStoreHandler, httpClientFactory, null, "accessTokenRequestUrl", "deviceCodeRequestUrl", "clientId",
                "scope")) {
            assertNotNull(oAuthConnectorRFC8628);

            Request request = mock(Request.class);
            HttpClient httpClient = mock(HttpClient.class);
            ContentResponse contentResponse = Mockito.mock(ContentResponse.class);

            when(httpClientFactory.createHttpClient(anyString())).thenReturn(httpClient);
            when(httpClient.isStarted()).thenReturn(true);

            when(httpClient.newRequest(anyString())).thenReturn(request);

            try {
                when(request.send()).thenReturn(contentResponse);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                fail(e);
            }

            DeviceCodeResponseDTO dcr = new DeviceCodeResponseDTO();
            dcr.setDeviceCode("DeviceCode");
            dcr.setExpiresIn(123);
            dcr.setInterval(4L);
            dcr.setUserCode("UserCode");
            dcr.setVerificationUri("VerificationUri");
            dcr.setVerificationUriComplete("VerificationUriComplete");

            when(contentResponse.getStatus()).thenReturn(200);
            when(contentResponse.getContentAsString()).thenReturn(gson.toJson(dcr), "");

            DeviceCodeResponseDTO dcrOut = null;
            try {
                dcrOut = oAuthConnectorRFC8628.getDeviceCodeResponse();
            } catch (OAuthException e) {
                fail(e);
            }
            assertNotNull(dcrOut);
            dcr.setCreatedOn(dcrOut.getCreatedOn()); // allow for test running time;

            assertEquals(dcr, dcrOut);

            verify(oAuthClientService, times(1)).isClosed();

            try {
                verify(oAuthClientService, times(1)).getAccessTokenResponse();
            } catch (OAuthException | IOException | OAuthResponseException e) {
                fail(e);
            }

            try {
                verify(oAuthStoreHandler, times(1)).loadDeviceCodeResponse(anyString());
            } catch (GeneralSecurityException e) {
                fail(e);
            }

            try {
                verify(oAuthClientService, times(0)).importAccessTokenResponse(any(AccessTokenResponse.class));
                verify(oAuthClientService, times(0)).notifyAccessTokenResponse(any(AccessTokenResponse.class));
            } catch (OAuthException e) {
                fail(e);
            }
        } catch (OAuthException e) {
            fail(e);
        }
    }

    @Test
    void testDeviceCodeResponseGoodWhenLoadedFromStorage() {
        OAuthClientService oAuthClientService = mock(OAuthClientService.class);
        OAuthStoreHandler oAuthStoreHandler = mock(OAuthStoreHandler.class);
        HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);

        try (OAuthConnectorRFC8628 oAuthConnectorRFC8628 = new OAuthConnectorRFC8628Ext(oAuthClientService, "handle",
                oAuthStoreHandler, httpClientFactory, null, "accessTokenRequestUrl", "deviceCodeRequestUrl", "clientId",
                "scope")) {
            assertNotNull(oAuthConnectorRFC8628);

            Request request = mock(Request.class);
            HttpClient httpClient = mock(HttpClient.class);
            ContentResponse contentResponse = Mockito.mock(ContentResponse.class);

            when(httpClientFactory.createHttpClient(anyString())).thenReturn(httpClient);
            when(httpClient.isStarted()).thenReturn(true);

            when(httpClient.newRequest(anyString())).thenReturn(request);

            try {
                when(request.send()).thenReturn(contentResponse);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                fail(e);
            }

            DeviceCodeResponseDTO dcr = new DeviceCodeResponseDTO();
            dcr.setCreatedOn(Instant.now());
            dcr.setDeviceCode("DeviceCode");
            dcr.setExpiresIn(123);
            dcr.setInterval(4L);
            dcr.setUserCode("UserCode");
            dcr.setVerificationUri("VerificationUri");
            dcr.setVerificationUriComplete("VerificationUriComplete");

            when(contentResponse.getStatus()).thenReturn(200);
            when(contentResponse.getContentAsString()).thenReturn("");

            try {
                when(oAuthStoreHandler.loadDeviceCodeResponse(anyString())).thenReturn(dcr);
            } catch (GeneralSecurityException e) {
                fail(e);
            }

            DeviceCodeResponseDTO dcrOut = null;
            try {
                dcrOut = oAuthConnectorRFC8628.getDeviceCodeResponse();
            } catch (OAuthException e) {
                fail(e);
            }
            assertNotNull(dcrOut);
            dcr.setCreatedOn(dcrOut.getCreatedOn()); // allow for test running time;

            assertEquals(dcr, dcrOut);

            verify(oAuthClientService, times(1)).isClosed();

            try {
                verify(oAuthClientService, times(1)).getAccessTokenResponse();
            } catch (OAuthException | IOException | OAuthResponseException e) {
                fail(e);
            }

            try {
                verify(oAuthStoreHandler, times(1)).loadDeviceCodeResponse(anyString());
            } catch (GeneralSecurityException e) {
                fail(e);
            }

            try {
                verify(oAuthClientService, times(0)).importAccessTokenResponse(any(AccessTokenResponse.class));
                verify(oAuthClientService, times(0)).notifyAccessTokenResponse(any(AccessTokenResponse.class));
            } catch (OAuthException e) {
                fail(e);
            }
        } catch (OAuthException e) {
            fail(e);
        }
    }

    @Test
    void testDeviceCodeResponseNullWhenAccessTokenResponseLoadedFromRemote() {
        OAuthClientService oAuthClientService = mock(OAuthClientService.class);
        OAuthStoreHandler oAuthStoreHandler = mock(OAuthStoreHandler.class);
        HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);

        try (OAuthConnectorRFC8628 oAuthConnectorRFC8628 = new OAuthConnectorRFC8628Ext(oAuthClientService, "handle",
                oAuthStoreHandler, httpClientFactory, null, "accessTokenRequestUrl", "deviceCodeRequestUrl", "clientId",
                "scope")) {
            assertNotNull(oAuthConnectorRFC8628);

            Request request = mock(Request.class);
            HttpClient httpClient = mock(HttpClient.class);
            ContentResponse contentResponse = Mockito.mock(ContentResponse.class);

            when(httpClientFactory.createHttpClient(anyString())).thenReturn(httpClient);
            when(httpClient.isStarted()).thenReturn(true);

            when(httpClient.newRequest(anyString())).thenReturn(request);

            try {
                when(request.send()).thenReturn(contentResponse);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                fail(e);
            }

            DeviceCodeResponseDTO dcr = new DeviceCodeResponseDTO();
            dcr.setDeviceCode("DeviceCode");
            dcr.setExpiresIn(123);
            dcr.setInterval(4L);
            dcr.setUserCode("UserCode");
            dcr.setVerificationUri("VerificationUri");
            dcr.setVerificationUriComplete("VerificationUriComplete");

            AccessTokenResponse atr = new AccessTokenResponse();
            atr.setAccessToken("AccessToken");
            atr.setExpiresIn(123);
            atr.setRefreshToken("RefreshToken");

            when(contentResponse.getStatus()).thenReturn(200);
            when(contentResponse.getContentAsString()).thenReturn(gson.toJson(dcr), gson.toJson(atr));

            DeviceCodeResponseDTO dcrOut = null;
            try {
                dcrOut = oAuthConnectorRFC8628.getDeviceCodeResponse();
            } catch (OAuthException e) {
                fail(e);
            }
            assertNull(dcrOut);

            verify(oAuthClientService, times(1)).isClosed();

            try {
                verify(oAuthClientService, times(1)).getAccessTokenResponse();
            } catch (OAuthException | IOException | OAuthResponseException e) {
                fail(e);
            }

            try {
                verify(oAuthStoreHandler, times(1)).loadDeviceCodeResponse(anyString());
            } catch (GeneralSecurityException e) {
                fail(e);
            }

            try {
                verify(oAuthClientService, times(1)).importAccessTokenResponse(any(AccessTokenResponse.class));
                verify(oAuthClientService, times(1)).notifyAccessTokenResponse(any(AccessTokenResponse.class));
            } catch (OAuthException e) {
                fail(e);
            }
        } catch (OAuthException e) {
            fail(e);
        }
    }

    @Test
    void testDeviceCodeResponseNullWhenAccessTokenResponseLoadedFromStorage() {
        OAuthClientService oAuthClientService = mock(OAuthClientService.class);
        OAuthStoreHandler oAuthStoreHandler = mock(OAuthStoreHandler.class);
        HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);

        try (OAuthConnectorRFC8628 oAuthConnectorRFC8628 = new OAuthConnectorRFC8628Ext(oAuthClientService, "handle",
                oAuthStoreHandler, httpClientFactory, null, "accessTokenRequestUrl", "deviceCodeRequestUrl", "clientId",
                "scope")) {
            assertNotNull(oAuthConnectorRFC8628);

            AccessTokenResponse atr = new AccessTokenResponse();
            atr.setAccessToken("AccessToken");
            atr.setExpiresIn(123);
            atr.setRefreshToken("RefreshToken");

            try {
                when(oAuthClientService.getAccessTokenResponse()).thenReturn(atr);
            } catch (OAuthException | IOException | OAuthResponseException e) {
                fail(e);
            }

            DeviceCodeResponseDTO dcr = null;
            try {
                dcr = oAuthConnectorRFC8628.getDeviceCodeResponse();
            } catch (OAuthException e) {
                fail(e);
            }
            assertNull(dcr);

            verify(oAuthClientService, times(1)).isClosed();

            try {
                verify(oAuthClientService, times(1)).getAccessTokenResponse();
            } catch (OAuthException | IOException | OAuthResponseException e) {
                fail(e);
            }

            try {
                verify(oAuthStoreHandler, times(0)).loadDeviceCodeResponse(anyString());
            } catch (GeneralSecurityException e) {
                fail(e);
            }

            try {
                verify(oAuthClientService, times(0)).importAccessTokenResponse(any(AccessTokenResponse.class));
                verify(oAuthClientService, times(0)).notifyAccessTokenResponse(any(AccessTokenResponse.class));
            } catch (OAuthException e) {
                fail(e);
            }
        } catch (OAuthException e) {
            fail(e);
        }
    }

    @Test
    void testDeviceCodeResponseNullWhenServiceIsClosed() {
        OAuthClientService oAuthClientService = mock(OAuthClientService.class);
        OAuthStoreHandler oAuthStoreHandler = mock(OAuthStoreHandler.class);
        HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);

        when(oAuthClientService.isClosed()).thenReturn(true);

        try (OAuthConnectorRFC8628 oAuthConnectorRFC8628 = new OAuthConnectorRFC8628Ext(oAuthClientService, "handle",
                oAuthStoreHandler, httpClientFactory, null, "accessTokenRequestUrl", "deviceCodeRequestUrl", "clientId",
                "scope")) {
            assertThrows(OAuthException.class, () -> oAuthConnectorRFC8628.getDeviceCodeResponse());
        } catch (OAuthException e) {
            fail(e);
        }
    }
}
