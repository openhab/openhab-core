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
package org.openhab.core.config.discovery.usbserial.javaxusb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.javaxusb.internal.JavaxUsbDeviceInformationProvider;

/**
 * JUnit tests for {@link JavaxUsbDeviceInformationProvider}.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
class JavaxUsbDiscoveryTests {

    @Test
    void testInformationProvider() {
        JavaxUsbDeviceInformationProvider provider = new JavaxUsbDeviceInformationProvider();

        Optional<UsbSerialDeviceInformation> optionalInfo = provider.getDeviceInfo(0, 0);
        assertFalse(optionalInfo.isPresent());

        optionalInfo = provider.getDeviceInfo(0xFFFF, 0x1111);
        assertTrue(optionalInfo.isPresent());
        UsbSerialDeviceInformation info = optionalInfo.get();
        assertEquals(0xFFFF, info.getVendorId());
        assertEquals(0x1111, info.getProductId());
        assertEquals("Test Manufacturer", info.getManufacturer());
        assertEquals("Test Product", info.getProduct());
    }
}
