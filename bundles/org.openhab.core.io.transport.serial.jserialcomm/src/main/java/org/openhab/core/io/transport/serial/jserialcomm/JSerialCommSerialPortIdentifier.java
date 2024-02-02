/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.io.transport.serial.jserialcomm;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific OH serial transport SerialPortIdentifier implementation using com.fazecast.jSerialComm.SerialPort
 *
 * @author Massimo Valla - Initial contribution
 */
@NonNullByDefault
public class JSerialCommSerialPortIdentifier implements SerialPortIdentifier {

    private final Logger logger = LoggerFactory.getLogger(JSerialCommSerialPortIdentifier.class);

    final com.fazecast.jSerialComm.SerialPort sp;

    /**
     * Constructor.
     *
     * @param sp
     */
    public JSerialCommSerialPortIdentifier(final com.fazecast.jSerialComm.SerialPort sp) {
        this.sp = sp;
    }

    @Override
    public String getName() {
        final String name = sp.getSystemPortName();
        return name != null ? name : "";
    }

    @Override
    public SerialPort open(String owner, int timeout) throws PortInUseException {
        logger.debug("--------TRANSPORT-jSerialComm--- SerialPort.getReadTimeout() = {}", sp.getReadTimeout());
        logger.debug("--------TRANSPORT-jSerialComm--- SerialPort.getPortDescription() = {}", sp.getPortDescription());
        boolean success = sp.openPort();
        if (success) {
            return new JSerialCommSerialPort(sp);
        } else {
            logger.error("--------TRANSPORT-jSerialComm--- Could not open port: {}", sp.getSystemPortName());
            throw new PortInUseException(new Exception("Could not open port: " + sp.getSystemPortName()));
        }
    }

    @Override
    public boolean isCurrentlyOwned() {
        return false;
    }

    @Override
    public @Nullable String getCurrentOwner() {
        // FIXME !!!!! placeholder implementation
        return "jserialcomm owner";
    }
}
