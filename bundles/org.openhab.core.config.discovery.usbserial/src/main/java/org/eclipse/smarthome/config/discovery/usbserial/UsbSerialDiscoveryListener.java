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
package org.eclipse.smarthome.config.discovery.usbserial;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Listener interface for {@link UsbSerialDiscovery}s.
 *
 * @author Henning Sudbrock - initial contribution
 */
@NonNullByDefault
public interface UsbSerialDiscoveryListener {

    /**
     * Called when a new serial port provided by a USB device is discovered.
     */
    void usbSerialDeviceDiscovered(UsbSerialDeviceInformation usbSerialDeviceInformation);

    /**
     * Called when a serial port provided by a USB device has been removed.
     */
    void usbSerialDeviceRemoved(UsbSerialDeviceInformation usbSerialDeviceInformation);

}
