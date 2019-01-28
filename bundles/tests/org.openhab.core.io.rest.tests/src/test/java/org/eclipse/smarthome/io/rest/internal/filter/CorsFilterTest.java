/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.rest.internal.filter;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.eclipse.smarthome.io.rest.internal.filter.CorsFilter.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Test for the {@link CorsFilter} filter
 *
 * @author Antoine Besnard - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java and use Mockito
 */
public class CorsFilterTest {

    private static final String CONTENT_TYPE_HEADER = HttpHeaders.CONTENT_TYPE;

    private static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    private static final String ECLIPSE_ORIGIN = "http://eclipse.org";
    private static final String VARY_HEADER_VALUE = "Content-Type";
    private static final String REQUEST_HEADERS = "X-Custom, X-Mine";

    private CorsFilter filter;
    private MultivaluedMap<String, String> responseHeaders = new MultivaluedHashMap<>();

    private @Mock ContainerRequestContext requestContext;
    private @Mock ContainerResponseContext responseContext;

    public @Rule MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() {
        filter = new CorsFilter();
        filter.activate(singletonMap("enable", "true"));
    }

    @Test
    public void notCorsOptionsRequestTest() throws IOException {
        setupRequestContext(HTTP_OPTIONS_METHOD, null, null, null);
        setupResponseContext(null);

        filter.filter(requestContext, responseContext);

        // Not a CORS request, thus no CORS headers should be added.
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_HEADERS);
        assertResponseWithoutHeader(VARY_HEADER);
    }

    @Test
    public void notCorsRealRequestTest() throws IOException {
        setupRequestContext(HTTP_GET_METHOD, null, null, null);
        setupResponseContext(null);

        filter.filter(requestContext, responseContext);

        // Not a CORS request, thus no CORS headers should be added.
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_HEADERS);
        assertResponseWithoutHeader(VARY_HEADER);
    }

    @Test
    public void corsPreflightRequestTest() throws IOException {
        setupRequestContext(HTTP_OPTIONS_METHOD, ECLIPSE_ORIGIN, HTTP_GET_METHOD, REQUEST_HEADERS);
        setupResponseContext(VARY_HEADER_VALUE);

        filter.filter(requestContext, responseContext);

        assertResponseHasHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER, ACCEPTED_HTTP_METHODS);
        assertResponseHasHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, ECLIPSE_ORIGIN);
        assertResponseHasHeader(ACCESS_CONTROL_ALLOW_HEADERS, CONTENT_TYPE_HEADER);
        assertResponseHasHeader(VARY_HEADER, VARY_HEADER_VALUE + "," + ORIGIN_HEADER);
    }

    @Test
    public void partialCorsPreflightRequestTest() throws IOException {
        setupRequestContext(HTTP_OPTIONS_METHOD, ECLIPSE_ORIGIN, null, REQUEST_HEADERS);
        setupResponseContext(VARY_HEADER_VALUE);

        filter.filter(requestContext, responseContext);

        // Since the requestMethod header is not present in the request, it is not a valid Preflight CORS request.
        // Thus, no CORS header should be added to the response.
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_HEADERS);
        assertResponseHasHeader(VARY_HEADER, VARY_HEADER_VALUE);
    }

    @Test
    public void corsPreflightRequestWithoutRequestHeadersTest() throws IOException {
        setupRequestContext(HTTP_OPTIONS_METHOD, ECLIPSE_ORIGIN, HTTP_GET_METHOD, null);
        setupResponseContext(VARY_HEADER_VALUE);

        filter.filter(requestContext, responseContext);

        // Since the requestMethod header is not present in the request, it is not a valid Preflight CORS request.
        // Thus, no CORS header should be added to the response.
        assertResponseHasHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER, ACCEPTED_HTTP_METHODS);
        assertResponseHasHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, ECLIPSE_ORIGIN);
        assertResponseHasHeader(ACCESS_CONTROL_ALLOW_HEADERS, CONTENT_TYPE_HEADER);
        assertResponseHasHeader(VARY_HEADER, VARY_HEADER_VALUE + "," + ORIGIN_HEADER);
    }

    @Test
    public void corsRealRequestTest() throws IOException {
        setupRequestContext(HTTP_GET_METHOD, ECLIPSE_ORIGIN, null, null);
        setupResponseContext(null);

        filter.filter(requestContext, responseContext);

        // Not a CORS request, thus no CORS headers should be added.
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        assertResponseHasHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, ECLIPSE_ORIGIN);
        assertResponseWithoutHeader(ACCESS_CONTROL_ALLOW_HEADERS);
        assertResponseWithoutHeader(VARY_HEADER);
    }

    private void assertResponseWithoutHeader(String header) {
        assertFalse(responseHeaders.containsKey(header));
    }

    private void assertResponseHasHeader(String header, String value) {
        assertTrue(responseHeaders.containsKey(header));
        assertTrue(responseHeaders.getFirst(header).equals(value));
    }

    private void setupRequestContext(String methodValue, String originValue, final String requestMethodValue,
            String requestHeadersValue) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (originValue != null) {
            headers.put(ORIGIN_HEADER, singletonList(originValue));
        }
        if (requestMethodValue != null) {
            headers.put(ACCESS_CONTROL_REQUEST_METHOD, singletonList(requestMethodValue));
        }
        if (requestHeadersValue != null) {
            headers.put(ACCESS_CONTROL_REQUEST_HEADERS, singletonList(requestHeadersValue));
        }

        when(requestContext.getHeaders()).thenReturn(headers);
        when(requestContext.getMethod()).thenReturn(methodValue);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setupResponseContext(String varyHeaderValue) {
        if (varyHeaderValue != null) {
            responseHeaders.put(VARY_HEADER, Stream.of(varyHeaderValue).collect(toList()));
        }

        when(responseContext.getHeaders()).thenReturn((MultivaluedHashMap) responseHeaders);
        when(responseContext.getStringHeaders()).thenReturn(responseHeaders);
    }
}
