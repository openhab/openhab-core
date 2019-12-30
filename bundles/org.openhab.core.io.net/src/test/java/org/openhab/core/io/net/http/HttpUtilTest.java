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
package org.openhab.core.io.net.http;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for the HttpUtil
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Martin van Wingerden - Added tests based on HttpClientFactory
 */
public class HttpUtilTest extends BaseHttpUtilTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void baseTest() throws Exception {
        mockResponse(HttpStatus.OK_200);

        String result = HttpUtil.executeUrl("GET", URL, 500);

        assertEquals("Some content", result);

        verify(httpClientMock).newRequest(URL);
        verify(requestMock).method(HttpMethod.GET);
        verify(requestMock).timeout(500, TimeUnit.MILLISECONDS);
        verify(requestMock).send();
    }

    @Test
    public void testAuthentication() throws Exception {
        when(httpClientMock.newRequest("http://john:doe@example.org/")).thenReturn(requestMock);
        mockResponse(HttpStatus.OK_200);

        String result = HttpUtil.executeUrl("GET", "http://john:doe@example.org/", 500);

        assertEquals("Some content", result);

        verify(requestMock).header(HttpHeader.AUTHORIZATION, "Basic am9objpkb2U=");
    }

    @Test
    public void testCreateHttpMethod() {
        assertEquals(HttpMethod.GET, HttpUtil.createHttpMethod("GET"));
        assertEquals(HttpMethod.PUT, HttpUtil.createHttpMethod("PUT"));
        assertEquals(HttpMethod.POST, HttpUtil.createHttpMethod("POST"));
        assertEquals(HttpMethod.DELETE, HttpUtil.createHttpMethod("DELETE"));
    }

    @Test
    public void testCreateHttpMethodForUnsupportedFake() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Given HTTP Method 'FAKE' is unknown");

        HttpUtil.createHttpMethod("FAKE");
    }

    @Test
    public void testCreateHttpMethodForUnsupportedTrace() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Given HTTP Method 'TRACE' is unknown");

        HttpUtil.createHttpMethod("TRACE");
    }
}
