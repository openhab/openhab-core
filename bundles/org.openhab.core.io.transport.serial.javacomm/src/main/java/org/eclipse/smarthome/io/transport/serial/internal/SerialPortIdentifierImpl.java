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
package org.eclipse.smarthome.io.transport.serial.internal;

import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.transport.serial.PortInUseException;
import org.eclipse.smarthome.io.transport.serial.SerialPort;
import org.eclipse.smarthome.io.transport.serial.SerialPortIdentifier;

/**
 * Specific serial port identifier implementation.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class SerialPortIdentifierImpl implements SerialPortIdentifier {

    final CommPortIdentifier id;

    /**
     * Constructor.
     *
     * @param id the underlying comm port identifier implementation
     */
    public SerialPortIdentifierImpl(final CommPortIdentifier id) {
        this.id = id;
    }

    @Override
    public String getName() {
        final String name = id.getName();
        return name != null ? name : "";
    }

    @Override
    public SerialPort open(String owner, int timeout) throws PortInUseException {
        try {
            final CommPort cp = id.open(owner, timeout);
            if (cp instanceof javax.comm.SerialPort) {
                return new SerialPortImpl((javax.comm.SerialPort) cp);
            } else {
                throw new IllegalStateException(
                        String.format("We expect an serial port instead of '%s'", cp.getClass()));
            }
        } catch (javax.comm.PortInUseException e) {
            throw new PortInUseException();
        }
    }

    @Override
    public boolean isCurrentlyOwned() {
        return id.isCurrentlyOwned();
    }

    @Override
    public @Nullable String getCurrentOwner() {
        return id.getCurrentOwner();
    }
}
