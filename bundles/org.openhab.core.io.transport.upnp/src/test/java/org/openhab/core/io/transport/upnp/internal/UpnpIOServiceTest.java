/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jupnp.UpnpService;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
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
import org.openhab.core.io.transport.upnp.internal.UpnpIOServiceImpl.ParticipantData;
import org.openhab.core.util.SameThreadExecutorService;

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

        RemoteDeviceIdentity deviceIdentity = new RemoteDeviceIdentity(UDN_1, 300,
                URI.create("http://example.com/ident-descriptor").toURL(), new byte[4], null);
        DeviceType deviceType = new DeviceType(UDAServiceId.DEFAULT_NAMESPACE, DEVICE_TYPE, 1);
        ServiceType serviceType = new ServiceType(UDAServiceId.DEFAULT_NAMESPACE, SERVICE_TYPE);

        ServiceId serviceId = new ServiceId(UDAServiceId.DEFAULT_NAMESPACE, SERVICE_ID);
        RemoteService service = new RemoteService(serviceType, serviceId, URI.create("http://example.com/descriptor"),
                URI.create("http://example.com/control"), URI.create("http://example.com/events"));
        RemoteDevice device = new RemoteDevice(deviceIdentity, deviceType, (DeviceDetails) null, service);

        ServiceId serviceId2 = new ServiceId(UDAServiceId.DEFAULT_NAMESPACE, SERVICE_ID_2);
        RemoteService service2 = new RemoteService(serviceType, serviceId2, URI.create("http://example.org/descriptor"),
                URI.create("http://example.org/control"), URI.create("http://example.org/events"));
        RemoteDevice device2 = new RemoteDevice(deviceIdentity, deviceType, (DeviceDetails) null, service2);

        when(upnpRegistryMock.getRemoteDevice(eq(UDN_1), anyBoolean())).thenReturn(device);
        when(upnpRegistryMock.getRemoteDevice(eq(UDN_2), anyBoolean())).thenReturn(device2);

        when(upnpServiceMock.getRegistry()).thenReturn(upnpRegistryMock);
        when(upnpServiceMock.getControlPoint()).thenReturn(controlPointMock);

        upnpIoService = new UpnpIOServiceImpl(upnpServiceMock, new SameThreadExecutorService(), 0L);
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
        assertTrue(upnpIoService.participants.containsKey(upnpIoParticipantMock));
        assertTrue(upnpIoService.isParticipantRegistered(upnpIoParticipantMock));
        ParticipantData data = upnpIoService.participants.get(upnpIoParticipantMock);
        assertNotNull(data);
        assertFalse(data.hasJob());
        assertTrue(data.isAvailable());
        assertTrue(data.getCallbacks().isEmpty());
    }

    @Test
    public void testAddStatusListener() {
        upnpIoService.addStatusListener(upnpIoParticipantMock, SERVICE_ID, ACTION_ID, 60);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.containsKey(upnpIoParticipantMock));
        ParticipantData data = upnpIoService.participants.get(upnpIoParticipantMock);
        assertNotNull(data);
        assertTrue(data.hasJob());
        assertTrue(data.isAvailable());
        assertTrue(data.getCallbacks().isEmpty());

        upnpIoService.removeStatusListener(upnpIoParticipantMock);
        assertThatDataIsIsEmpty(data);
    }

    @Test
    public void testAddSubscription() {
        upnpIoService.addSubscription(upnpIoParticipantMock, SERVICE_ID, 60);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.containsKey(upnpIoParticipantMock));
        ParticipantData data = upnpIoService.participants.get(upnpIoParticipantMock);
        assertNotNull(data);
        assertFalse(data.hasJob());
        assertTrue(data.isAvailable());
        assertEquals(1, data.getCallbacks().size());

        upnpIoService.addSubscription(upnpIoParticipant2Mock, SERVICE_ID_2, 60);
        assertEquals(2, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.containsKey(upnpIoParticipantMock));
        data = upnpIoService.participants.get(upnpIoParticipant2Mock);
        assertNotNull(data);
        assertFalse(data.hasJob());
        assertTrue(data.isAvailable());
        assertEquals(1, data.getCallbacks().size());

        upnpIoService.removeSubscription(upnpIoParticipantMock, SERVICE_ID);
        upnpIoService.unregisterParticipant(upnpIoParticipantMock);
        assertEquals(1, upnpIoService.participants.size());
        assertTrue(upnpIoService.participants.containsKey(upnpIoParticipant2Mock));
        data = upnpIoService.participants.get(upnpIoParticipant2Mock);
        assertNotNull(data);
        assertFalse(data.hasJob());
        assertTrue(data.isAvailable());
        assertEquals(1, data.getCallbacks().size());

        upnpIoService.removeSubscription(upnpIoParticipant2Mock, SERVICE_ID_2);
        upnpIoService.unregisterParticipant(upnpIoParticipant2Mock);
        assertThatEverythingIsEmpty();
    }

    private void assertThatEverythingIsEmpty() {
        assertTrue(upnpIoService.participants.isEmpty());
    }

    private void assertThatDataIsIsEmpty(ParticipantData data) {
        assertFalse(data.hasJob());
        assertTrue(data.getCallbacks().isEmpty());
    }
}
