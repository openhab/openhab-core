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
package org.openhab.core.io.transport.serial.internal;

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
 *
 * @author Massimo Valla - Initial contribution
 *
 */
@NonNullByDefault
@Component(service = SerialPortProvider.class)
public class JSerialCommPortProvider implements SerialPortProvider {

    private final Logger logger = LoggerFactory.getLogger(JSerialCommPortProvider.class);

    @Override
    public @Nullable SerialPortIdentifier getPortIdentifier(URI port) {
        String portPathAsString = port.getPath();
        try {
            SerialPort spFound = SerialPort.getCommPort(portPathAsString);
            return new SerialPortIdentifierImpl(spFound);
        } catch (SerialPortInvalidPortException e) {
            logger.debug("jSerialComm --- No SerialPortr found for: {}", portPathAsString, e);
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
        logger.debug("jSerialComm --- Found ports: " + portsArray);

        Stream<com.fazecast.jSerialComm.SerialPort> ports = Arrays.stream(portsArray);

        return ports.map(sid -> new SerialPortIdentifierImpl(sid));
    }
}
