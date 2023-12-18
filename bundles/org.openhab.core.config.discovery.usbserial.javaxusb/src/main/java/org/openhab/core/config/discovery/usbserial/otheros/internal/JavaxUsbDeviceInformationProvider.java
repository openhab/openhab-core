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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private final List<UsbSerialDeviceInformation> infos = new ArrayList<>();

    public JavaxUsbDeviceInformationProvider() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("usb-device-infos.txt");
        if (stream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            try {
                while (reader.ready()) {
                    String[] data = reader.readLine().split(",");
                    if (data.length > 3) {
                        infos.add(new UsbSerialDeviceInformation(Integer.decode(data[0]), Integer.decode(data[1]), null,
                                data[2], data[3], 0, null, ""));
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    public Optional<UsbSerialDeviceInformation> getDeviceInfo(int vendorId, int productId) {
        return infos.stream().filter(p -> (vendorId == p.getVendorId()) && (productId == p.getProductId())).findAny();
    }
}
