/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test for {@link ProxyFilter}
 *
 * @author Ivan Iliev - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java and use Mockito
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ProxyFilterTest {

    private final ProxyFilter filter = new ProxyFilter();

    private @Mock ContainerRequestContext context;
    private @Mock UriInfo uriInfo;

    @BeforeEach
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
            when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(baseURI));
            when(uriInfo.getRequestUri()).thenReturn(new URI(requestURI));
            when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromUri(requestURI));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error while setting up context mock", e);
        }
    }

    private void setupContextHeaders(String protoHeader, String hostHeader) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (protoHeader != null) {
            headers.put(PROTO_PROXY_HEADER, List.of(protoHeader));
        }
        if (hostHeader != null) {
            headers.put(HOST_PROXY_HEADER, List.of(hostHeader));
        }
        when(context.getHeaders()).thenReturn(headers);
    }
}
