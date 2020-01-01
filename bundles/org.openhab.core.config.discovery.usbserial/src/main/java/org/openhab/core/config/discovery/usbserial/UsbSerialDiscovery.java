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
package org.openhab.core.config.discovery.usbserial;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.usbserial.internal.UsbSerialDiscoveryService;

/**
 * Interface for implementations for discovering serial ports provided by a USB device. An implementation of this
 * interface is required by the {@link UsbSerialDiscoveryService}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@NonNullByDefault
public interface UsbSerialDiscovery {

    /**
     * Executes a single scan for serial ports provided by USB devices; informs listeners about all discovered devices
     * (including those discovered in a previous scan).
     */
    void doSingleScan();

    /**
     * Starts scanning for serial ports provided by USB devices in the background; informs listeners about newly
     * discovered devices. Should return fast.
     */
    void startBackgroundScanning();

    /**
     * Stops scanning for serial ports provided by USB devices in the background. Should return fast.
     */
    void stopBackgroundScanning();

    /**
     * Registers an {@link UsbSerialDiscoveryListener} that is then notified about discovered serial ports and USB
     * devices.
     */
    void registerDiscoveryListener(UsbSerialDiscoveryListener listener);

    /**
     * Unregisters an {@link UsbSerialDiscoveryListener}.
     */
    void unregisterDiscoveryListener(UsbSerialDiscoveryListener listener);

}
