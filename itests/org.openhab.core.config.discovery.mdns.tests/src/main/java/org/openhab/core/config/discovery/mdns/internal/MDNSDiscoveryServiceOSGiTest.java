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
package org.openhab.core.config.discovery.mdns.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.discovery.DiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Integration tests for the {@link MDNSDiscoveryService}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@NonNullByDefault
public class MDNSDiscoveryServiceOSGiTest extends JavaOSGiTest {

    private @NonNullByDefault({}) MDNSDiscoveryService mdnsDiscoveryService;

    @BeforeEach
    public void setup() {
        mdnsDiscoveryService = getService(DiscoveryService.class, MDNSDiscoveryService.class);
        assertThat(mdnsDiscoveryService, is(notNullValue()));
    }

    @Test
    public void testThingDiscoveredAndRemoved() {
        String serviceType = "_http._tcp.local.";
        ThingTypeUID thingTypeUID = new ThingTypeUID("myBinding", "myThingType");
        ThingUID thingUID = new ThingUID(thingTypeUID, "test" + new Random().nextInt(999999999));
        Set<ThingTypeUID> thingTypeUIDs = Set.of(thingTypeUID);
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).build();

        class TestParticipant implements MDNSDiscoveryParticipant {
            @Override
            public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
                return Set.of(thingTypeUID);
            }

            @Override
            public String getServiceType() {
                return serviceType;
            }

            @Override
            public @Nullable DiscoveryResult createResult(ServiceInfo info) {
                return discoveryResult;
            }

            @Override
            public @Nullable ThingUID getThingUID(ServiceInfo info) {
                return thingUID;
            }
        }

        MDNSDiscoveryParticipant participant = new TestParticipant();
        mdnsDiscoveryService.addMDNSDiscoveryParticipant(participant);

        assertThat(mdnsDiscoveryService.getSupportedThingTypes(), is(thingTypeUIDs));

        ServiceInfo serviceInfo = mock(ServiceInfo.class);
        when(serviceInfo.getServer()).thenReturn("my-host.local");
        when(serviceInfo.getPort()).thenReturn(1234);
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName("192.168.1.10");
        } catch (UnknownHostException e) {
            addr = null;
        }
        assertNotNull(addr);
        when(serviceInfo.getInetAddresses()).thenReturn(new InetAddress[] { addr });
        when(serviceInfo.getTextBytes()).thenReturn("ok".getBytes(StandardCharsets.UTF_8));

        ServiceEvent mockServiceEvent = mock(ServiceEvent.class);
        when(mockServiceEvent.getType()).thenReturn(serviceType);
        when(mockServiceEvent.getInfo()).thenReturn(serviceInfo);
        when(mockServiceEvent.getDNS()).thenReturn(mock(JmDNS.class));
        when(mockServiceEvent.getName()).thenReturn("my-host");

        DiscoveryListener mockDiscoveryListener = mock(DiscoveryListener.class);
        mdnsDiscoveryService.addDiscoveryListener(mockDiscoveryListener);

        mdnsDiscoveryService.serviceAdded(mockServiceEvent);
        verify(mockDiscoveryListener, timeout(2000).times(1)).thingDiscovered(mdnsDiscoveryService, discoveryResult);
        verifyNoMoreInteractions(mockDiscoveryListener);

        mdnsDiscoveryService.serviceResolved(mockServiceEvent);
        verify(mockDiscoveryListener, timeout(2000).times(1)).thingDiscovered(mdnsDiscoveryService, discoveryResult);
        verifyNoMoreInteractions(mockDiscoveryListener);

        mdnsDiscoveryService.serviceRemoved(mockServiceEvent);
        verify(mockDiscoveryListener, timeout(2000).times(1)).thingRemoved(mdnsDiscoveryService, thingUID);
        verifyNoMoreInteractions(mockDiscoveryListener);
    }

    /**
     * Test that configuring the background discovery dynamically via config admin is effective.
     */
    @Test
    public void testDynamicConfigurationOfBackgroundDiscovery() throws IOException {
        waitForAssert(() -> assertThat(mdnsDiscoveryService.isBackgroundDiscoveryEnabled(), is(true)), 5000, 100);

        setBackgroundDiscoveryViaConfigAdmin(false);
        waitForAssert(() -> assertThat(mdnsDiscoveryService.isBackgroundDiscoveryEnabled(), is(false)), 5000, 100);

        setBackgroundDiscoveryViaConfigAdmin(true);
        waitForAssert(() -> assertThat(mdnsDiscoveryService.isBackgroundDiscoveryEnabled(), is(true)), 5000, 100);
    }

    private void setBackgroundDiscoveryViaConfigAdmin(boolean status) throws IOException {
        ConfigurationAdmin configAdmin = getService(ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));

        @SuppressWarnings("null")
        Configuration configuration = configAdmin.getConfiguration("discovery.mdns");
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(DiscoveryService.CONFIG_PROPERTY_BACKGROUND_DISCOVERY, Boolean.valueOf(status));
        configuration.update(properties);
    }
}
