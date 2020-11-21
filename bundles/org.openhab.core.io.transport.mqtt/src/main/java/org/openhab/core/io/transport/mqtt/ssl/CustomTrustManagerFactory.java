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

import java.security.KeyStore;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;

/**
 * The {@link CustomTrustManagerFactory} is a TrustManagerFactory that provides a custom {@link TrustManager}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CustomTrustManagerFactory extends SimpleTrustManagerFactory {
    private final TrustManager[] trustManagers;

    public CustomTrustManagerFactory(TrustManager[] trustManagers) {
        this.trustManagers = trustManagers;
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
