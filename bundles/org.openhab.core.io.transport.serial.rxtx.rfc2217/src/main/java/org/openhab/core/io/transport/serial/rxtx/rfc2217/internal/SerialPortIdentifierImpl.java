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
package org.openhab.core.io.transport.serial.rxtx.rfc2217.internal;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.rxtx.RxTxSerialPort;

import gnu.io.rfc2217.TelnetSerialPort;

/**
 * Specific serial port identifier implementation for RFC2217.
 *
 * @author Matthias Steigenberger - Initial contribution
 */
@NonNullByDefault
public class SerialPortIdentifierImpl implements SerialPortIdentifier {

    final TelnetSerialPort id;
    private final URI uri;

    /**
     * Constructor.
     *
     * @param id the underlying comm port identifier implementation
     */
    public SerialPortIdentifierImpl(final TelnetSerialPort id, URI uri) {
        this.id = id;
        this.uri = uri;
    }

    @Override
    public String getName() {
        final String name = id.getName();
        return name != null ? name : "";
    }

    @Override
    public SerialPort open(String owner, int timeout) throws PortInUseException {
        try {
            id.getTelnetClient().setConnectTimeout(timeout);
            id.getTelnetClient().connect(uri.getHost(), uri.getPort());
            return new RxTxSerialPort(id);
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Unable to establish remote connection to serial port %s", uri), e);
        }
    }

    @Override
    public boolean isCurrentlyOwned() {
        // Check if the socket is not available for use, if true interpret as being owned.
        return !id.getTelnetClient().isAvailable();
    }

    @Override
    public @Nullable String getCurrentOwner() {
        // Unknown who owns a socket connection. Therefore return null.
        return null;
    }
}
