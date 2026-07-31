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
package org.openhab.core.io.net.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Test cases for the <code>HttpRequestBuilder</code> to validate its behaviour
 *
 * @author Martin van Wingerden and Wouter Born - Initial contribution
 */
@NonNullByDefault
public class HttpRequestBuilderTest extends BaseHttpUtilTest {

    @Test
    public void baseTest() throws Exception {
        mockResponse(HttpStatus.OK_200);

        String result = HttpRequestBuilder.getFrom(URL).getContentAsString();

        assertEquals("Some content", result);

        verify(httpClientMock).newRequest(URI.create(URL));
        verify(requestMock).method(HttpMethod.GET);
        verify(requestMock).send();
    }

    @Test
    public void testHeader() throws Exception {
        mockResponse(HttpStatus.OK_200);

        // @formatter:off
        String result = HttpRequestBuilder.getFrom(URL)
                .withHeader("Authorization", "Bearer sometoken")
                .withHeader("X-Token", "test")
                .getContentAsString();
        // @formatter:on

        assertEquals("Some content", result);

        // verify the headers to be added to the request
        verify(requestMock, times(2)).headers(any());
    }

    @Test
    public void testTimeout() throws Exception {
        mockResponse(HttpStatus.OK_200);

        String result = HttpRequestBuilder.getFrom(URL).withTimeout(Duration.ofMillis(200)).getContentAsString();

        assertEquals("Some content", result);

        // verify the timeout to be forwarded
        verify(requestMock).timeout(200, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testPostWithContent() throws Exception {
        ArgumentCaptor<Request.Content> argumentCaptor = ArgumentCaptor.forClass(Request.Content.class);

        mockResponse(HttpStatus.OK_200);

        String result = HttpRequestBuilder.postTo(URL).withContent("{json: true}").getContentAsString();

        assertEquals("Some content", result);

        // verify the content to be added to the request
        verify(requestMock).body(argumentCaptor.capture());

        assertEquals("application/octet-stream", argumentCaptor.getValue().getContentType());
    }

    @Test
    public void testPostWithContentType() throws Exception {
        ArgumentCaptor<Request.Content> argumentCaptor = ArgumentCaptor.forClass(Request.Content.class);

        mockResponse(HttpStatus.OK_200);

        String result = HttpRequestBuilder.postTo(URL).withContent("{json: true}", "application/json")
                .getContentAsString();

        assertEquals("Some content", result);

        // verify just the content-type to be added to the request
        verify(requestMock).method(HttpMethod.POST);
        verify(requestMock).body(argumentCaptor.capture());
        assertEquals("application/json", argumentCaptor.getValue().getContentType());
    }
}
