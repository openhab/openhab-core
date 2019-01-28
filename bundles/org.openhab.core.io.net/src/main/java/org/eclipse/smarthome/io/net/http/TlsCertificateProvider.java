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

import java.net.URL;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Provides a certificate for the given host name
 *
 * Implement this interface to request the framework to use a specific certificate for the given host
 *
 * NOTE: implementations of this interface should be immutable, to guarantee efficient and correct functionality
 *
 * @author Martin van Wingerden - Initial Contribution
 */
@NonNullByDefault
public interface TlsCertificateProvider extends TlsProvider {

    /**
     * A resources pointing to a X509 certificate for the specified host name
     *
     * @return this should refer to a file containing a base64 encoded X.509 certificate
     */
    URL getCertificate();
}
