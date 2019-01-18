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
package org.eclipse.smarthome.io.http.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import org.eclipse.smarthome.io.http.WrappingHttpContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class HttpContextFactoryServiceImplTest {

    private static final String RESOURCE = "resource";

    private HttpContextFactoryServiceImpl httpContextFactoryService;

    @Mock
    private Bundle bundle;

    @Mock
    private WrappingHttpContext httpContext;

    @Before
    public void setup() {
        initMocks(this);
        httpContextFactoryService = new HttpContextFactoryServiceImpl();
        httpContextFactoryService.setHttpContext(httpContext);

        when(httpContext.wrap(bundle)).thenReturn(new BundleHttpContext(httpContext, bundle));
    }

    @Test
    public void shouldCreateHttpContext() {
        HttpContext context = httpContextFactoryService.createDefaultHttpContext(bundle);
        assertThat(context, is(notNullValue()));

        verify(httpContext).wrap(bundle);
    }

    @Test
    public void httpContextShouldCallgetResourceOnBundle() {
        HttpContext context = httpContextFactoryService.createDefaultHttpContext(bundle);
        context.getResource(RESOURCE);

        verify(httpContext).wrap(bundle);
        verify(bundle).getResource(RESOURCE);
    }

    @Test
    public void httpContextShouldCallgetResourceOnBundleWithoutLeadingSlash() {
        HttpContext context = httpContextFactoryService.createDefaultHttpContext(bundle);
        context.getResource("/" + RESOURCE);

        verify(httpContext).wrap(bundle);
        verify(bundle).getResource(RESOURCE);
    }
}
