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
package org.eclipse.smarthome.config.discovery.usbserial.testutil;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.smarthome.config.discovery.usbserial.UsbSerialDeviceInformation;

/**
 * Generate simple instances of {@link UsbSerialDeviceInformation} that can be used in tests.
 *
 * @author Henning Sudbrock - initial contribution
 */
public class UsbSerialDeviceInformationGenerator {

    private final AtomicInteger counter = new AtomicInteger(0);

    public UsbSerialDeviceInformation generate() {
        int i = counter.getAndIncrement();
        return new UsbSerialDeviceInformation(i, i, "serialNumber-" + i, "manufacturer-" + i, "product-" + i, i,
                "interface-" + i, "ttyUSB" + i);
    }

}
