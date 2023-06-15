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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.openhab.core.config.discovery.usbserial.linuxsysfs.internal.PollingUsbSerialScanner.PAUSE_BETWEEN_SCANS_IN_SECONDS_ATTRIBUTE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.openhab.core.config.discovery.usbserial.linuxsysfs.testutil.UsbSerialDeviceInformationGenerator;

/**
 * Unit tests for the {@link PollingUsbSerialScanner}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class PollingUsbSerialScannerTest {

    private UsbSerialDeviceInformationGenerator usbDeviceInfoGenerator = new UsbSerialDeviceInformationGenerator();

    private @NonNullByDefault({}) PollingUsbSerialScanner pollingScanner;

    private @Mock @NonNullByDefault({}) UsbSerialDiscoveryListener discoveryListenerMock;
    private @Mock @NonNullByDefault({}) UsbSerialScanner usbSerialScannerMock;

    @BeforeEach
    public void beforeEach() {
        Map<String, Object> config = new HashMap<>();
        config.put(PAUSE_BETWEEN_SCANS_IN_SECONDS_ATTRIBUTE, "1");

        pollingScanner = new PollingUsbSerialScanner(config, usbSerialScannerMock);

        pollingScanner.registerDiscoveryListener(discoveryListenerMock);
    }

    @Test
    public void testNoScansWithoutBackgroundDiscovery() throws IOException, InterruptedException {
        // Wait a little more than one second to give background scanning a chance to kick in.
        Thread.sleep(1200);

        verify(usbSerialScannerMock, never()).scan();
    }

    @Test
    public void testSingleScanReportsResultsCorrectAfterOneScan() throws IOException {
        UsbSerialDeviceInformation usb1 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb2 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb3 = usbDeviceInfoGenerator.generate();

        when(usbSerialScannerMock.scan()).thenReturn(Set.of(usb1, usb2));
        when(usbSerialScannerMock.canPerformScans()).thenReturn(true);

        pollingScanner.doSingleScan();

        // Expectation: discovery listener called with newly discovered devices usb1 and usb2; not called with removed
        // devices.

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(usb3);

        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(any(UsbSerialDeviceInformation.class));
    }

    @Test
    public void testSingleScanReportsResultsCorrectlyAfterTwoScans() throws IOException {
        UsbSerialDeviceInformation usb1 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb2 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb3 = usbDeviceInfoGenerator.generate();

        when(usbSerialScannerMock.scan()).thenReturn(Set.of(usb1, usb2)).thenReturn(Set.of(usb2, usb3));
        when(usbSerialScannerMock.canPerformScans()).thenReturn(true);

        pollingScanner.unregisterDiscoveryListener(discoveryListenerMock);
        pollingScanner.doSingleScan();

        pollingScanner.registerDiscoveryListener(discoveryListenerMock);
        pollingScanner.doSingleScan();

        // Expectation: discovery listener called once for adding usb1 and usb2 (on registration)
        // then once for removing usb1, and once again for adding usb2 (another registration)
        // and once for usb3

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListenerMock, times(1)).usbSerialDeviceRemoved(usb1);

        verify(discoveryListenerMock, times(2)).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb2);

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb3);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb3);
    }

    @Test
    public void testBackgroundScanning() throws IOException, InterruptedException {
        UsbSerialDeviceInformation usb1 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb2 = usbDeviceInfoGenerator.generate();
        UsbSerialDeviceInformation usb3 = usbDeviceInfoGenerator.generate();

        when(usbSerialScannerMock.scan()).thenReturn(Set.of(usb1, usb2)).thenReturn(Set.of(usb2, usb3));
        when(usbSerialScannerMock.canPerformScans()).thenReturn(true);

        pollingScanner.startBackgroundScanning();

        Thread.sleep(1500);

        pollingScanner.stopBackgroundScanning();

        // Expectation: discovery listener called once for each discovered device, and once for removal of usb1.

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListenerMock, times(1)).usbSerialDeviceRemoved(usb1);

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb2);

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb3);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb3);
    }

    @Test
    public void testNoBackgroundScanningWhenNoScansPossible() throws IOException, InterruptedException {
        when(usbSerialScannerMock.scan()).thenReturn(Set.of(usbDeviceInfoGenerator.generate()));
        when(usbSerialScannerMock.canPerformScans()).thenReturn(false);

        pollingScanner.startBackgroundScanning();

        Thread.sleep(1500);

        pollingScanner.stopBackgroundScanning();

        // Expectation: discovery listener never called, as usbSerialScanner indicates that no scans possible

        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(any(UsbSerialDeviceInformation.class));
    }
}
