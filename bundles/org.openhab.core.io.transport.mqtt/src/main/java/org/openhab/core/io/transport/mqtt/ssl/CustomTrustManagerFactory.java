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
package org.openhab.core.io.transport.mqtt.ssl;

import java.lang.reflect.Field;
import java.security.KeyStore;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.sslcontext.SSLContextProvider;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SimpleTrustManagerFactory;

/**
 * The {@link CustomTrustManagerFactory} is a TrustManagerFactory that provides a custom {@link TrustManager}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CustomTrustManagerFactory extends SimpleTrustManagerFactory {
    private final Logger logger = LoggerFactory.getLogger(CustomTrustManagerFactory.class);
    private final TrustManager[] trustManagers;

    public CustomTrustManagerFactory(TrustManager[] trustManagers) {
        this.trustManagers = trustManagers;
    }

    @Deprecated
    public CustomTrustManagerFactory(SSLContextProvider contextProvider) {
        TrustManager[] tm;
        try {
            SSLContext ctx = contextProvider.getContext();

            // get SSLContextImpl
            Field contextSpiField = ctx.getClass().getDeclaredField("contextSpi");
            contextSpiField.setAccessible(true);
            Object sslContextImpl = contextSpiField.get(ctx);
            Class<?> sslContextImplClass = sslContextImpl.getClass().getSuperclass().getSuperclass();

            // get trustmanager
            Field trustManagerField = sslContextImplClass.getDeclaredField("trustManager");
            trustManagerField.setAccessible(true);
            Object trustManagerObj = trustManagerField.get(sslContextImpl);

            tm = new TrustManager[] { (X509TrustManager) trustManagerObj };
        } catch (IllegalAccessException | NoSuchFieldException | ConfigurationException e) {
            logger.warn("using default insecure trustmanager, could not extract trustmanager from SSL context:", e);
            tm = InsecureTrustManagerFactory.INSTANCE.getTrustManagers();
        }
        trustManagers = tm;
    }

    @Override
    protected void engineInit(@Nullable KeyStore keyStore) throws Exception {
    }

    @Override
    protected void engineInit(@Nullable ManagerFactoryParameters managerFactoryParameters) throws Exception {
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return trustManagers;
    }
}
