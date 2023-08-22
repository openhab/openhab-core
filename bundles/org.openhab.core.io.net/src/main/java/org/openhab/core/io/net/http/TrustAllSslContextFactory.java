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
package org.openhab.core.io.net.http;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * The {@link TrustAllSslContextFactory} is a "trust all" implementation of {@link SslContextFactory.Client}
 * that will suppress the warning logs about trusting all certificates during its instantiation.
 * 
 * It is meant to be used only against local IoT devices with a self-signed certificate.
 * 
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class TrustAllSslContextFactory extends SslContextFactory.Client {
    public TrustAllSslContextFactory() {
        super(true);
    }

    @Override
    protected void checkTrustAll() {
        // Override parent implementation to suppress warning log
    }

    @Override
    protected void checkEndPointIdentificationAlgorithm() {
        // Override parent implementation to suppress warning log
    }
}
