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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Provides some TLS validation implementation for the given host name
 *
 * You should implement one of children of this interface, in order to request the framework to use a specific
 * implementation for the given host.
 *
 * NOTE: implementations of this interface should be immutable, to guarantee efficient and correct functionality
 *
 * @author Martin van Wingerden - Initial Contribution
 */
@NonNullByDefault
public interface TlsProvider {

    /**
     * Host name for which this tls-provider is intended.
     *
     * It can either be matched on common-name (from the certificate) or peer-host / peer-port based on the actual
     * ssl-connection. Both options can be used without further configuration.
     *
     * @return a host name in string format, eg: www.eclipse.org (based on certificate common-name) or
     *         www.eclipse.org:443 (based on peer host/port)
     */
    String getHostName();
}
