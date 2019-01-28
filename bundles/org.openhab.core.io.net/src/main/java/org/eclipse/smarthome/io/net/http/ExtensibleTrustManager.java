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
package org.eclipse.smarthome.io.net.http;

import javax.net.ssl.TrustManager;

/**
 * Provides an extensible composite TrustManager
 *
 * The trust manager can be extended with implementations of the following interfaces:
 *
 * - {@code TlsTrustManagerProvider}
 * - {@code TlsCertificateProvider}
 *
 * @author Martin van Wingerden - Initial Contribution
 */
public interface ExtensibleTrustManager extends TrustManager {

    /**
     * Add a {@code TlsCertificateProvider} to be used by HttpClient / WebSocket Client's
     *
     * When the Provider is no longer valid please make sure to remove it.
     *
     * @param tlsCertificateProvider same instance as given when removing
     */
    void addTlsCertificateProvider(TlsCertificateProvider tlsCertificateProvider);

    /**
     * Remove a {@code TlsCertificateProvider} so it will longer be used by HttpClient / WebSocket Client's
     *
     * @param tlsCertificateProvider same instance as given when adding
     */
    void removeTlsCertificateProvider(TlsCertificateProvider tlsCertificateProvider);

    /**
     * Add a {@code TlsTrustManagerProvider} to be used by HttpClient / WebSocket Client's
     *
     * When the Provider is no longer valid please make sure to remove it.
     *
     * @param tlsTrustManagerProvider same instance as given when removing
     */
    void addTlsTrustManagerProvider(TlsTrustManagerProvider tlsTrustManagerProvider);

    /**
     * Remove a {@code TlsTrustManagerProvider} so it will longer be used by HttpClient / WebSocket Client's
     *
     * @param tlsTrustManagerProvider same instance as given when adding
     */
    void removeTlsTrustManagerProvider(TlsTrustManagerProvider tlsTrustManagerProvider);
}
