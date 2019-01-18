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
package org.eclipse.smarthome.config.discovery.usbserial.linuxsysfs.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.usbserial.UsbSerialDeviceInformation;

/**
 * Permits to perform repeated scans for USB devices with associated serial port. Keeps the last scan result as internal
 * state, for detecting which devices were added, as well as which devices were removed.
 *
 * @author Henning Sudbrock - initial contribution
 */
@NonNullByDefault
public class DeltaUsbSerialScanner {

    private Set<UsbSerialDeviceInformation> lastScanResult = new HashSet<>();

    private final UsbSerialScanner usbSerialScanner;

    public DeltaUsbSerialScanner(UsbSerialScanner usbSerialScanner) {
        this.usbSerialScanner = usbSerialScanner;
    }

    /**
     * Scans for USB-Serial devices, and returns the delta to the last scan result.
     * <p/>
     * This method is synchronized to prevent multiple parallel invocations of this method that could bring the value of
     * lastScanResult into an inconsistent state.
     *
     * @return The delta to the last scan result.
     * @throws IOException if the scan using the {@link UsbSerialScanner} throws an IOException.
     */
    public synchronized Delta<UsbSerialDeviceInformation> scan() throws IOException {
        Set<UsbSerialDeviceInformation> scanResult = usbSerialScanner.scan();

        Set<UsbSerialDeviceInformation> added = setDifference(scanResult, lastScanResult);
        Set<UsbSerialDeviceInformation> removed = setDifference(lastScanResult, scanResult);
        Set<UsbSerialDeviceInformation> unchanged = setDifference(scanResult, added);

        lastScanResult = scanResult;

        return new Delta<>(added, removed, unchanged);
    }

    /**
     * @return <code>true</code> if the used {@link UsbSerialScanner} can perform scans on this system,
     *         <code>false</code> otherwise.
     */
    public boolean canPerformScans() {
        return usbSerialScanner.canPerformScans();
    }

    private <T> Set<T> setDifference(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.removeAll(set2);
        return Collections.unmodifiableSet(result);
    }

    /**
     * Delta between two subsequent scan results.
     */
    class Delta<T> {

        private final Set<T> added;
        private final Set<T> removed;
        private final Set<T> unchanged;

        public Delta(Set<T> added, Set<T> removed, Set<T> unchanged) {
            this.added = added;
            this.removed = removed;
            this.unchanged = unchanged;
        }

        public Set<T> getAdded() {
            return added;
        }

        public Set<T> getRemoved() {
            return removed;
        }

        public Set<T> getUnchanged() {
            return unchanged;
        }
    }

}
