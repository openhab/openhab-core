/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Base class for tests for the <code>HttpRequestBuilder</code> and <code>HttpUtil</code> to validate their behavior
 *
 * @author Martin van Wingerden & Wouter Born - Initial contribution
 * @author Markus Rathgeb - Base test classes without tests needs to be abstract
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public abstract class BaseHttpUtilTest {

    static final String URL = "http://example.org/test";

    protected @Mock @NonNullByDefault({}) HttpClientFactory clientFactoryMock;
    protected @Mock @NonNullByDefault({}) HttpClient httpClientMock;
    protected @Mock @NonNullByDefault({}) Request requestMock;
    protected @Mock @NonNullByDefault({}) ContentResponse contentResponseMock;

    @BeforeEach
    public void setUp() throws Exception {
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
