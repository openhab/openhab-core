/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.core.io.net.http.HttpClientFactory;

/**
 * JUnit tests for {@link OAuthConnector}
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
class OAuthConnectorTest {

    private static void setupHttpMocks(HttpClientFactory httpClientFactory, HttpClient httpClient, Request request,
            ContentResponse contentResponse, int statusCode, String responseBody)
            throws InterruptedException, TimeoutException, ExecutionException {
        when(httpClientFactory.createHttpClient(anyString())).thenReturn(httpClient);
        when(httpClient.isStarted()).thenReturn(true);
        when(httpClient.newRequest(anyString())).thenReturn(request);
        when(request.send()).thenReturn(contentResponse);
        when(contentResponse.getStatus()).thenReturn(statusCode);
        when(contentResponse.getContentAsString()).thenReturn(responseBody);
    }

    /**
     * RFC 6749 section 5.2: HTTP 401 with no JSON body must produce an
     * {@link OAuthResponseException} with {@code error = "invalid_client"}.
     */
    @Test
    void testHttp401WithEmptyBodyThrowsInvalidClientException() {
        HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
        HttpClient httpClient = mock(HttpClient.class);
        // RETURNS_SELF handles the Request builder-pattern chain (method/timeout/header/content)
        Request request = mock(Request.class, Mockito.RETURNS_SELF);
        ContentResponse contentResponse = mock(ContentResponse.class);

        try {
            setupHttpMocks(httpClientFactory, httpClient, request, contentResponse, HttpStatus.UNAUTHORIZED_401, "");
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            fail(e);
        }

        OAuthConnector connector = new OAuthConnector(httpClientFactory);

        OAuthResponseException exception = assertThrows(OAuthResponseException.class, () -> connector
                .grantTypeClientCredentials("http://token.example.com", "clientId", "clientSecret", null, false));

        assertEquals("invalid_client", exception.getError());
    }

    /**
     * RFC 6749 section 5.2: HTTP 401 with a JSON error body must preserve the
     * server-supplied {@code error} and {@code error_description} fields.
     */
    @Test
    void testHttp401WithJsonBodyThrowsExceptionWithServerError() {
        HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
        HttpClient httpClient = mock(HttpClient.class);
        Request request = mock(Request.class, Mockito.RETURNS_SELF);
        ContentResponse contentResponse = mock(ContentResponse.class);

        String jsonBody = "{\"error\":\"unauthorized_client\","
                + "\"error_description\":\"The client is not authorized to use this grant type\"}";

        try {
            setupHttpMocks(httpClientFactory, httpClient, request, contentResponse, HttpStatus.UNAUTHORIZED_401,
                    jsonBody);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            fail(e);
        }

        OAuthConnector connector = new OAuthConnector(httpClientFactory);

        OAuthResponseException exception = assertThrows(OAuthResponseException.class, () -> connector
                .grantTypeClientCredentials("http://token.example.com", "clientId", "clientSecret", null, false));

        assertEquals("unauthorized_client", exception.getError());
        assertEquals("The client is not authorized to use this grant type", exception.getErrorDescription());
    }

    /**
     * RFC 6749 section 5.2: HTTP 401 with a non-JSON body (e.g. "Unauthorized")
     * must be treated like an empty body and produce an {@link OAuthResponseException}
     * with {@code error = "invalid_client"} instead of failing JSON parsing.
     */
    @Test
    void testHttp401WithNonJsonBodyThrowsInvalidClientException() {
        HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
        HttpClient httpClient = mock(HttpClient.class);
        Request request = mock(Request.class, Mockito.RETURNS_SELF);
        ContentResponse contentResponse = mock(ContentResponse.class);

        try {
            setupHttpMocks(httpClientFactory, httpClient, request, contentResponse, HttpStatus.UNAUTHORIZED_401,
                    "Unauthorized");
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            fail(e);
        }

        OAuthConnector connector = new OAuthConnector(httpClientFactory);

        OAuthResponseException exception = assertThrows(OAuthResponseException.class, () -> connector
                .grantTypeClientCredentials("http://token.example.com", "clientId", "clientSecret", null, false));

        assertEquals("invalid_client", exception.getError());
    }
}
