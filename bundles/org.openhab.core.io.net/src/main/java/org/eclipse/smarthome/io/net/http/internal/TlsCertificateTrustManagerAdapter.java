/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.net.http.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.X509ExtendedTrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.io.net.http.TlsCertificateProvider;
import org.eclipse.smarthome.io.net.http.TlsTrustManagerProvider;

/**
 * Adapter to use a {@code TlsCertificateProvider} as a {@code TlsTrustManagerProvider}
 *
 * @author Martin van Wingerden - Initial contribution
 */
@NonNullByDefault
class TlsCertificateTrustManagerAdapter implements TlsTrustManagerProvider {

    private final String hostname;
    private final X509ExtendedTrustManager trustManager;

    TlsCertificateTrustManagerAdapter(TlsCertificateProvider tlsCertificateProvider) {
        this.hostname = tlsCertificateProvider.getHostName();
        this.trustManager = trustManagerFromCertificate(this.hostname, tlsCertificateProvider.getCertificate());
    }

    @Override
    public String getHostName() {
        return hostname;
    }

    @Override
    public X509ExtendedTrustManager getTrustManager() {
        return trustManager;
    }

    private static X509ExtendedTrustManager trustManagerFromCertificate(String hostname, URL certificateUrl) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            try (InputStream inputStream = certificateUrl.openStream()) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, null);
                Certificate certificate = certificateFactory.generateCertificate(inputStream);
                keyStore.setCertificateEntry(hostname, certificate);

                return TrustManagerUtil.keyStoreToTrustManager(keyStore);
            } catch (KeyStoreException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Failed to initialize internal keystore", e);
            }
        } catch (CertificateException | IOException e) {
            throw new IllegalStateException("Failed to initialize TrustManager", e);
        }
    }
}
