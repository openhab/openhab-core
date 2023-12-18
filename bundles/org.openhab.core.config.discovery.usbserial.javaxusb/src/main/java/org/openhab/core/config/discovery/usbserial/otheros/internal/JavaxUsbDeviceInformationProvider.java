/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.usbserial.otheros.internal;

import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;

/**
 * This is an information provider that provides additional information about known products. It is required because in
 * some cases the javax.usb USB scanner gets null values for manufacturer and product description strings, and this
 * provider can try to fill in null values as a fallback.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class JavaxUsbDeviceInformationProvider {

    // @formatter:off
    private static final Set<UsbSerialDeviceInformation> KNOWN_DEVICES = Set.of(
        /*
         * ==== EnOcean sticks ====
         */
        // the following generic is used in EnOcean sticks => EXCLUDE from being discovered
        new UsbSerialDeviceInformation(0x0403, 0x6001, null, "Future Technology Devices", "GENERIC", 0, null, ""),

        /*
         * ==== Zigbee sticks ====
         */
        new UsbSerialDeviceInformation(0x0403, 0x8A28, null, "Future Technology Devices", "Rainforest Automation ZigBee", 0, null, ""),
        new UsbSerialDeviceInformation(0x0451, 0x16A8, null, "Texas Instruments", "ZigBee", 0, null, ""),
        new UsbSerialDeviceInformation(0x10C4, 0x89FB, null, "Silicon Laboratories", "ZigBee", 0, null, ""),
        new UsbSerialDeviceInformation(0x1CF1, 0x0030, null, "Dresden Elektronik", "ZigBee", 0, null, ""),

        /*
         * ==== Z-Wave sticks ====
         */
        new UsbSerialDeviceInformation(0x0658, 0x0200, null, "Sigma Designs", "Aeotec / ZWave.me UZB Z-Wave", 0, null, ""),
        new UsbSerialDeviceInformation(0x1A86, 0x55D4, null, "Nanjing Qinheng Microelectronics", "Zooz 800 Z-Wave", 0, null, ""),

        /*
         * ==== Zigbee sticks || Z-Wave sticks ====
         */
        // the following generic very common => EXCLUDE from being discovered
        new UsbSerialDeviceInformation(0x10C4, 0xEA60, null, "Silicon Laboratories", "Aeon Labs / Zooz 700 / sonoff / GENERIC", 0, null, "")
    );
    // @formatter:on

    public static Optional<UsbSerialDeviceInformation> getDeviceInformation(int vendorId, int productId) {
        return KNOWN_DEVICES.stream().filter(p -> (vendorId == p.getVendorId()) && (productId == p.getProductId()))
                .findFirst();
    }
}
