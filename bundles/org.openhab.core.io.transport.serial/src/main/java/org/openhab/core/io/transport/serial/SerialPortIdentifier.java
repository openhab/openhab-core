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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface of a serial port identifier.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public interface SerialPortIdentifier {
    /**
     * Gets the name of the port.
     *
     * @return the port's name
     */
    String getName();

    /**
     * Opens a serial port for communicating.
     *
     * @param owner name of the owner that port should be assigned to
     * @param timeout time in milliseconds to block waiting for opening the port
     * @return a serial port
     * @throws PortInUseException thrown when the serial port is already in use
     */
    SerialPort open(String owner, int timeout) throws PortInUseException;

    /**
     * Determines whether the associated port is in use by an application (including this application).
     *
     * @return true if an application is using the port, false if the port is not currently owned.
     */
    boolean isCurrentlyOwned();

    /**
     * Returns a textual representation of the current owner of the port. An owner is an application which is currently
     * using the port (in the sense that it opened the port and has not closed it yet).
     *
     * To check if a port is owned use the <code>isCurrentlyOwned</code> method. Do not rely on this method to return
     * null. It can't be guaranteed that owned ports have a non null owner.
     *
     * @return the port owner or null if the port is not currently owned
     */
    @Nullable
    String getCurrentOwner();
}
