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
package org.openhab.core.io.transport.mqtt.sslcontext;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This SSLContextProvider returns an {@link SSLContext} that accepts all connections and doesn't perform any
 * certificate validations. This implementation forces a TLS v1.2 {@link SSLContext} instance.
 *
 * @author Jan N. Klug - Initial contribution
 */
@Deprecated
@NonNullByDefault
public class CustomSSLContextProvider implements SSLContextProvider {
    private final Logger logger = LoggerFactory.getLogger(CustomSSLContextProvider.class);
    private final @Nullable TrustManagerFactory factory;

    public CustomSSLContextProvider(@Nullable TrustManagerFactory factory) {
        this.factory = factory;
    }

    @Override
    public SSLContext getContext() throws ConfigurationException {
        try {
            if (factory == null) {
                return SSLContext.getDefault();
            } else {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, factory.getTrustManagers(), null);
                return sslContext;
            }
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            logger.warn("SSL configuration failed", e);
            throw new ConfigurationException("ssl", e.getMessage());
        }
    }
}
