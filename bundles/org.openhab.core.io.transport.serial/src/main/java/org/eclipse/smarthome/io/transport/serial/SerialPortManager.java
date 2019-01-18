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
     * Gets the serial port identifiers.
     *
     * @return stream of serial port identifiers
     */
    Stream<SerialPortIdentifier> getIdentifiers();
}
