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
package org.openhab.core.net;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * @author Henning Treu - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class HttpServiceUtilTest {

    private static final String ORG_OSGI_SERVICE_HTTP_SERVICE = "org.osgi.service.http.HttpService";
    private static final String HTTP_PORT = "org.osgi.service.http.port";
    private static final String HTTP_PORT_SECURE = "org.osgi.service.http.port.secure";

    private @Mock @NonNullByDefault({}) BundleContext bundleContextMock;

    @BeforeEach
    public void setup() throws InvalidSyntaxException {
        ServiceReference<?>[] httpServiceReferences = getHttpServiceReferences();
        ServiceReference<?>[] secureHttpServiceReferences = getSecureHttpServiceReferences();

        when(bundleContextMock.getAllServiceReferences(ORG_OSGI_SERVICE_HTTP_SERVICE, null)).thenReturn(
                Stream.concat(Arrays.stream(httpServiceReferences), Arrays.stream(secureHttpServiceReferences))
                        .toArray(ServiceReference<?>[]::new));
    }

    @Test
    public void shouldReturnHttpServicePortFromServiceReference() {
        int port = HttpServiceUtil.getHttpServicePort(bundleContextMock);

        assertThat(port, is(8080));
    }

    @Test
    public void shouldReturnHttpServicePortSecureFromServiceReference() {
        int port = HttpServiceUtil.getHttpServicePortSecure(bundleContextMock);

        assertThat(port, is(48080));
    }

    @Test
    public void shouldReturnHttpServicePortFromSystemProperty() throws InvalidSyntaxException {
        when(bundleContextMock.getAllServiceReferences(ORG_OSGI_SERVICE_HTTP_SERVICE, null))
                .thenReturn(new ServiceReference[0]);
        when(bundleContextMock.getProperty(HTTP_PORT)).thenReturn("9090");

        int port = HttpServiceUtil.getHttpServicePort(bundleContextMock);

        assertThat(port, is(9090));
    }

    @Test
    public void shouldReturnHttpServicePortSecureFromSystemProperty() throws InvalidSyntaxException {
        when(bundleContextMock.getAllServiceReferences(ORG_OSGI_SERVICE_HTTP_SERVICE, null))
                .thenReturn(new ServiceReference[0]);
        when(bundleContextMock.getProperty(HTTP_PORT_SECURE)).thenReturn("49090");

        int port = HttpServiceUtil.getHttpServicePortSecure(bundleContextMock);

        assertThat(port, is(49090));
    }

    @Test
    public void shouldReturnUndefinedForException() throws InvalidSyntaxException {
        when(bundleContextMock.getAllServiceReferences(ORG_OSGI_SERVICE_HTTP_SERVICE, null))
                .thenThrow(new InvalidSyntaxException(null, null));

        int undfinedPort = HttpServiceUtil.getHttpServicePort(bundleContextMock);
        assertThat(undfinedPort, is(-1));
    }

    private ServiceReference<?>[] getHttpServiceReferences() {
        ServiceReference<?> ref1 = mock(ServiceReference.class);
        when(ref1.getProperty(HTTP_PORT)).thenReturn("8081");
        when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn("10");

        ServiceReference<?> ref2 = mock(ServiceReference.class);
        when(ref2.getProperty(HTTP_PORT)).thenReturn("8080");
        when(ref2.getProperty(Constants.SERVICE_RANKING)).thenReturn("100");

        return new ServiceReference[] { ref1, ref2 };
    }

    private ServiceReference<?>[] getSecureHttpServiceReferences() {
        ServiceReference<?> ref1 = mock(ServiceReference.class);
        when(ref1.getProperty(HTTP_PORT_SECURE)).thenReturn("48081");
        when(ref1.getProperty(Constants.SERVICE_RANKING)).thenReturn("1");

        ServiceReference<?> ref2 = mock(ServiceReference.class);
        when(ref2.getProperty(HTTP_PORT_SECURE)).thenReturn("48080");
        when(ref2.getProperty(Constants.SERVICE_RANKING)).thenReturn("2");

        return new ServiceReference[] { ref1, ref2 };
    }
}
