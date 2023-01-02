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
package org.openhab.core.io.net.http.internal;

import static org.mockito.Mockito.*;

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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.io.net.http.TlsTrustManagerProvider;

/**
 * Tests which validate the behavior of the ExtensibleTrustManager
 *
 * @author Martin van Wingerden - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ExtensibleTrustManagerImplTest {

    private @NonNullByDefault({}) X509Certificate[] chain;
    private @NonNullByDefault({}) ExtensibleTrustManagerImpl subject;

    private @Mock @NonNullByDefault({}) TlsTrustManagerProvider trustmanagerProviderMock;
    private @Mock @NonNullByDefault({}) TlsTrustManagerProvider trustmanagerProviderHostPortMock;
    private @Mock @NonNullByDefault({}) X509ExtendedTrustManager trustmanagerMock;
    private @Mock @NonNullByDefault({}) X509ExtendedTrustManager trustmanager2Mock;
    private @Mock @NonNullByDefault({}) X509ExtendedTrustManager defaultTrustManagerMock;
    private @Mock @NonNullByDefault({}) SSLEngine sslEngineMock;
    private @Mock @NonNullByDefault({}) X509Certificate topOfChainMock;
    private @Mock @NonNullByDefault({}) X509Certificate bottomOfChainMock;

    @BeforeEach
    public void setup() {
        when(trustmanagerProviderMock.getHostName()).thenReturn("example.org");
        when(trustmanagerProviderMock.getTrustManager()).thenReturn(trustmanagerMock);

        when(trustmanagerProviderHostPortMock.getHostName()).thenReturn("example.org:443");
        when(trustmanagerProviderHostPortMock.getTrustManager()).thenReturn(trustmanager2Mock);

        subject = new ExtensibleTrustManagerImpl();
        subject.addTlsTrustManagerProvider(trustmanagerProviderMock);
        subject.addTlsTrustManagerProvider(trustmanagerProviderHostPortMock);

        chain = new X509Certificate[] { topOfChainMock, bottomOfChainMock };
    }

    @Test
    public void shouldForwardCallsToMockForMatchingCN() throws CertificateException {
        when(topOfChainMock.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=example.org, OU=Core, O=openHAB, C=DE"));

        subject.checkServerTrusted(chain, "just");

        verify(trustmanagerMock).checkServerTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanagerMock, trustmanager2Mock);
    }

    @Test
    public void shouldForwardCallsToMockForMatchingHost() throws CertificateException {
        when(sslEngineMock.getPeerHost()).thenReturn("example.org");
        when(sslEngineMock.getPeerPort()).thenReturn(443);

        subject.checkServerTrusted(chain, "just", sslEngineMock);

        verify(trustmanager2Mock).checkServerTrusted(chain, "just", sslEngineMock);
        verifyNoMoreInteractions(trustmanagerMock, trustmanager2Mock);
    }

    @Test
    public void shouldForwardCallsToMockForMatchingAlternativeNames() throws CertificateException {
        when(topOfChainMock.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=example.com, OU=Core, O=openHAB, C=DE"));
        when(topOfChainMock.getSubjectAlternativeNames())
                .thenReturn(constructAlternativeNames("example1.com", "example.org"));

        subject.checkClientTrusted(chain, "just");

        verify(trustmanagerMock).checkClientTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanagerMock);
    }

    @Test
    public void shouldBeResilientAgainstNullSubjectAlternativeNames()
            throws CertificateException, IllegalAccessException, NoSuchFieldException, SecurityException {
        writeField(subject, "defaultTrustManager", defaultTrustManagerMock, true);

        when(topOfChainMock.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=example.com, OU=Core, O=openHAB, C=DE"));
        when(topOfChainMock.getSubjectAlternativeNames()).thenReturn(null);

        subject.checkClientTrusted(chain, "just");

        verify(defaultTrustManagerMock).checkClientTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanagerMock);
    }

    @Test
    public void shouldBeResilientAgainstMissingCommonNames() throws CertificateException, IllegalAccessException,
            NoSuchFieldException, SecurityException, IllegalArgumentException {
        writeField(subject, "defaultTrustManager", defaultTrustManagerMock, true);

        when(topOfChainMock.getSubjectX500Principal()).thenReturn(new X500Principal("OU=Core, O=openHAB, C=DE"));

        subject.checkClientTrusted(chain, "just");

        verify(defaultTrustManagerMock).checkClientTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanagerMock);
    }

    @Test
    public void shouldBeResilientAgainstInvalidCertificates() throws CertificateException, IllegalAccessException,
            NoSuchFieldException, SecurityException, IllegalArgumentException {
        writeField(subject, "defaultTrustManager", defaultTrustManagerMock, true);

        when(topOfChainMock.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=example.com, OU=Core, O=openHAB, C=DE"));
        when(topOfChainMock.getSubjectAlternativeNames())
                .thenThrow(new CertificateParsingException("Invalid certificate!!!"));

        subject.checkClientTrusted(chain, "just");

        verify(defaultTrustManagerMock).checkClientTrusted(chain, "just", (Socket) null);
        verifyNoMoreInteractions(trustmanagerMock);
    }

    @Test
    public void shouldNotForwardCallsToMockForDifferentCN() throws CertificateException, IllegalAccessException,
            NoSuchFieldException, SecurityException, IllegalArgumentException {
        writeField(subject, "defaultTrustManager", defaultTrustManagerMock, true);
        mockSubjectForCertificate(topOfChainMock, "CN=example.com, OU=Core, O=openHAB, C=DE");
        mockIssuerForCertificate(topOfChainMock, "CN=openHAB, OU=Core, O=openHAB, C=DE");
        mockSubjectForCertificate(bottomOfChainMock, "CN=openHAB, OU=Core, O=openHAB, C=DE");
        mockIssuerForCertificate(bottomOfChainMock, "");
        when(topOfChainMock.getEncoded()).thenReturn(new byte[0]);

        subject.checkServerTrusted(chain, "just");

        verify(defaultTrustManagerMock).checkServerTrusted(chain, "just", (Socket) null);
        verifyNoInteractions(trustmanagerMock);
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
