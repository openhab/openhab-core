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
package org.openhab.core.io.rest.core.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.openhab.core.io.jetty.certificate.CertificateGenerator.KEYSTORE_ENTRY_ALIAS;
import static org.openhab.core.io.jetty.certificate.CertificateGenerator.KEYSTORE_JKS_TYPE;
import static org.openhab.core.io.jetty.certificate.CertificateGenerator.KEYSTORE_PASSWORD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.io.jetty.certificate.CertificateGenerator;

/**
 * The {@link CertificateResourceTest} contains tests for the {@link CertificateResource}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CertificateResourceTest {
    private static final String KEY_STORE_PATH_STRING = "target/test_keystore";
    private static final Path KEY_STORE_PATH = Path.of(KEY_STORE_PATH_STRING);

    private @NonNullByDefault({}) CertificateResource certificateResource;
    private CertificateResource.CertificateDTO fromPEM = new CertificateResource.CertificateDTO();

    @BeforeEach
    public void setup() throws IOException {
        System.setProperty(CertificateGenerator.JETTY_KEYSTORE_PATH_PROPERTY, KEY_STORE_PATH_STRING);

        fromPEM.certificateChain = Files.readString(Path.of("src/test/resources/cert/chain.pem"));
        fromPEM.privateKey = Files.readString(Path.of("src/test/resources/cert/server.key"));

        certificateResource = new CertificateResource();
    }

    @AfterEach
    public void cleanUp() throws IOException {
        Files.deleteIfExists(KEY_STORE_PATH);
    }

    @Test
    public void certificateFromPEM() throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, UnrecoverableKeyException {
        Response response = certificateResource.putCertificate(fromPEM);
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));

        assertThat(Files.exists(KEY_STORE_PATH), is(true));

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_JKS_TYPE);
        keyStore.load(Files.newInputStream(KEY_STORE_PATH), KEYSTORE_PASSWORD.toCharArray());

        Certificate certificate = keyStore.getCertificate(KEYSTORE_ENTRY_ALIAS);
        assertThat(certificate, notNullValue());
        assertThat(((X509Certificate) certificate).getSubjectDN().getName(),
                is("CN=TestCert, ST=North Rhine-Westphalia, C=DE"));

        Key key = keyStore.getKey(KEYSTORE_ENTRY_ALIAS, KEYSTORE_PASSWORD.toCharArray());
        assertThat(key, instanceOf(PrivateKey.class));
    }

    @Test
    public void selfSignedCertificate() throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, UnrecoverableKeyException {
        Response response = certificateResource.putCertificate(new CertificateResource.CertificateDTO());

        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));

        assertThat(Files.exists(KEY_STORE_PATH), is(true));

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_JKS_TYPE);
        keyStore.load(Files.newInputStream(KEY_STORE_PATH), KEYSTORE_PASSWORD.toCharArray());

        Certificate certificate = keyStore.getCertificate(KEYSTORE_ENTRY_ALIAS);
        assertThat(certificate, notNullValue());
        assertThat(((X509Certificate) certificate).getSubjectDN().getName(),
                is("C=None, L=None, O=None, OU=None, CN=openhab.org"));

        Key key = keyStore.getKey(KEYSTORE_ENTRY_ALIAS, KEYSTORE_PASSWORD.toCharArray());
        assertThat(key, instanceOf(PrivateKey.class));
    }

    @Test
    public void invalidCertificate() {
        fromPEM.certificateChain = "invalid";
        Response response = certificateResource.putCertificate(fromPEM);

        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(Files.exists(KEY_STORE_PATH), is(false));
    }

    @Test
    public void invalidPrivateKey() {
        fromPEM.certificateChain = "invalid";
        Response response = certificateResource.putCertificate(fromPEM);

        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(Files.exists(KEY_STORE_PATH), is(false));
    }
}
