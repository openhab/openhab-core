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
package org.eclipse.smarthome.io.transport.serial;

import java.net.URI;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Provides a concrete SerialPort which can handle remote (e.g. via rfc2217) or local ports.
 *
 * @author Matthias Steigenberger - Initial Contribution
 *
 */
@NonNullByDefault
public interface SerialPortProvider {

    /**
     * Gets the {@link SerialPortIdentifier} if it is available or null otherwise.
     *
     * @param portName The ports name.
     * @return The created {@link SerialPort}.
     * @throws NoSuchPortException If the serial port does not exist.
     * @throws UnsupportedCommOperationException
     * @throws PortInUseException
     */
    public @Nullable SerialPortIdentifier getPortIdentifier(URI portName);

    /**
     * Gets all protocol types which this provider is able to create.
     *
     * @return The protocol type.
     */
    public Stream<ProtocolType> getAcceptedProtocols();

    /**
     * Gets all the available {@link SerialPortIdentifier}s for this {@link SerialPortProvider}.
     * Please note: Discovery is not available necessarily, hence the {@link #getPortIdentifier(URI)} must be used in
     * this case.
     *
     * @return The available ports
     */
    public Stream<SerialPortIdentifier> getSerialPortIdentifiers();
}
