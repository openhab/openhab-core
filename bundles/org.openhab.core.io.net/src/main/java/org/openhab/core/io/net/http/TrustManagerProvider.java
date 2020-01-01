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
package org.openhab.core.io.net.http;

import java.util.stream.Stream;

import javax.net.ssl.TrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Service to get custom trust managers for a given endpoint
 *
 * @author Michael Bock - Initial contribution
 */
@NonNullByDefault
@Deprecated
public interface TrustManagerProvider {

    /**
     * Provides a (potentially empty) list of trust managers to be used for an endpoint.
     * If the list is empty, the default java trust managers should be used.
     *
     * @param endpoint the desired endpoint, protocol and host are sufficient
     * @return a (potentially empty) list of trust managers
     */
    Stream<TrustManager> getTrustManagers(String endpoint);
}
