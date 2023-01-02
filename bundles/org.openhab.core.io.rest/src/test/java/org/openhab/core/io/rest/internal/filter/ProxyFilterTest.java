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
package org.openhab.core.io.rest.internal.filter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openhab.core.io.rest.internal.filter.ProxyFilter.*;

import java.net.URI;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ProxyFilterTest {

    private final ProxyFilter filter = new ProxyFilter();

    private @Mock @NonNullByDefault({}) ContainerRequestContext contextMock;
    private @Mock @NonNullByDefault({}) UriInfo uriInfoMock;

    @BeforeEach
    public void before() {
        when(contextMock.getUriInfo()).thenReturn(uriInfoMock);
    }

    @Test
    public void basicTest() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("https", "eclipse.org");

        filter.filter(contextMock);

        URI newBaseURI = new URI("https://eclipse.org/rest");
        URI newRequestURI = new URI("https://eclipse.org/rest/test");
        verify(contextMock).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void basicTest2() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("http", "eclipse.org:8081");

        filter.filter(contextMock);

        URI newBaseURI = new URI("http://eclipse.org:8081/rest");
        URI newRequestURI = new URI("http://eclipse.org:8081/rest/test");
        verify(contextMock).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void hostListTest() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("https", "eclipse.org, foo.bar");

        filter.filter(contextMock);

        URI newBaseURI = new URI("https://eclipse.org/rest");
        URI newRequestURI = new URI("https://eclipse.org/rest/test");
        verify(contextMock).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void noHeaderTest() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders(null, null);

        filter.filter(contextMock);

        verify(contextMock, never()).setRequestUri(any(URI.class), any(URI.class));
    }

    @Test
    public void onlySchemeTest() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("https", null);

        filter.filter(contextMock);

        URI newBaseURI = new URI("https://localhost:8080/rest");
        URI newRequestURI = new URI("https://localhost:8080/rest/test");
        verify(contextMock).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void onlySchemeDefaultHostWithoutPortTest() throws Exception {
        String baseURI = "http://localhost/rest";
        String requestURI = "http://localhost/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("https", null);

        filter.filter(contextMock);

        URI newBaseURI = new URI("https://localhost/rest");
        URI newRequestURI = new URI("https://localhost/rest/test");
        verify(contextMock).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void onlyHostTest() throws Exception {
        String baseURI = "http://localhost/rest";
        String requestURI = "http://localhost/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders(null, "eclipse.org:8081");

        filter.filter(contextMock);

        URI newBaseURI = new URI("http://eclipse.org:8081/rest");
        URI newRequestURI = new URI("http://eclipse.org:8081/rest/test");
        verify(contextMock).setRequestUri(eq(newBaseURI), eq(newRequestURI));
    }

    @Test
    public void invalidHeaderTest() throws Exception {
        String baseURI = "http://localhost:8080/rest";
        String requestURI = "http://localhost:8080/rest/test";
        setupContextURIs(baseURI, requestURI);

        setupContextHeaders("https", "://sometext\\\\///");

        filter.filter(contextMock);

        verify(contextMock, never()).setRequestUri(any(URI.class), any(URI.class));
    }

    private void setupContextURIs(String baseURI, String requestURI) {
        when(uriInfoMock.getBaseUri()).thenReturn(URI.create(baseURI));
        when(uriInfoMock.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(baseURI));
        when(uriInfoMock.getRequestUri()).thenReturn(URI.create(requestURI));
        when(uriInfoMock.getRequestUriBuilder()).thenReturn(UriBuilder.fromUri(requestURI));
    }

    private void setupContextHeaders(@Nullable String protoHeader, @Nullable String hostHeader) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (protoHeader != null) {
            headers.put(PROTO_PROXY_HEADER, List.of(protoHeader));
        }
        if (hostHeader != null) {
            headers.put(HOST_PROXY_HEADER, List.of(hostHeader));
        }
        when(contextMock.getHeaders()).thenReturn(headers);
    }
}
