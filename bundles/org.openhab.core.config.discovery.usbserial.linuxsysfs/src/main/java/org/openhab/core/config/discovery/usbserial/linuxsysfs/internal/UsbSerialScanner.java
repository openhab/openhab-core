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
package org.openhab.core.config.discovery.usbserial.linuxsysfs.internal;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;

/**
 * Implementations of this interface scan for serial ports provided by USB devices.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@NonNullByDefault
public interface UsbSerialScanner {

    /**
     * Performs a single scan for serial ports provided by USB devices.
     *
     * @return A collection containing all scan results.
     * @throws IOException if an I/O issue prevented the scan. Note that implementors are free to swallow I/O issues
     *             that occur when trying to read the information about a single USB device or serial port, so that
     *             information about other devices can still be retrieved. (Such issues should nevertheless be logged by
     *             implementors.)
     */
    Set<UsbSerialDeviceInformation> scan() throws IOException;

    /**
     * {@link UsbSerialScanner}s might be able to perform scans only on certain platforms, or with proper configuration.
     * {@link UsbSerialScanner}s can indicate whether they are able to perform scans using this method.
     *
     * @return <code>true</code> if able to perform scans, and <code>false</code> otherwise.
     */
    boolean canPerformScans();
}
