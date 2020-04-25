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
package org.openhab.core.io.transport.serial.internal;

import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;

/**
 *
 * @author Matthias Steigenberger - Initial contribution
 * @author Wouter Born - Fix serial ports missing when ports are added to system property
 */
@NonNullByDefault
public class SerialPortUtil {

    private static final String GNU_IO_RXTX_SERIAL_PORTS = "gnu.io.rxtx.SerialPorts";

    private static synchronized boolean isSerialPortsKeySet() {
        return System.getProperties().containsKey(GNU_IO_RXTX_SERIAL_PORTS);
    }

    public static synchronized CommPortIdentifier getPortIdentifier(String port) throws NoSuchPortException {
        if ((System.getProperty("os.name").toLowerCase().indexOf("linux") != -1)) {
            appendSerialPortProperty(port);
        }

        return CommPortIdentifier.getPortIdentifier(port);
    }

    /**
     * Registers the given port as system property {@value #GNU_IO_RXTX_SERIAL_PORTS}.
     * The method is capable of extending the system property, if any other ports are already registered.
     *
     * @param port the port to be registered
     */
    private static synchronized void appendSerialPortProperty(String port) {
        String serialPortsProperty = System.getProperty(GNU_IO_RXTX_SERIAL_PORTS);
        String newValue = initSerialPort(port, serialPortsProperty);
        if (newValue != null) {
            System.setProperty(GNU_IO_RXTX_SERIAL_PORTS, newValue);
        }
    }

    /**
     * Scans for available port identifiers by calling RXTX without using the ({@value #GNU_IO_RXTX_SERIAL_PORTS}
     * property. Finds port identifiers based on operating system and distribution.
     *
     * @return the scanned port identifiers
     */
    @SuppressWarnings("unchecked")
    public static synchronized Stream<CommPortIdentifier> getPortIdentifiersUsingScan() {
        Enumeration<CommPortIdentifier> identifiers;
        if (isSerialPortsKeySet()) {
            // Save the existing serial ports property
            String value = System.getProperty(GNU_IO_RXTX_SERIAL_PORTS);
            // Clear the property so library scans the ports
            System.clearProperty(GNU_IO_RXTX_SERIAL_PORTS);
            identifiers = CommPortIdentifier.getPortIdentifiers();
            // Restore the existing serial ports property
            System.setProperty(GNU_IO_RXTX_SERIAL_PORTS, value);
        } else {
            identifiers = CommPortIdentifier.getPortIdentifiers();
        }

        // Save the Enumeration to a new list so the result is thread safe
        return Collections.list(identifiers).stream();
    }

    /**
     * Get the port identifiers for the ports in the system property by calling RXTX while using the
     * ({@value #GNU_IO_RXTX_SERIAL_PORTS} property.
     *
     * @return the port identifiers for the ports defined in the {@value #GNU_IO_RXTX_SERIAL_PORTS} property
     */
    @SuppressWarnings("unchecked")
    public static synchronized Stream<CommPortIdentifier> getPortIdentifiersUsingProperty() {
        if (isSerialPortsKeySet()) {
            // Save the Enumeration to a new list so the result is thread safe
            return Collections.list(CommPortIdentifier.getPortIdentifiers()).stream();
        }

        return Stream.empty();
    }

    static @Nullable String initSerialPort(String port, @Nullable String serialPortsProperty) {
        String pathSeparator = File.pathSeparator;
        Set<String> serialPorts = null;
        if (serialPortsProperty != null) {
            serialPorts = Stream.of(serialPortsProperty.split(pathSeparator)).collect(Collectors.toSet());
        } else {
            serialPorts = new HashSet<>();
        }
        if (serialPorts.add(port)) {
            return serialPorts.stream().collect(Collectors.joining(pathSeparator)); // see
                                                                                    // RXTXCommDriver#addSpecifiedPorts
        }
        return null;
    }
}
