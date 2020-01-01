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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Internal utility class to handle TrustManager's
 *
 * @author Martin van Wingerden - Initial contribution
 */
@NonNullByDefault
class TrustManagerUtil {
    static X509ExtendedTrustManager keyStoreToTrustManager(@Nullable KeyStore keyStore) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            // Get hold of the X509ExtendedTrustManager
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509ExtendedTrustManager) {
                    return (X509ExtendedTrustManager) tm;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Default algorithm missing...", e);
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Problem while processing keystore", e);
        }
        throw new IllegalStateException("Could not find X509ExtendedTrustManager");
    }
}
