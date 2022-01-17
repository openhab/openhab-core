/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
public class HttpContextFactoryServiceImplTest {

    private static final String RESOURCE = "resource";

    private HttpContextFactoryServiceImpl httpContextFactoryService;

    private @Mock Bundle bundle;
    private @Mock WrappingHttpContext httpContext;

    @BeforeEach
    public void setup() {
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
