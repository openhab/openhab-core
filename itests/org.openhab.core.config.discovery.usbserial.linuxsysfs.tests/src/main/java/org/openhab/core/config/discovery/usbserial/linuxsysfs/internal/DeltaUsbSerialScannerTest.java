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

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.linuxsysfs.internal.DeltaUsbSerialScanner.Delta;
import org.openhab.core.config.discovery.usbserial.linuxsysfs.testutil.UsbSerialDeviceInformationGenerator;

/**
 * Unit tests for the {@link DeltaUsbSerialScanner}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
public class DeltaUsbSerialScannerTest {

    private UsbSerialDeviceInformationGenerator usbDeviceInfoGenerator = new UsbSerialDeviceInformationGenerator();

    private UsbSerialScanner usbSerialScanner;
    private DeltaUsbSerialScanner deltaUsbSerialScanner;

    @Before
    public void setup() {
        usbSerialScanner = mock(UsbSerialScanner.class);
        deltaUsbSerialScanner = new DeltaUsbSerialScanner(usbSerialScanner);
    }

    /**
     * If there are no devices discovered in a first scan, then there is no delta.
     */
    @Test
    public void testInitialEmptyResult() throws IOException {
        when(usbSerialScanner.scan()).thenReturn(emptySet());

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
        when(usbSerialScanner.scan()).thenReturn(new HashSet<>(asList(usb1, usb2)));

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

        when(usbSerialScanner.scan()).thenReturn(new HashSet<>(asList(usb1, usb2)));
        deltaUsbSerialScanner.scan();

        when(usbSerialScanner.scan()).thenReturn(new HashSet<>(asList(usb2, usb3)));
        Delta<UsbSerialDeviceInformation> delta = deltaUsbSerialScanner.scan();

        assertThat(delta.getAdded(), contains(usb3));
        assertThat(delta.getRemoved(), contains(usb1));
        assertThat(delta.getUnchanged(), contains(usb2));
    }

}
