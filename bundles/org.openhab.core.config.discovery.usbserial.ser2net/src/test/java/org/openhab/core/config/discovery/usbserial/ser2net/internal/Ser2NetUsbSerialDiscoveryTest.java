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
package org.openhab.core.config.discovery.usbserial.ser2net.internal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openhab.core.config.discovery.usbserial.ser2net.internal.Ser2NetUsbSerialDiscovery.*;

import java.io.IOException;
import java.time.Duration;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.openhab.core.io.transport.mdns.MDNSClient;

/**
 * Unit tests for the {@link Ser2NetUsbSerialDiscovery}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Ser2NetUsbSerialDiscoveryTest {

    private @Mock @NonNullByDefault({}) UsbSerialDiscoveryListener discoveryListenerMock;
    private @Mock @NonNullByDefault({}) MDNSClient mdnsClientMock;

    private @Mock @NonNullByDefault({}) ServiceInfo serviceInfo1Mock;
    private @Mock @NonNullByDefault({}) ServiceInfo serviceInfo2Mock;
    private @Mock @NonNullByDefault({}) ServiceInfo serviceInfo3Mock;
    private @Mock @NonNullByDefault({}) ServiceInfo invalidServiceInfoMock;

    private @Mock @NonNullByDefault({}) ServiceEvent serviceEvent1Mock;
    private @Mock @NonNullByDefault({}) ServiceEvent serviceEvent2Mock;
    private @Mock @NonNullByDefault({}) ServiceEvent serviceEvent3Mock;
    private @Mock @NonNullByDefault({}) ServiceEvent invalidServiceEventMock;

    private @NonNullByDefault({}) Ser2NetUsbSerialDiscovery discovery;

    private UsbSerialDeviceInformation usb1 = new UsbSerialDeviceInformation(0x100, 0x111, "serial1", "manufacturer1",
            "product1", 0x1, "interface1", "rfc2217://1.1.1.1:1000");
    private UsbSerialDeviceInformation usb2 = new UsbSerialDeviceInformation(0x200, 0x222, "serial2", "manufacturer2",
            "product2", 0x2, "interface2", "rfc2217://[0:0:0:0:0:ffff:0202:0202]:2222");
    private UsbSerialDeviceInformation usb3 = new UsbSerialDeviceInformation(0x300, 0x333, null, null, null, 0x3, null,
            "rfc2217://123.222.100.000:3030");

    @BeforeEach
    public void beforeEach() {
        discovery = new Ser2NetUsbSerialDiscovery(mdnsClientMock);
        discovery.registerDiscoveryListener(discoveryListenerMock);

        setupServiceInfo1Mock();
        setupServiceInfo2Mock();
        setupServiceInfo3Mock();
        setupInvalidServiceInfoMock();

        when(serviceEvent1Mock.getInfo()).thenReturn(serviceInfo1Mock);
        when(serviceEvent2Mock.getInfo()).thenReturn(serviceInfo2Mock);
        when(serviceEvent3Mock.getInfo()).thenReturn(serviceInfo3Mock);
        when(invalidServiceEventMock.getInfo()).thenReturn(invalidServiceInfoMock);
    }

    private void setupServiceInfo1Mock() {
        when(serviceInfo1Mock.getHostAddresses()).thenReturn(new String[] { "1.1.1.1" });
        when(serviceInfo1Mock.getPort()).thenReturn(1000);

        when(serviceInfo1Mock.getPropertyString(PROPERTY_VENDOR_ID)).thenReturn("0100");
        when(serviceInfo1Mock.getPropertyString(PROPERTY_PRODUCT_ID)).thenReturn("0111");
        when(serviceInfo1Mock.getPropertyString(PROPERTY_SERIAL_NUMBER)).thenReturn("serial1");
        when(serviceInfo1Mock.getPropertyString(PROPERTY_MANUFACTURER)).thenReturn("manufacturer1");
        when(serviceInfo1Mock.getPropertyString(PROPERTY_PRODUCT)).thenReturn("product1");
        when(serviceInfo1Mock.getPropertyString(PROPERTY_INTERFACE_NUMBER)).thenReturn("01");
        when(serviceInfo1Mock.getPropertyString(PROPERTY_INTERFACE)).thenReturn("interface1");

        when(serviceInfo1Mock.getPropertyString(PROPERTY_PROVIDER)).thenReturn(SER2NET);
        when(serviceInfo1Mock.getPropertyString(PROPERTY_DEVICE_TYPE)).thenReturn(SERIALUSB);
        when(serviceInfo1Mock.getPropertyString(PROPERTY_GENSIO_STACK)).thenReturn(TELNET_RFC2217_TCP);
    }

    private void setupServiceInfo2Mock() {
        when(serviceInfo2Mock.getHostAddresses()).thenReturn(new String[] { "[0:0:0:0:0:ffff:0202:0202]" });
        when(serviceInfo2Mock.getPort()).thenReturn(2222);

        when(serviceInfo2Mock.getPropertyString(PROPERTY_VENDOR_ID)).thenReturn("0200");
        when(serviceInfo2Mock.getPropertyString(PROPERTY_PRODUCT_ID)).thenReturn("0222");
        when(serviceInfo2Mock.getPropertyString(PROPERTY_SERIAL_NUMBER)).thenReturn("serial2");
        when(serviceInfo2Mock.getPropertyString(PROPERTY_MANUFACTURER)).thenReturn("manufacturer2");
        when(serviceInfo2Mock.getPropertyString(PROPERTY_PRODUCT)).thenReturn("product2");
        when(serviceInfo2Mock.getPropertyString(PROPERTY_INTERFACE_NUMBER)).thenReturn("02");
        when(serviceInfo2Mock.getPropertyString(PROPERTY_INTERFACE)).thenReturn("interface2");
    }

    private void setupServiceInfo3Mock() {
        when(serviceInfo3Mock.getHostAddresses()).thenReturn(new String[] { "123.222.100.000" });
        when(serviceInfo3Mock.getPort()).thenReturn(3030);

        when(serviceInfo3Mock.getPropertyString(PROPERTY_VENDOR_ID)).thenReturn("0300");
        when(serviceInfo3Mock.getPropertyString(PROPERTY_PRODUCT_ID)).thenReturn("0333");
        when(serviceInfo3Mock.getPropertyString(PROPERTY_INTERFACE_NUMBER)).thenReturn("03");
    }

    private void setupInvalidServiceInfoMock() {
        when(invalidServiceInfoMock.getHostAddresses()).thenReturn(new String[] { "1.1.1.1" });
        when(invalidServiceInfoMock.getPort()).thenReturn(1000);

        when(invalidServiceInfoMock.getPropertyString(PROPERTY_VENDOR_ID)).thenReturn("invalid");
    }

    @Test
    public void noScansWithoutBackgroundDiscovery() throws InterruptedException {
        // Wait a little more than one second to give background scanning a chance to kick in.
        Thread.sleep(1200);

        verify(mdnsClientMock, never()).list(anyString());
        verify(mdnsClientMock, never()).list(anyString(), ArgumentMatchers.any(Duration.class));
    }

    @Test
    public void singleScanReportsResultsCorrectAfterOneScan() {
        when(mdnsClientMock.list(SERVICE_TYPE, SINGLE_SCAN_DURATION))
                .thenReturn(new ServiceInfo[] { serviceInfo1Mock, serviceInfo2Mock });

        discovery.doSingleScan();

        // Expectation: discovery listener called with newly discovered devices usb1 and usb2; not called with removed
        // devices.

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(usb3);

        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(any(UsbSerialDeviceInformation.class));
    }

    @Test
    public void singleScanReportsResultsCorrectAfterOneScanWithInvalidServiceInfo() {
        when(mdnsClientMock.list(SERVICE_TYPE, SINGLE_SCAN_DURATION))
                .thenReturn(new ServiceInfo[] { serviceInfo1Mock, invalidServiceInfoMock, serviceInfo2Mock });

        discovery.doSingleScan();

        // Expectation: discovery listener is still called with newly discovered devices usb1 and usb2; not called with
        // removed devices.

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(usb3);

        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(any(UsbSerialDeviceInformation.class));
    }

    @Test
    public void singleScanReportsResultsCorrectlyAfterTwoScans() {
        when(mdnsClientMock.list(SERVICE_TYPE, SINGLE_SCAN_DURATION))
                .thenReturn(new ServiceInfo[] { serviceInfo1Mock, serviceInfo2Mock })
                .thenReturn(new ServiceInfo[] { serviceInfo2Mock, serviceInfo3Mock });

        discovery.unregisterDiscoveryListener(discoveryListenerMock);
        discovery.doSingleScan();

        discovery.registerDiscoveryListener(discoveryListenerMock);
        discovery.doSingleScan();

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
    public void backgroundScanning() {
        discovery.startBackgroundScanning();

        discovery.serviceAdded(serviceEvent1Mock);
        discovery.serviceRemoved(serviceEvent1Mock);
        discovery.serviceAdded(serviceEvent2Mock);
        discovery.serviceAdded(invalidServiceEventMock);
        discovery.serviceResolved(serviceEvent3Mock);

        discovery.stopBackgroundScanning();

        // Expectation: discovery listener called once for each discovered device, and once for removal of usb1.

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListenerMock, times(1)).usbSerialDeviceRemoved(usb1);

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb2);

        verify(discoveryListenerMock, times(1)).usbSerialDeviceDiscovered(usb3);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb3);
    }

    @Test
    public void noBackgroundScanning() throws IOException, InterruptedException {
        discovery.stopBackgroundScanning();

        discovery.serviceAdded(serviceEvent1Mock);
        discovery.serviceRemoved(serviceEvent1Mock);
        discovery.serviceAdded(serviceEvent2Mock);
        discovery.serviceResolved(serviceEvent3Mock);

        // Expectation: discovery listener is never called when there is no background scanning is.

        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb1);

        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb2);

        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(usb3);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb3);
    }

    @Test
    public void discoveryChecksSer2NetSpecificProperties() {
        discovery.startBackgroundScanning();

        when(serviceInfo3Mock.getPropertyString(PROPERTY_PROVIDER)).thenReturn(SER2NET);
        when(serviceInfo3Mock.getPropertyString(PROPERTY_GENSIO_STACK)).thenReturn("incompatible");

        discovery.serviceAdded(serviceEvent3Mock);

        when(serviceInfo3Mock.getPropertyString(PROPERTY_PROVIDER)).thenReturn(SER2NET);
        when(serviceInfo3Mock.getPropertyString(PROPERTY_DEVICE_TYPE)).thenReturn("incompatible");

        discovery.serviceAdded(serviceEvent3Mock);

        // Expectation: discovery listener is never called when the ser2net specific properties do not match.

        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(usb1);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb1);

        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(usb2);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb2);

        verify(discoveryListenerMock, never()).usbSerialDeviceDiscovered(usb3);
        verify(discoveryListenerMock, never()).usbSerialDeviceRemoved(usb3);
    }
}
