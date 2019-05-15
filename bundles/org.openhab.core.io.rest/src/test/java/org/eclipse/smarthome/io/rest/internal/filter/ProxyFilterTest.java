/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.internal.filter;

import static org.eclipse.smarthome.io.rest.internal.filter.ProxyFilter.HOST_PROXY_HEADER;
import static org.eclipse.smarthome.io.rest.internal.filter.ProxyFilter.PROTO_PROXY_HEADER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

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

    private static class UriBuilderEx extends UriBuilder {

        private String scheme = "http";
        private String host = "";

        @Override
        public UriBuilder clone() {
            return this;
        }

        @Override
        public UriBuilder uri(URI uri) {
            return this;
        }

        @Override
        public UriBuilder uri(String uriTemplate) {
            return this;
        }

        @Override
        public UriBuilder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        @Override
        public UriBuilder schemeSpecificPart(String ssp) {
            return this;
        }

        @Override
        public UriBuilder userInfo(String ui) {
            return this;
        }

        @Override
        public UriBuilder host(String host) {
            return this;
        }

        @Override
        public UriBuilder port(int port) {
            return this;
        }

        @Override
        public UriBuilder replacePath(String path) {
            return this;
        }

        @Override
        public UriBuilder path(String path) {
            return this;
        }

        @Override
        public UriBuilder path(Class resource) {
            return this;
        }

        @Override
        public UriBuilder path(Class resource, String method) {
            return this;
        }

        @Override
        public UriBuilder path(Method method) {
            return this;
        }

        @Override
        public UriBuilder segment(String... segments) {
            return this;
        }

        @Override
        public UriBuilder replaceMatrix(String matrix) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public UriBuilder matrixParam(String name, Object... values) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public UriBuilder replaceMatrixParam(String name, Object... values) {
            return this;
        }

        @Override
        public UriBuilder replaceQuery(String query) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public UriBuilder queryParam(String name, Object... values) {
            return this;
        }

        @Override
        public UriBuilder replaceQueryParam(String name, Object... values) {
            return this;
        }

        @Override
        public UriBuilder fragment(String fragment) {
            return this;
        }

        @Override
        public UriBuilder resolveTemplate(String name, Object value) {
            return this;
        }

        @Override
        public UriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
            return this;
        }

        @Override
        public UriBuilder resolveTemplateFromEncoded(String name, Object value) {
            return this;
        }

        @Override
        public UriBuilder resolveTemplates(Map<String, Object> templateValues) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public UriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath)
                throws IllegalArgumentException {
            return this;
        }

        @Override
        public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
            return this;
        }

        @Override
        public URI buildFromMap(Map<String, ?> values) {
            return null;
        }

        @Override
        public URI buildFromMap(Map<String, ?> values, boolean encodeSlashInPath)
                throws IllegalArgumentException, UriBuilderException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public URI buildFromEncodedMap(Map<String, ?> values) throws IllegalArgumentException, UriBuilderException {
            return null;
        }

        @Override
        public URI build(Object... values) throws IllegalArgumentException, UriBuilderException {
            return URI.create(scheme + "://" + host);
        }

        @Override
        public URI build(Object[] values, boolean encodeSlashInPath)
                throws IllegalArgumentException, UriBuilderException {
            return null;
        }

        @Override
        public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException {
            return null;
        }

        @Override
        public String toTemplate() {
            return null;
        }

    }

    private @Mock ContainerRequestContext context;
    private @Mock UriInfo uriInfo;
    private @Mock UriBuilder uriBuilderBase;
    private @Mock UriBuilder uriBuilderRequest;
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
        when(uriInfo.getBaseUri()).thenReturn(URI.create(baseURI));
        when(uriBuilderBase.build()).thenReturn(URI.create(baseURI));
        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilderBase);
        when(uriInfo.getRequestUri()).thenReturn(URI.create(requestURI));
        when(uriBuilderRequest.build()).thenReturn(URI.create(requestURI));
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilderRequest);
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
