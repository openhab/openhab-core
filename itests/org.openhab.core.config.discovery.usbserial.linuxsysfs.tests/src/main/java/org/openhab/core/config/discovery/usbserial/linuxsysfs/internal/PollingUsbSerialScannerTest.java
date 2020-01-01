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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.openhab.core.config.discovery.usbserial.linuxsysfs.internal.PollingUsbSerialScanner.PAUSE_BETWEEN_SCANS_IN_SECONDS_ATTRIBUTE;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.openhab.core.config.discovery.usbserial.linuxsysfs.testutil.UsbSerialDeviceInformationGenerator;

/**
 * Unit tests for the {@link PollingUsbSerialScanner}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
public class PollingUsbSerialScannerTest {

    private UsbSerialDeviceInformationGenerator usbDeviceInfoGenerator = new UsbSerialDeviceInformationGenerator();

    private UsbSerialScanner usbSerialScanner;
    private PollingUsbSerialScanner pollingScanner;
    private UsbSerialDiscoveryListener discoveryListener;

    @Before
    public void setup() {
        usbSerialScanner = mock(UsbSerialScanner.class);
        pollingScanner = new PollingUsbSerialScanner();
        pollingScanner.setUsbSerialScanner(usbSerialScanner);

        discoveryListener = mock(UsbSerialDiscoveryListener.class);
        pollingScanner.registerDiscoveryListener(discoveryListener);

        Map<String, Object> config = new HashMap<>();
        config.put(PAUSE_BETWEEN_SCANS_IN_SECONDS_ATTRIBUTE, "1");
        pollingScanner.modified(config);
    }

    @Test
    public void testNoScansWithoutBackgroundDiscovery() throws IOException, InterruptedException {
        // Wait a little more than one second to give background scanning a chance to kick in.
        Thread.sleep(1200);

        verify(usbSerialScanner, never()).scan();
    }

    @Test
    public void testSingleScanReportsResultsCorrectAfterOneScan() throws IOException {
        UsbSerialDeviceInformation usb1 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb2 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb3 = usbDeviceInfoGenerator.generate();

        when(usbSerialScanner.scan()).thenReturn(new HashSet<>(asList(usb1, usb2)));
        when(usbSerialScanner.canPerformScans()).thenReturn(true);

        pollingScanner.doSingleScan();

        // Expectation: discovery listener called with newly discovered devices usb1 and usb2; not called with removed
        // devices.

        verify(discoveryListener, times(1)).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListener, times(1)).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListener, never()).usbSerialDeviceDiscovered(usb3);

        verify(discoveryListener, never()).usbSerialDeviceRemoved(any(UsbSerialDeviceInformation.class));
    }

    @Test
    public void testSingleScanReportsResultsCorrectlyAfterTwoScans() throws IOException {
        UsbSerialDeviceInformation usb1 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb2 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb3 = usbDeviceInfoGenerator.generate();

        when(usbSerialScanner.scan()).thenReturn(new HashSet<>(asList(usb1, usb2)))
                .thenReturn(new HashSet<>(asList(usb2, usb3)));
        when(usbSerialScanner.canPerformScans()).thenReturn(true);

        pollingScanner.unregisterDiscoveryListener(discoveryListener);
        pollingScanner.doSingleScan();

        pollingScanner.registerDiscoveryListener(discoveryListener);
        pollingScanner.doSingleScan();

        // Expectation: discovery listener called once for removing usb1, and once for adding usb2/usb3 each.

        verify(discoveryListener, never()).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListener, times(1)).usbSerialDeviceRemoved(usb1);

        verify(discoveryListener, times(1)).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListener, never()).usbSerialDeviceRemoved(usb2);

        verify(discoveryListener, times(1)).usbSerialDeviceDiscovered(usb3);
        verify(discoveryListener, never()).usbSerialDeviceRemoved(usb3);
    }

    @Test
    public void testBackgroundScanning() throws IOException, InterruptedException {
        UsbSerialDeviceInformation usb1 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb2 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb3 = usbDeviceInfoGenerator.generate();

        when(usbSerialScanner.scan()).thenReturn(new HashSet<>(asList(usb1, usb2)))
                .thenReturn(new HashSet<>(asList(usb2, usb3)));
        when(usbSerialScanner.canPerformScans()).thenReturn(true);

        pollingScanner.activate(new HashMap<>());
        pollingScanner.startBackgroundScanning();

        Thread.sleep(1500);

        pollingScanner.stopBackgroundScanning();

        // Expectation: discovery listener called once for each discovered device, and once for removal of usb1.

        verify(discoveryListener, times(1)).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListener, times(1)).usbSerialDeviceRemoved(usb1);

        verify(discoveryListener, times(1)).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListener, never()).usbSerialDeviceRemoved(usb2);

        verify(discoveryListener, times(1)).usbSerialDeviceDiscovered(usb3);
        verify(discoveryListener, never()).usbSerialDeviceRemoved(usb3);
    }

    @Test
    public void testNoBackgroundScanningWhenNoScansPossible() throws IOException, InterruptedException {
        when(usbSerialScanner.scan()).thenReturn(new HashSet<>(asList(usbDeviceInfoGenerator.generate())));
        when(usbSerialScanner.canPerformScans()).thenReturn(false);

        pollingScanner.startBackgroundScanning();

        Thread.sleep(1500);

        pollingScanner.stopBackgroundScanning();

        // Expectation: discovery listener never called, as usbSerialScanner indicates that no scans possible

        verify(discoveryListener, never()).usbSerialDeviceDiscovered(any(UsbSerialDeviceInformation.class));
    }

}
