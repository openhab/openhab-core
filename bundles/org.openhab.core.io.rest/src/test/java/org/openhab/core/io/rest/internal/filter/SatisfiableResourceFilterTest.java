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
package org.openhab.core.io.rest.internal.filter;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openhab.core.io.rest.RESTResource;

/**
 * Test for {@link SatisfiableResourceFilter}
 *
 * @author Ivan Iliev - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java and use Mockito
 */
public class SatisfiableResourceFilterTest {

    private @Mock ContainerRequestContext context;
    private @Mock UriInfo uriInfo;
    private @Mock RESTResource resource;

    public @Rule MockitoRule mockitoRule = MockitoJUnit.rule();

    private SatisfiableResourceFilter filter = new SatisfiableResourceFilter();

    @Before
    public void before() {
        when(context.getUriInfo()).thenReturn(uriInfo);
    }

    @Test
    public void testWithBasicRESTResource() throws IOException {
        when(uriInfo.getMatchedResources()).thenReturn(singletonList(new RESTResource() {
        }));

        filter.filter(context);

        verify(context, never()).abortWith(isA(Response.class));
        verify(uriInfo).getMatchedResources();
    }

    @Test
    public void testWithSatisfiableRESTResourceSatisfied() throws IOException {
        when(uriInfo.getMatchedResources()).thenReturn(singletonList(resource));
        when(resource.isSatisfied()).thenReturn(true);

        filter.filter(context);

        verify(context, never()).abortWith(isA(Response.class));
        verify(uriInfo).getMatchedResources();
        verify(resource, atLeastOnce()).isSatisfied();
    }

    @Test
    public void testWithSatisfiableRESTResourceNOTSatisfied() throws IOException {
        when(uriInfo.getMatchedResources()).thenReturn(singletonList(resource));
        when(resource.isSatisfied()).thenReturn(false);

        filter.filter(context);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(context, times(1)).abortWith(captor.capture());
        assertEquals(Status.SERVICE_UNAVAILABLE.getStatusCode(), captor.getValue().getStatus());
        verify(uriInfo).getMatchedResources();
        verify(resource, atLeastOnce()).isSatisfied();
    }
}
