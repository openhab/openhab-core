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
package org.openhab.core.config.serial.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.openhab.core.config.serial.internal.SerialConfigOptionProvider.SERIAL_PORT;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscovery;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;

/**
 * Unit tests for the {@link SerialConfigOptionProvider}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SerialConfigOptionProviderTest {

    private static final String DEV_TTY_S1 = "/dev/ttyS1";
    private static final String DEV_TTY_S2 = "/dev/ttyS2";
    private static final String DEV_TTY_S3 = "/dev/ttyS3";

    private static final String RFC2217_IPV4 = "rfc2217://1.1.1.1:1000";
    private static final String RFC2217_IPV6 = "rfc2217://[0:0:0:0:0:ffff:0202:0202]:2222";

    private UsbSerialDeviceInformation usb1 = new UsbSerialDeviceInformation(0x100, 0x111, "serial1", "manufacturer1",
            "product1", 0x1, "interface1", RFC2217_IPV4);
    private UsbSerialDeviceInformation usb2 = new UsbSerialDeviceInformation(0x200, 0x222, "serial2", "manufacturer2",
            "product2", 0x2, "interface2", RFC2217_IPV6);
    private UsbSerialDeviceInformation usb3 = new UsbSerialDeviceInformation(0x300, 0x333, "serial3", "manufacturer3",
            "product3", 0x3, "interface3", DEV_TTY_S3);

    private @Mock @NonNullByDefault({}) SerialPortManager serialPortManagerMock;
    private @Mock @NonNullByDefault({}) UsbSerialDiscovery usbSerialDiscoveryMock;

    private @Mock @NonNullByDefault({}) SerialPortIdentifier serialPortIdentifier1Mock;
    private @Mock @NonNullByDefault({}) SerialPortIdentifier serialPortIdentifier2Mock;
    private @Mock @NonNullByDefault({}) SerialPortIdentifier serialPortIdentifier3Mock;

    private @NonNullByDefault({}) SerialConfigOptionProvider provider;

    @BeforeEach
    public void beforeEach() {
        provider = new SerialConfigOptionProvider(serialPortManagerMock);

        when(serialPortIdentifier1Mock.getName()).thenReturn(DEV_TTY_S1);
        when(serialPortIdentifier2Mock.getName()).thenReturn(DEV_TTY_S2);
        when(serialPortIdentifier3Mock.getName()).thenReturn(DEV_TTY_S3);
    }

    private void assertParameterOptions(String... serialPortIdentifiers) {
        Collection<ParameterOption> actual = provider.getParameterOptions(URI.create("uri"), "serialPort", SERIAL_PORT,
                null);
        Collection<ParameterOption> expected = Arrays.stream(serialPortIdentifiers)
                .map(id -> new ParameterOption(id, id)).collect(Collectors.toList());
        assertThat(actual, is(expected));
    }

    @Test
    public void noSerialPortIdentifiers() {
        when(serialPortManagerMock.getIdentifiers()).thenReturn(Stream.of());
        assertParameterOptions();
    }

    @Test
    public void serialPortManagerIdentifiersOnly() {
        when(serialPortManagerMock.getIdentifiers())
                .thenReturn(Stream.of(serialPortIdentifier1Mock, serialPortIdentifier2Mock));

        assertParameterOptions(DEV_TTY_S1, DEV_TTY_S2);
    }

    @Test
    public void discoveredIdentifiersOnly() {
        provider.addUsbSerialDiscovery(usbSerialDiscoveryMock);

        provider.usbSerialDeviceDiscovered(usb1);
        provider.usbSerialDeviceDiscovered(usb2);

        when(serialPortManagerMock.getIdentifiers()).thenReturn(Stream.of());

        assertParameterOptions(RFC2217_IPV4, RFC2217_IPV6);
    }

    @Test
    public void serialPortManagerAndDiscoveredIdentifiers() {
        provider.addUsbSerialDiscovery(usbSerialDiscoveryMock);

        provider.usbSerialDeviceDiscovered(usb1);
        provider.usbSerialDeviceDiscovered(usb2);

        when(serialPortManagerMock.getIdentifiers())
                .thenReturn(Stream.of(serialPortIdentifier1Mock, serialPortIdentifier2Mock));

        assertParameterOptions(DEV_TTY_S1, DEV_TTY_S2, RFC2217_IPV4, RFC2217_IPV6);
    }

    @Test
    public void removedDevicesAreRemoved() {
        provider.addUsbSerialDiscovery(usbSerialDiscoveryMock);

        provider.usbSerialDeviceDiscovered(usb1);

        when(serialPortManagerMock.getIdentifiers()).thenReturn(Stream.of());
        assertParameterOptions(RFC2217_IPV4);

        provider.usbSerialDeviceRemoved(usb1);

        when(serialPortManagerMock.getIdentifiers()).thenReturn(Stream.of());
        assertParameterOptions();
    }

    @Test
    public void discoveryRemovalClearsDiscoveryResults() {
        provider.addUsbSerialDiscovery(usbSerialDiscoveryMock);

        provider.usbSerialDeviceDiscovered(usb1);
        provider.usbSerialDeviceDiscovered(usb2);
        provider.usbSerialDeviceDiscovered(usb3);

        when(serialPortManagerMock.getIdentifiers()).thenReturn(Stream.of());
        assertParameterOptions(RFC2217_IPV4, RFC2217_IPV6, DEV_TTY_S3);

        provider.removeUsbSerialDiscovery(usbSerialDiscoveryMock);

        when(serialPortManagerMock.getIdentifiers()).thenReturn(Stream.of());
        assertParameterOptions();
    }

    @Test
    public void serialPortIdentifiersAreUnique() {
        provider.addUsbSerialDiscovery(usbSerialDiscoveryMock);

        provider.usbSerialDeviceDiscovered(usb3);

        when(serialPortManagerMock.getIdentifiers()).thenReturn(Stream.of(serialPortIdentifier3Mock));

        assertParameterOptions(DEV_TTY_S3);
    }

    @Test
    public void nullResultIfContextDoesNotMatch() {
        Collection<ParameterOption> actual = provider.getParameterOptions(URI.create("uri"), "serialPort",
                "otherContext", null);
        assertThat(actual, is(nullValue()));
    }
}
