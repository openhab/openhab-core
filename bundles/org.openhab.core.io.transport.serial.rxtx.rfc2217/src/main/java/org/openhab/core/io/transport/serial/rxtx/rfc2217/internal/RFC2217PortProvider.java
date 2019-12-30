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
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.ProtocolType;
import org.openhab.core.io.transport.serial.ProtocolType.PathType;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortProvider;
import org.osgi.service.component.annotations.Component;

import gnu.io.rfc2217.TelnetSerialPort;

/**
 *
 * @author Matthias Steigenberger - Initial contribution
 */
@NonNullByDefault
@Component(service = SerialPortProvider.class)
public class RFC2217PortProvider implements SerialPortProvider {

    private static final String PROTOCOL = "rfc2217";

    @Override
    public @Nullable SerialPortIdentifier getPortIdentifier(URI portName) {
        TelnetSerialPort telnetSerialPort = new TelnetSerialPort();
        telnetSerialPort.setName(portName.toString());
        return new SerialPortIdentifierImpl(telnetSerialPort, portName);
    }

    @Override
    public Stream<ProtocolType> getAcceptedProtocols() {
        return Stream.of(new ProtocolType(PathType.NET, PROTOCOL));
    }

    @Override
    public Stream<SerialPortIdentifier> getSerialPortIdentifiers() {
        // TODO implement discovery here. https://github.com/eclipse/smarthome/pull/5560
        return Stream.empty();
    }

}
