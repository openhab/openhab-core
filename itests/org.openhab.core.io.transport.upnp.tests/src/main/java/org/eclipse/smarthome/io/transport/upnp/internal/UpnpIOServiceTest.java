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
package org.eclipse.smarthome.io.transport.upnp.internal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.eclipse.smarthome.io.transport.upnp.UpnpIOParticipant;
import org.junit.Before;
import org.junit.Test;
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

    private @Mock UpnpIOParticipant upnpIoParticipant;
    private @Mock UpnpIOParticipant upnpIoParticipant2;
    private @Mock Registry upnpRegistry;
    private @Mock ControlPoint controlPoint;
    private @Mock UpnpService upnpServiceMock;

    private UpnpIOServiceImpl upnpIoService;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(upnpIoParticipant.getUDN()).thenReturn(UDN_1_STRING);

        when(upnpIoParticipant2.getUDN()).thenReturn(UDN_2_STRING);

        DeviceIdentity deviceIdentity = new DeviceIdentity(UDN_1);
        DeviceType deviceType = new DeviceType(UDAServiceId.DEFAULT_NAMESPACE, DEVICE_TYPE, 1);
        ServiceType serviceType = new ServiceType(UDAServiceId.DEFAULT_NAMESPACE, SERVICE_TYPE);

        ServiceId serviceId = new ServiceId(UDAServiceId.DEFAULT_NAMESPACE, SERVICE_ID);
        LocalService<?> service = new LocalService<>(serviceType, serviceId, null, null);
        LocalDevice device = new LocalDevice(deviceIdentity, deviceType, (DeviceDetails) null, service);

        ServiceId serviceId2 = new ServiceId(UDAServiceId.DEFAULT_NAMESPACE, SERVICE_ID_2);
        LocalService<?> service2 = new LocalService<>(serviceType, serviceId2, null, null);
        LocalDevice device2 = new LocalDevice(deviceIdentity, deviceType, (DeviceDetails) null, service2);

        when(upnpRegistry.getDevice(eq(UDN_1), anyBoolean())).thenReturn(device);
        when(upnpRegistry.getDevice(eq(UDN_2), anyBoolean())).thenReturn(device2);

        when(upnpServiceMock.getRegistry()).thenReturn(upnpRegistry);
        when(upnpServiceMock.getControlPoint()).thenReturn(controlPoint);

        upnpIoService = new UpnpIOServiceImpl();
        upnpIoService.setUpnpService(upnpServiceMock);
    }

    @Test
    public void testIsRegistered() {
        assertTrue(upnpIoService.isRegistered(upnpIoParticipant));
    }

    @Test
    public void testIsRegistered_everythingEmptyInitially() {
        assertTrue(upnpIoService.isRegistered(upnpIoParticipant));
        assertThatEverythingIsEmpty();
    }

    @Test
    public void testRegisterParticipant() {
        upnpIoService.registerParticipant(upnpIoParticipant);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipant));
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertTrue(upnpIoService.subscriptionCallbacks.keySet().isEmpty());
    }

    @Test
    public void testAddStatusListener() {
        upnpIoService.addStatusListener(upnpIoParticipant, SERVICE_ID, ACTION_ID, 60);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipant));
        assertEquals(1, upnpIoService.pollingJobs.keySet().size());
        assertTrue(upnpIoService.pollingJobs.containsKey(upnpIoParticipant));
        assertEquals(1, upnpIoService.currentStates.keySet().size());
        assertTrue(upnpIoService.currentStates.containsKey(upnpIoParticipant));
        assertTrue(upnpIoService.subscriptionCallbacks.keySet().isEmpty());

        upnpIoService.removeStatusListener(upnpIoParticipant);
        assertThatEverythingIsEmpty();
    }

    @Test
    public void testAddSubscription() {
        upnpIoService.addSubscription(upnpIoParticipant, SERVICE_ID, 60);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipant));
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertEquals(1, upnpIoService.subscriptionCallbacks.size());

        upnpIoService.addSubscription(upnpIoParticipant2, SERVICE_ID_2, 60);
        assertEquals(2, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipant));
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertEquals(2, upnpIoService.subscriptionCallbacks.size());

        upnpIoService.removeSubscription(upnpIoParticipant, SERVICE_ID);
        upnpIoService.unregisterParticipant(upnpIoParticipant);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.contains(upnpIoParticipant2));
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertEquals(1, upnpIoService.subscriptionCallbacks.size());

        upnpIoService.removeSubscription(upnpIoParticipant2, SERVICE_ID_2);
        upnpIoService.unregisterParticipant(upnpIoParticipant2);
        assertThatEverythingIsEmpty();
    }

    private void assertThatEverythingIsEmpty() {
        assertTrue(upnpIoService.participants.isEmpty());
        assertTrue(upnpIoService.pollingJobs.keySet().isEmpty());
        assertTrue(upnpIoService.currentStates.keySet().isEmpty());
        assertTrue(upnpIoService.subscriptionCallbacks.keySet().isEmpty());
    }
}
