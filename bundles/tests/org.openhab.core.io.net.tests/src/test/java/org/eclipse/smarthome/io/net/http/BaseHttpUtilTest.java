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
package org.eclipse.smarthome.io.net.http;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Before;
import org.mockito.Mock;

/**
 * Base class for tests for the <code>HttpRequestBuilder</code> and <code>HttpUtil</code> to validate their behavior
 *
 * @author Martin van Wingerden & Wouter Born - Initial contribution
 */
public class BaseHttpUtilTest {
    static final String URL = "http://example.org/test";

    @Mock
    private HttpClientFactory clientFactoryMock;

    @Mock
    HttpClient httpClientMock;

    @Mock
    Request requestMock;

    @Mock
    ContentResponse contentResponseMock;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        Field httpClientFactory = HttpUtil.class.getDeclaredField("httpClientFactory");
        httpClientFactory.setAccessible(true);
        httpClientFactory.set(null, clientFactoryMock);

        when(clientFactoryMock.getCommonHttpClient()).thenReturn(httpClientMock);
        when(httpClientMock.newRequest(URL)).thenReturn(requestMock);
        when(requestMock.method(any(HttpMethod.class))).thenReturn(requestMock);
        when(requestMock.timeout(anyLong(), any(TimeUnit.class))).thenReturn(requestMock);
        when(requestMock.send()).thenReturn(contentResponseMock);
    }

    void mockResponse(int httpStatus) {
        when(contentResponseMock.getStatus()).thenReturn(httpStatus);
        when(contentResponseMock.getContent()).thenReturn("Some content".getBytes());
    }
}
