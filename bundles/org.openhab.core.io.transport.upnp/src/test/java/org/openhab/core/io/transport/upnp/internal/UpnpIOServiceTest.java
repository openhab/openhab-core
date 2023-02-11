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
package org.openhab.core.io.transport.upnp.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupnp.UpnpService;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.io.transport.upnp.UpnpIOParticipant;

/**
 * Tests {@link UpnpIOServiceImpl}.
 *
 * @author Andre Fuechsel - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class UpnpIOServiceTest {

    private static final String UDN_1_STRING = "UDN";
    private static final UDN UDN_1 = new UDN(UDN_1_STRING);
    private static final String UDN_2_STRING = "UDN2";
    private static final UDN UDN_2 = new UDN(UDN_2_STRING);
    private static final String SERVICE_ID = "serviceId";
    private static final String SERVICE_ID_2 = "serviceId2";
    private static final String ACTION_ID = "actionId";
    private static final String DEVICE_TYPE = "deviceType";
    private static final String SERVICE_TYPE = "serviceType";

    private @Mock @NonNullByDefault({}) UpnpIOParticipant upnpIoParticipantMock;
    private @Mock @NonNullByDefault({}) UpnpIOParticipant upnpIoParticipant2Mock;
    private @Mock @NonNullByDefault({}) Registry upnpRegistryMock;
    private @Mock @NonNullByDefault({}) ControlPoint controlPointMock;
    private @Mock @NonNullByDefault({}) UpnpService upnpServiceMock;

    private @NonNullByDefault({}) UpnpIOServiceImpl upnpIoService;

    @BeforeEach
    public void setup() throws Exception {
        when(upnpIoParticipantMock.getUDN()).thenReturn(UDN_1_STRING);
        when(upnpIoParticipant2Mock.getUDN()).thenReturn(UDN_2_STRING);

        DeviceIdentity deviceIdentity = new DeviceIdentity(UDN_1);
        DeviceType deviceType = new DeviceType(UDAServiceId.DEFAULT_NAMESPACE, DEVICE_TYPE, 1);
        ServiceType serviceType = new ServiceType(UDAServiceId.DEFAULT_NAMESPACE, SERVICE_TYPE);

        ServiceId serviceId = new ServiceId(UDAServiceId.DEFAULT_NAMESPACE, SERVICE_ID);
        LocalService<?> service = new LocalService<>(serviceType, serviceId, null, null);
        LocalDevice device = new LocalDevice(deviceIdentity, deviceType, (DeviceDetails) null, service);

        ServiceId serviceId2 = new ServiceId(UDAServiceId.DEFAULT_NAMESPACE, SERVICE_ID_2);
        LocalService<?> service2 = new LocalService<>(serviceType, serviceId2, null, null);
        LocalDevice device2 = new LocalDevice(deviceIdentity, deviceType, (DeviceDetails) null, service2);

        when(upnpRegistryMock.getDevice(eq(UDN_1), anyBoolean())).thenReturn(device);
        when(upnpRegistryMock.getDevice(eq(UDN_2), anyBoolean())).thenReturn(device2);

        when(upnpServiceMock.getRegistry()).thenReturn(upnpRegistryMock);
        when(upnpServiceMock.getControlPoint()).thenReturn(controlPointMock);

        upnpIoService = new UpnpIOServiceImpl(upnpServiceMock);
    }

    @Test
    public void testIsRegistered() {
        assertTrue(upnpIoService.isRegistered(upnpIoParticipantMock));
    }

    @Test
    public void testIsRegisteredEverythingEmptyInitially() {
        assertTrue(upnpIoService.isRegistered(upnpIoParticipantMock));
        assertThatEverythingIsEmpty();
    }

    @Test
    public void testRegisterParticipant() {
        upnpIoService.registerParticipant(upnpIoParticipantMock);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipantMock));
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertTrue(upnpIoService.subscriptionCallbacks.keySet().isEmpty());
    }

    @Test
    public void testAddStatusListener() {
        upnpIoService.addStatusListener(upnpIoParticipantMock, SERVICE_ID, ACTION_ID, 60);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipantMock));
        assertEquals(1, upnpIoService.pollingJobs.keySet().size());
        assertTrue(upnpIoService.pollingJobs.containsKey(upnpIoParticipantMock));
        assertEquals(1, upnpIoService.currentStates.keySet().size());
        assertTrue(upnpIoService.currentStates.containsKey(upnpIoParticipantMock));
        assertTrue(upnpIoService.subscriptionCallbacks.keySet().isEmpty());

        upnpIoService.removeStatusListener(upnpIoParticipantMock);
        assertThatEverythingIsEmpty();
    }

    @Test
    public void testAddSubscription() {
        upnpIoService.addSubscription(upnpIoParticipantMock, SERVICE_ID, 60);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipantMock));
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertEquals(1, upnpIoService.subscriptionCallbacks.size());

        upnpIoService.addSubscription(upnpIoParticipant2Mock, SERVICE_ID_2, 60);
        assertEquals(2, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipantMock));
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertEquals(2, upnpIoService.subscriptionCallbacks.size());

        upnpIoService.removeSubscription(upnpIoParticipantMock, SERVICE_ID);
        upnpIoService.unregisterParticipant(upnpIoParticipantMock);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipant2Mock));
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertEquals(1, upnpIoService.subscriptionCallbacks.size());

        upnpIoService.removeSubscription(upnpIoParticipant2Mock, SERVICE_ID_2);
        upnpIoService.unregisterParticipant(upnpIoParticipant2Mock);
        assertThatEverythingIsEmpty();
    }

    private void assertThatEverythingIsEmpty() {
        assertTrue(upnpIoService.participants.isEmpty());
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertTrue(upnpIoService.subscriptionCallbacks.keySet().isEmpty());
    }
}
