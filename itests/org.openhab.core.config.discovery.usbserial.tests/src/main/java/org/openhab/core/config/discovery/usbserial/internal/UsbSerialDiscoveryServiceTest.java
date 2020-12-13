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
package org.openhab.core.config.discovery.usbserial.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openhab.core.config.discovery.DiscoveryService.CONFIG_PROPERTY_BACKGROUND_DISCOVERY;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.config.discovery.DiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscovery;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryParticipant;
import org.openhab.core.config.discovery.usbserial.testutil.UsbSerialDeviceInformationGenerator;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Unit tests for the {@link UsbSerialDiscoveryService}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
public class UsbSerialDiscoveryServiceTest extends JavaOSGiTest {

    private UsbSerialDiscovery usbSerialDiscovery;
    private UsbSerialDiscoveryService usbSerialDiscoveryService;

    private final UsbSerialDeviceInformationGenerator usbSerialDeviceInformationGenerator = new UsbSerialDeviceInformationGenerator();

    @BeforeEach
    public void setup() {
        usbSerialDiscovery = mock(UsbSerialDiscovery.class);
        registerService(usbSerialDiscovery);

        usbSerialDiscoveryService = getService(UsbSerialDiscoveryService.class);
    }

    @Test
    public void testSettingUsbSerialDiscoveryStartsBackgroundDiscoveryIfEnabled()
            throws InterruptedException, IOException {
        // Background discovery is enabled by default, hence no need to set it explicitly again. In consequence,
        // background discovery is started >=1 times (both in the activator and in startBackgroundDiscovery).
        verify(usbSerialDiscovery, atLeast(1)).startBackgroundScanning();
    }

    @Test
    public void testSettingUsbSerialDiscoveryDoesNotStartBackgroundDiscoveryIfDisabled()
            throws IOException, InterruptedException {
        setBackgroundDiscovery(false);
        unregisterService(usbSerialDiscovery);

        UsbSerialDiscovery anotherUsbSerialDiscovery = mock(UsbSerialDiscovery.class);
        registerService(anotherUsbSerialDiscovery);
        verify(anotherUsbSerialDiscovery, never()).startBackgroundScanning();
    }

    @Test
    public void testRegistersAsUsbserialDiscoveryListener() {
        verify(usbSerialDiscovery, times(1)).registerDiscoveryListener(usbSerialDiscoveryService);
    }

    @Test
    public void testUnregistersAsUsbserialDiscoveryListener() {
        unregisterService(usbSerialDiscovery);
        verify(usbSerialDiscovery, times(1)).unregisterDiscoveryListener(usbSerialDiscoveryService);
    }

    @Test
    public void testSupportedThingTypesAreRetrievedFromDiscoveryParticipants() {
        // with no discovery participants available, no thing types are supported.
        assertThat(usbSerialDiscoveryService.getSupportedThingTypes(), is(empty()));

        // with two discovery participants available, the thing types supported by them are supported.
        ThingTypeUID thingTypeA = new ThingTypeUID("a:b:c");
        ThingTypeUID thingTypeB = new ThingTypeUID("d:e:f");
        ThingTypeUID thingTypeC = new ThingTypeUID("g:h:i");

        UsbSerialDiscoveryParticipant discoveryParticipantA = mock(UsbSerialDiscoveryParticipant.class);
        when(discoveryParticipantA.getSupportedThingTypeUIDs()).thenReturn(Set.of(thingTypeA, thingTypeB));
        registerService(discoveryParticipantA);

        UsbSerialDiscoveryParticipant discoveryParticipantB = mock(UsbSerialDiscoveryParticipant.class);
        when(discoveryParticipantB.getSupportedThingTypeUIDs()).thenReturn(Set.of(thingTypeB, thingTypeC));
        registerService(discoveryParticipantB);

        assertThat(usbSerialDiscoveryService.getSupportedThingTypes(),
                containsInAnyOrder(thingTypeA, thingTypeB, thingTypeC));
    }

    @Test
    public void testThingsAreActuallyDiscovered() {
        // register one discovery listener
        DiscoveryListener discoveryListener = mock(DiscoveryListener.class);
        usbSerialDiscoveryService.addDiscoveryListener(discoveryListener);

        // register two discovery participants
        UsbSerialDiscoveryParticipant discoveryParticipantA = mock(UsbSerialDiscoveryParticipant.class);
        registerService(discoveryParticipantA);

        UsbSerialDiscoveryParticipant discoveryParticipantB = mock(UsbSerialDiscoveryParticipant.class);
        registerService(discoveryParticipantB);

        // when no discovery participant supports a newly discovered device, no device is discovered
        when(discoveryParticipantA.createResult(any())).thenReturn(null);
        when(discoveryParticipantB.createResult(any())).thenReturn(null);
        usbSerialDiscoveryService.usbSerialDeviceDiscovered(generateDeviceInfo());
        verify(discoveryListener, never()).thingDiscovered(any(), any());

        // when only the first discovery participant supports a newly discovered device, the device is discovered
        UsbSerialDeviceInformation deviceInfoA = generateDeviceInfo();
        DiscoveryResult discoveryResultA = mock(DiscoveryResult.class);
        when(discoveryResultA.getThingUID()).thenReturn(new ThingUID("mock:thing:uida"));
        when(discoveryResultA.getTimeToLive()).thenReturn(10L);
        when(discoveryParticipantA.createResult(deviceInfoA)).thenReturn(discoveryResultA);
        usbSerialDiscoveryService.usbSerialDeviceDiscovered(deviceInfoA);
        ArgumentCaptor<DiscoveryResult> captor = ArgumentCaptor.forClass(DiscoveryResult.class);
        verify(discoveryListener, times(1)).thingDiscovered(eq(usbSerialDiscoveryService), captor.capture());
        DiscoveryResult actualDiscoveryResultA = captor.getValue();
        assertThat(actualDiscoveryResultA.getProperties(),
                is(createUsbPropertiesMap(discoveryResultA.getProperties(), deviceInfoA)));
        reset(discoveryListener);

        // when only the second discovery participant supports a newly discovered device, the device is also discovered
        UsbSerialDeviceInformation deviceInfoB = generateDeviceInfo();
        DiscoveryResult discoveryResultB = mock(DiscoveryResult.class);
        when(discoveryResultB.getThingUID()).thenReturn(new ThingUID("mock:thing:uidb"));
        when(discoveryResultB.getTimeToLive()).thenReturn(10L);
        when(discoveryParticipantB.createResult(deviceInfoB)).thenReturn(discoveryResultB);
        usbSerialDiscoveryService.usbSerialDeviceDiscovered(deviceInfoB);
        verify(discoveryListener, times(1)).thingDiscovered(eq(usbSerialDiscoveryService), captor.capture());
        DiscoveryResult actualDiscoveryResultB = captor.getValue();
        assertThat(actualDiscoveryResultB.getProperties(),
                is(createUsbPropertiesMap(discoveryResultB.getProperties(), deviceInfoB)));
    }

    @Test
    public void testDiscoveredThingsAreRemoved() {
        // register one discovery listener
        DiscoveryListener discoveryListener = mock(DiscoveryListener.class);
        usbSerialDiscoveryService.addDiscoveryListener(discoveryListener);

        // register one discovery participant
        UsbSerialDiscoveryParticipant discoveryParticipant = mock(UsbSerialDiscoveryParticipant.class);
        registerService(discoveryParticipant);

        // when the discovery participant does not support a removed device, no discovery result is removed
        when(discoveryParticipant.createResult(any())).thenReturn(null);
        usbSerialDiscoveryService.usbSerialDeviceRemoved(generateDeviceInfo());
        verify(discoveryListener, never()).thingRemoved(any(), any());

        // when the first discovery participant supports a removed device, the discovery result is removed
        UsbSerialDeviceInformation deviceInfo = generateDeviceInfo();
        ThingUID thingUID = mock(ThingUID.class);
        when(discoveryParticipant.getThingUID(deviceInfo)).thenReturn(thingUID);
        usbSerialDiscoveryService.usbSerialDeviceRemoved(deviceInfo);
        verify(discoveryListener, times(1)).thingRemoved(usbSerialDiscoveryService, thingUID);
    }

    @Test
    public void testAddingDiscoveryParticipantAfterAddingUsbDongle() {
        UsbSerialDeviceInformation usb1 = generateDeviceInfo();
        UsbSerialDeviceInformation usb2 = generateDeviceInfo();
        UsbSerialDeviceInformation usb3 = generateDeviceInfo();

        // get info about three added and one removed USB dongles from UsbSerialDiscovery
        usbSerialDiscoveryService.usbSerialDeviceDiscovered(usb1);
        usbSerialDiscoveryService.usbSerialDeviceDiscovered(usb2);
        usbSerialDiscoveryService.usbSerialDeviceRemoved(usb1);
        usbSerialDiscoveryService.usbSerialDeviceDiscovered(usb3);

        // register one discovery participant
        UsbSerialDiscoveryParticipant discoveryParticipant = mock(UsbSerialDiscoveryParticipant.class);
        registerService(discoveryParticipant);

        // then this discovery participant is informed about USB devices usb2 and usb3, but not about usb1
        verify(discoveryParticipant, never()).createResult(usb1);
        verify(discoveryParticipant, times(1)).createResult(usb2);
        verify(discoveryParticipant, times(1)).createResult(usb3);
    }

    private void setBackgroundDiscovery(boolean status) throws IOException, InterruptedException {
        ConfigurationAdmin configAdmin = getService(ConfigurationAdmin.class);
        Configuration configuration = configAdmin.getConfiguration("discovery.usbserial");
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(CONFIG_PROPERTY_BACKGROUND_DISCOVERY, Boolean.valueOf(status));
        configuration.update(properties);

        // wait until the configuration is actually set in the usbSerialDiscoveryService
        waitForAssert(() -> {
            assertThat(usbSerialDiscoveryService.isBackgroundDiscoveryEnabled(), is(status));
        }, 1000, 100);
    }

    private UsbSerialDeviceInformation generateDeviceInfo() {
        return usbSerialDeviceInformationGenerator.generate();
    }

    private Map<String, Object> createUsbPropertiesMap(Map<String, Object> properties,
            UsbSerialDeviceInformation usbSerialDeviceInformation) {
        Map<String, Object> resultProperties = new HashMap<>(properties);
        resultProperties.put("usb_vendor_id", usbSerialDeviceInformation.getVendorId());
        resultProperties.put("usb_product_id", usbSerialDeviceInformation.getProductId());
        return resultProperties;
    }
}
