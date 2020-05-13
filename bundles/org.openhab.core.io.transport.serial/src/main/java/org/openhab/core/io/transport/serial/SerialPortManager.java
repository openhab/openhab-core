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
package org.openhab.core.io.transport.serial;

import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface for a serial port manager.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public interface SerialPortManager {
    /**
     * Gets an serial port identifier for a given name.
     *
     * @param name the name
     * @return a serial port identifier or null
     */
    default @Nullable SerialPortIdentifier getIdentifier(final String name) {
        final Optional<SerialPortIdentifier> opt = getIdentifiers().filter(id -> id.getName().equals(name)).findFirst();
        if (opt.isPresent()) {
            return opt.get();
        } else {
            return null;
        }
    }

    /**
     * Gets the discovered serial port identifiers.
     *
     * {@link SerialPortProvider}s may not be able to discover any or all identifiers.
     * When the port name is known, the preferred way to get an identifier is by using {@link #getIdentifier(String)}.
     *
     * @return stream of discovered serial port identifiers
     */
    Stream<SerialPortIdentifier> getIdentifiers();
}
