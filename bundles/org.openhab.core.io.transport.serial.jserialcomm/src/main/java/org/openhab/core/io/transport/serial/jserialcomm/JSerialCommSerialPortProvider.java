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

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.ProtocolType;
import org.openhab.core.io.transport.serial.ProtocolType.PathType;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

/**
 * Specific OH serial transport SerialPortProvider implementation using com.fazecast.jSerialComm.SerialPort
 *
 * @author Massimo Valla - Initial contribution
 */
@NonNullByDefault
@Component(service = SerialPortProvider.class)
public class JSerialCommSerialPortProvider implements SerialPortProvider {

    private final Logger logger = LoggerFactory.getLogger(JSerialCommSerialPortProvider.class);

    @Override
    public @Nullable SerialPortIdentifier getPortIdentifier(URI port) {
        String portPathAsString = port.getPath();
        try {
            SerialPort spFound = SerialPort.getCommPort(portPathAsString);
            return new JSerialCommSerialPortIdentifier(spFound);
        } catch (SerialPortInvalidPortException e) {
            logger.debug(
                    "--------TRANSPORT-jSerialComm--- No SerialPort found for: {} (SerialPortInvalidPortException: {})",
                    portPathAsString, e.getMessage());
            return null;
        }
    }

    @Override
    public Stream<ProtocolType> getAcceptedProtocols() {
        return Stream.of(new ProtocolType(PathType.LOCAL, "jserialcomm"));
    }

    @Override
    public Stream<SerialPortIdentifier> getSerialPortIdentifiers() {
        com.fazecast.jSerialComm.SerialPort[] portsArray = com.fazecast.jSerialComm.SerialPort.getCommPorts();
        logger.debug("--------TRANSPORT-jSerialComm--- getSerialPortIdentifiers() :: Listing found ports:");
        for (com.fazecast.jSerialComm.SerialPort port : portsArray) {
            logger.debug("--------TRANSPORT-jSerialComm---     port: {} ({} - {})", port.getSystemPortName(),
                    port.getSystemPortPath(), port.getPortDescription());
        }
        logger.debug("--------TRANSPORT-jSerialComm--- ...finished listing found ports.");

        Stream<com.fazecast.jSerialComm.SerialPort> ports = Arrays.stream(portsArray);

        return ports.map(sid -> new JSerialCommSerialPortIdentifier(sid));
    }
}
