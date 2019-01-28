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
package org.eclipse.smarthome.io.transport.mqtt.sslcontext;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This SSLContextProvider returns an {@link SSLContext} that accepts all connections and doesn't perform any
 * certificate validations. This implementation forces a TLS v1.2 {@link SSLContext} instance.
 *
 * @author David Graeff - Initial contribution
 */
public class AcceptAllCertificatesSSLContext implements SSLContextProvider {
    private final Logger logger = LoggerFactory.getLogger(AcceptAllCertificatesSSLContext.class);

    TrustManager trustManager = new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    };

    @Override
    public SSLContext getContext() throws ConfigurationException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            return sslContext;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            logger.warn("SSL configuration failed", e);
            throw new ConfigurationException("ssl", e.getMessage());
        }
    }
}
