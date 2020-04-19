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
package org.openhab.core.io.net.http.internal;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.io.net.http.TlsTrustManagerProvider;

/**
 * Tests which validate the behavior of the ExtensibleTrustManager
 *
 * @author Martin van Wingerden - Initial contribution
 */
public class ExtensibleTrustManagerImplTest {

    private ExtensibleTrustManagerImpl subject;

    @Mock
    private TlsTrustManagerProvider trustmanagerProvider;

    @Mock
    private TlsTrustManagerProvider trustmanagerProviderHostPort;

    @Mock
    private X509ExtendedTrustManager trustmanager;

    @Mock
    private X509ExtendedTrustManager trustmanager2;

    @Mock
    private X509ExtendedTrustManager defaultTrustManager;

    @Mock
    private SSLEngine sslEngine;

    @Mock
    private X509Certificate topOfChain;

    @Mock
    private X509Certificate bottomOfChain;

    private X509Certificate[] chain;

    @Before
    public void setup() {
        initMocks(this);

        when(trustmanagerProvider.getHostName()).thenReturn("example.org");
        when(trustmanagerProvider.getTrustManager()).thenReturn(trustmanager);

        when(trustmanagerProviderHostPort.getHostName()).thenReturn("example.org:443");
        when(trustmanagerProviderHostPort.getTrustManager()).thenReturn(trustmanager2);

        subject = new ExtensibleTrustManagerImpl();
        subject.addTlsTrustManagerProvider(trustmanagerProvider);
        subject.addTlsTrustManagerProvider(trustmanagerProviderHostPort);

        chain = new X509Certificate[] { topOfChain, bottomOfChain };
    }

    @Test
    public void shouldForwardCallsToMockForMatchingCN() throws CertificateException {
        when(topOfChain.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=example.org, OU=Smarthome, O=Eclipse, C=DE"));

        subject.checkServerTrusted(chain, "just");

        verify(trustmanager).checkServerTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanager, trustmanager2);
    }

    @Test
    public void shouldForwardCallsToMockForMatchingHost() throws CertificateException {
        when(sslEngine.getPeerHost()).thenReturn("example.org");
        when(sslEngine.getPeerPort()).thenReturn(443);

        subject.checkServerTrusted(chain, "just", sslEngine);

        verify(trustmanager2).checkServerTrusted(chain, "just", sslEngine);
        verifyNoMoreInteractions(trustmanager, trustmanager2);
    }

    @Test
    public void shouldForwardCallsToMockForMatchingAlternativeNames() throws CertificateException {
        when(topOfChain.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=example.com, OU=Smarthome, O=Eclipse, C=DE"));
        when(topOfChain.getSubjectAlternativeNames())
                .thenReturn(constructAlternativeNames("example1.com", "example.org"));

        subject.checkClientTrusted(chain, "just");

        verify(trustmanager).checkClientTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanager);
    }

    @Test
    public void shouldBeResilientAgainstNullSubjectAlternativeNames()
            throws CertificateException, IllegalAccessException, NoSuchFieldException, SecurityException {
        writeField(subject, "defaultTrustManager", defaultTrustManager, true);

        when(topOfChain.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=example.com, OU=Smarthome, O=Eclipse, C=DE"));
        when(topOfChain.getSubjectAlternativeNames()).thenReturn(null);

        subject.checkClientTrusted(chain, "just");

        verify(defaultTrustManager).checkClientTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanager);
    }

    @Test
    public void shouldBeResilientAgainstMissingCommonNames() throws CertificateException, IllegalAccessException,
            NoSuchFieldException, SecurityException, IllegalArgumentException {
        writeField(subject, "defaultTrustManager", defaultTrustManager, true);

        when(topOfChain.getSubjectX500Principal()).thenReturn(new X500Principal("OU=Smarthome, O=Eclipse, C=DE"));

        subject.checkClientTrusted(chain, "just");

        verify(defaultTrustManager).checkClientTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanager);
    }

    @Test
    public void shouldBeResilientAgainstInvalidCertificates() throws CertificateException, IllegalAccessException,
            NoSuchFieldException, SecurityException, IllegalArgumentException {
        writeField(subject, "defaultTrustManager", defaultTrustManager, true);

        when(topOfChain.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=example.com, OU=Smarthome, O=Eclipse, C=DE"));
        when(topOfChain.getSubjectAlternativeNames())
                .thenThrow(new CertificateParsingException("Invalid certificate!!!"));

        subject.checkClientTrusted(chain, "just");

        verify(defaultTrustManager).checkClientTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanager);
    }

    @Test
    public void shouldNotForwardCallsToMockForDifferentCN() throws CertificateException, IllegalAccessException,
            NoSuchFieldException, SecurityException, IllegalArgumentException {
        writeField(subject, "defaultTrustManager", defaultTrustManager, true);
        mockSubjectForCertificate(topOfChain, "CN=example.com, OU=Smarthome, O=Eclipse, C=DE");
        mockIssuerForCertificate(topOfChain, "CN=Eclipse, OU=Smarthome, O=Eclipse, C=DE");
        mockSubjectForCertificate(bottomOfChain, "CN=Eclipse, OU=Smarthome, O=Eclipse, C=DE");
        mockIssuerForCertificate(bottomOfChain, "");
        when(topOfChain.getEncoded()).thenReturn(new byte[0]);

        subject.checkServerTrusted(chain, "just");

        verify(defaultTrustManager).checkServerTrusted(chain, "just", (Socket) null);
        verifyNoInteractions(trustmanager);
    }

    private Collection<List<?>> constructAlternativeNames(String... alternatives) {
        Collection<List<?>> alternativeNames = new ArrayList<>();
        for (String alternative : alternatives) {
            alternativeNames.add(Stream.of(0, alternative).collect(Collectors.toList()));
        }

        return alternativeNames;
    }

    private void mockSubjectForCertificate(X509Certificate certificate, String principal) {
        when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal(principal));
    }

    private void mockIssuerForCertificate(X509Certificate certificate, String principal) {
        when(certificate.getIssuerX500Principal()).thenReturn(new X500Principal(principal));
    }

    private void writeField(Object target, String fieldName, Object value, boolean forceAccess)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(forceAccess);
        field.set(target, value);
    }
}
