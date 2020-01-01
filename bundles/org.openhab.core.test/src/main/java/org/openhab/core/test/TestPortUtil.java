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
package org.openhab.core.test;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * {@link TestPortUtil} provides helper methods for working with ports in tests.
 *
 * @author Henning Treu - Initial contribution
 * @author Wouter Born - Increase reusability
 */
public final class TestPortUtil {

    private TestPortUtil() {
        // Hidden utility class constructor
    }

    /**
     * Returns a free TCP/IP port number on localhost.
     *
     * Heavily inspired from org.eclipse.jdt.launching.SocketUtil (to avoid a dependency to JDT just because of this).
     * Slightly improved with close() missing in JDT. And throws exception instead of returning -1.
     *
     * @return a free TCP/IP port number on localhost
     * @throws IllegalStateException if unable to find a free port
     */
    public static int findFreePort() {
        try (final ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (final IOException ex) {
            throw new IllegalStateException("Could not find a free TCP/IP port", ex);
        }
    }
}
