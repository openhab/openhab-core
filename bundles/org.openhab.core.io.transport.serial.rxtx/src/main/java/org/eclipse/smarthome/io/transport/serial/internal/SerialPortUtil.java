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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * @author Matthias Steigenberger - Initial Contribution
 *
 */
@NonNullByDefault
public class SerialPortUtil {

    private static final String GNU_IO_RXTX_SERIAL_PORTS = "gnu.io.rxtx.SerialPorts";

    /**
     * Registers the given port as system property {@value #GNU_IO_RXTX_SERIAL_PORTS}. The method is capable of
     * extending the system property, if any other ports are already registered.
     *
     * @param port the port to be registered
     */
    public synchronized static void appendSerialPortProperty(String port) {
        String serialPortsProperty = System.getProperty(GNU_IO_RXTX_SERIAL_PORTS);
        String newValue = initSerialPort(port, serialPortsProperty);
        if (newValue != null) {
            System.setProperty(GNU_IO_RXTX_SERIAL_PORTS, newValue);
        }
    }

    static @Nullable String initSerialPort(String port, @Nullable String serialPortsProperty) {

        String pathSeparator = File.pathSeparator;
        Set<String> serialPorts = null;
        if (serialPortsProperty != null) {
            serialPorts = Stream.of(serialPortsProperty.split(pathSeparator)).collect(Collectors.toSet());
        } else {
            serialPorts = new HashSet<String>();
        }
        if (serialPorts.add(port)) {
            return serialPorts.stream().collect(Collectors.joining(pathSeparator)); // see
                                                                                    // RXTXCommDriver#addSpecifiedPorts
        }
        return null;
    }

}
