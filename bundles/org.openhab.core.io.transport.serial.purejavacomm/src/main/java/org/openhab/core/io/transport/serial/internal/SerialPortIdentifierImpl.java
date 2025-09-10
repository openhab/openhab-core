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
package org.openhab.core.io.transport.serial.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;

import purejavacomm.CommPortIdentifier;

/**
 * Specific serial port identifier implementation.
 *
 * @author Łukasz Dywicki - Initial contribution
 * @author Karel Goderis - added further methods
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
            return new SerialPortImpl((purejavacomm.SerialPort) id.open(owner, timeout));
        } catch (purejavacomm.PortInUseException e) {
            String message = e.getMessage();
            if (message != null) {
                throw new PortInUseException(message, e);
            } else {
                throw new PortInUseException(e);
            }
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

    public String toString() {
        return getName() + " (pure java comm)";
    }
}
