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
package org.openhab.core.config.discovery.usbserial.linuxsysfs.internal;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.linuxsysfs.internal.DeltaUsbSerialScanner.Delta;
import org.openhab.core.config.discovery.usbserial.linuxsysfs.testutil.UsbSerialDeviceInformationGenerator;

/**
 * Unit tests for the {@link DeltaUsbSerialScanner}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class DeltaUsbSerialScannerTest {

    private UsbSerialDeviceInformationGenerator usbDeviceInfoGenerator = new UsbSerialDeviceInformationGenerator();

    private @Mock @NonNullByDefault({}) UsbSerialScanner usbSerialScannerMock;
    private @NonNullByDefault({}) DeltaUsbSerialScanner deltaUsbSerialScanner;

    @BeforeEach
    public void setup() {
        deltaUsbSerialScanner = new DeltaUsbSerialScanner(usbSerialScannerMock);
    }

    /**
     * If there are no devices discovered in a first scan, then there is no delta.
     */
    @Test
    public void testInitialEmptyResult() throws IOException {
        when(usbSerialScannerMock.scan()).thenReturn(emptySet());

        Delta<UsbSerialDeviceInformation> delta = deltaUsbSerialScanner.scan();

        assertThat(delta.getAdded(), is(empty()));
        assertThat(delta.getRemoved(), is(empty()));
        assertThat(delta.getUnchanged(), is(empty()));
    }

    /**
     * If there are devices discovered in a first scan, then all devices are in the 'added' section of the delta.
     */
    @Test
    public void testInitialNonEmptyResult() throws IOException {
        UsbSerialDeviceInformation usb1 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb2 = usbDeviceInfoGenerator.generate();
        when(usbSerialScannerMock.scan()).thenReturn(Set.of(usb1, usb2));

        Delta<UsbSerialDeviceInformation> delta = deltaUsbSerialScanner.scan();

        assertThat(delta.getAdded(), containsInAnyOrder(usb1, usb2));
        assertThat(delta.getRemoved(), is(empty()));
        assertThat(delta.getUnchanged(), is(empty()));
    }

    /**
     * If a first scan discovers devices usb1 and usb2, and a second scan discovers devices usb2 and usb3, then the
     * delta for the second scan is: usb3 is added, usb1 is removed, and usb2 is unchanged.
     */
    @Test
    public void testDevicesAddedAndRemovedAndUnchanged() throws IOException {
        UsbSerialDeviceInformation usb1 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb2 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb3 = usbDeviceInfoGenerator.generate();

        when(usbSerialScannerMock.scan()).thenReturn(Set.of(usb1, usb2));
        deltaUsbSerialScanner.scan();

        when(usbSerialScannerMock.scan()).thenReturn(Set.of(usb2, usb3));
        Delta<UsbSerialDeviceInformation> delta = deltaUsbSerialScanner.scan();

        assertThat(delta.getAdded(), contains(usb3));
        assertThat(delta.getRemoved(), contains(usb1));
        assertThat(delta.getUnchanged(), contains(usb2));
    }
}
