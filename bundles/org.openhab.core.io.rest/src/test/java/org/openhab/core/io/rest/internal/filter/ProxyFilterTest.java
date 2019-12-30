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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openhab.core.io.rest.internal.filter.ProxyFilter.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Test for {@link ProxyFilter}
 *
 * @author Ivan Iliev - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java and use Mockito
 */
public class ProxyFilterTest {

    private final ProxyFilter filter = new ProxyFilter();

    private @Mock ContainerRequestContext context;
    private @Mock UriInfo uriInfo;
    public @Rule MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void before() {
        when(context.getUriInfo()).thenReturn(uriInfo);
    }

    @Test
    public void basicTest() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("https", "eclipse.org");

        filter.filter(context);

        URI newBaseURI = new URI("https://eclipse.org/rest");
        URI newRequestURI = new URI("https://eclipse.org/rest/test");
        verify(context).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void basicTest2() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("http", "eclipse.org:8081");

        filter.filter(context);

        URI newBaseURI = new URI("http://eclipse.org:8081/rest");
        URI newRequestURI = new URI("http://eclipse.org:8081/rest/test");
        verify(context).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void noHeaderTest() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders(null, null);

        filter.filter(context);

        verify(context, never()).setRequestUri(any(URI.class), any(URI.class));
    }

    @Test
    public void onlySchemeTest() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("https", null);

        filter.filter(context);

        URI newBaseURI = new URI("https://localhost:8080/rest");
        URI newRequestURI = new URI("https://localhost:8080/rest/test");
        verify(context).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void onlySchemeDefaultHostWithoutPortTest() throws Exception {
        String baseURI = "http://localhost/rest";
        String requestURI = "http://localhost/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("https", null);

        filter.filter(context);

        URI newBaseURI = new URI("https://localhost/rest");
        URI newRequestURI = new URI("https://localhost/rest/test");
        verify(context).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void onlyHostTest() throws Exception {
        String baseURI = "http://localhost/rest";
        String requestURI = "http://localhost/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders(null, "eclipse.org:8081");

        filter.filter(context);

        URI newBaseURI = new URI("http://eclipse.org:8081/rest");
        URI newRequestURI = new URI("http://eclipse.org:8081/rest/test");
        verify(context).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void invalidHeaderTest() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("https", "://sometext\\\\///");

        filter.filter(context);

        verify(context, never()).setRequestUri(any(URI.class), any(URI.class));
    }

    private void setupContextURIs(String baseURI, String requestURI) {
        try {
            when(uriInfo.getBaseUri()).thenReturn(new URI(baseURI));
            when(uriInfo.getBaseUriBuilder()).thenReturn(new JerseyUriBuilder().uri(baseURI));
            when(uriInfo.getRequestUri()).thenReturn(new URI(requestURI));
            when(uriInfo.getRequestUriBuilder()).thenReturn(new JerseyUriBuilder().uri(requestURI));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error while setting up context mock", e);
        }
    }

    private void setupContextHeaders(String protoHeader, String hostHeader) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (protoHeader != null) {
            headers.put(PROTO_PROXY_HEADER, Collections.singletonList(protoHeader));
        }
        if (hostHeader != null) {
            headers.put(HOST_PROXY_HEADER, Collections.singletonList(hostHeader));
        }
        when(context.getHeaders()).thenReturn(headers);
    }

}
