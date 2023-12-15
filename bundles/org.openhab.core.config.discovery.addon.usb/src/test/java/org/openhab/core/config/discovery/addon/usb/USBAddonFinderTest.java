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
package org.openhab.core.config.discovery.addon.usb;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;

/**
 * This contains Junit tests for the {@link USBAddonFinder} class.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
class USBAddonFinderTest {

    @Test
    void test() {
        USBAddonFinder finder = new USBAddonFinder();
        finder.getSuggestedAddons();
        for (UsbSerialDeviceInformation deviceInfo : finder.getDeviceInformations()) {
            // System.out.println(deviceInfo.toString());
        }
    }
}
