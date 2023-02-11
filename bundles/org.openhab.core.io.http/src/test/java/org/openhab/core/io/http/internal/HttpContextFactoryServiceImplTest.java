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
package org.openhab.core.io.http.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.io.http.WrappingHttpContext;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 *
 * @author Łukasz Dywicki - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class HttpContextFactoryServiceImplTest {

    private static final String RESOURCE = "resource";

    private @NonNullByDefault({}) HttpContextFactoryServiceImpl httpContextFactoryService;

    private @Mock @NonNullByDefault({}) Bundle bundleMock;
    private @Mock @NonNullByDefault({}) WrappingHttpContext httpContextMock;

    @BeforeEach
    public void setup() {
        httpContextFactoryService = new HttpContextFactoryServiceImpl();
        httpContextFactoryService.setHttpContext(httpContextMock);

        when(httpContextMock.wrap(bundleMock)).thenReturn(new BundleHttpContext(httpContextMock, bundleMock));
    }

    @Test
    public void shouldCreateHttpContext() {
        HttpContext context = httpContextFactoryService.createDefaultHttpContext(bundleMock);
        assertThat(context, is(notNullValue()));

        verify(httpContextMock).wrap(bundleMock);
    }

    @Test
    public void httpContextShouldCallgetResourceOnBundle() {
        HttpContext context = httpContextFactoryService.createDefaultHttpContext(bundleMock);
        context.getResource(RESOURCE);

        verify(httpContextMock).wrap(bundleMock);
        verify(bundleMock).getResource(RESOURCE);
    }

    @Test
    public void httpContextShouldCallgetResourceOnBundleWithoutLeadingSlash() {
        HttpContext context = httpContextFactoryService.createDefaultHttpContext(bundleMock);
        context.getResource("/" + RESOURCE);

        verify(httpContextMock).wrap(bundleMock);
        verify(bundleMock).getResource(RESOURCE);
    }
}
