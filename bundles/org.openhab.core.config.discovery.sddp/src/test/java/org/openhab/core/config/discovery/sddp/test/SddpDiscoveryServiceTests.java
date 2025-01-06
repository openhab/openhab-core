/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.sddp.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openhab.core.config.discovery.sddp.SddpDevice;
import org.openhab.core.config.discovery.sddp.SddpDiscoveryService;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.net.NetworkAddressService;

/**
 * JUnit tests for parsing SDDP discovery results.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@TestInstance(Lifecycle.PER_CLASS)
public class SddpDiscoveryServiceTests {

    private static final String ALIVE_NOTIFICATION = """
            NOTIFY ALIVE SDDP/1.0
            From: "192.168.4.237:1902"
            Host: "JVC_PROJECTOR-E0DADC152802"
            Max-Age: 1800
            Type: "JVCKENWOOD:Projector"
            Primary-Proxy: "projector"
            Proxies: "projector"
            Manufacturer: "JVCKENWOOD"
            Model: "DLA-RS3100_NZ8"
            Driver: "projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i"
            """;

    private static final String IDENTIFY_NOTIFICATION = """
            NOTIFY IDENTIFY SDDP/1.0
            From: "192.168.4.237:1902"
            Host: "JVC_PROJECTOR-E0:DA:DC:15:28:02"
            Type: "JVCKENWOOD:Projector"
            Primary-Proxy: "projector"
            Proxies: "projector"
            Manufacturer: "JVCKENWOOD"
            Model: "DLA-RS3100_NZ8"
            Driver: "projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i"
            """;

    private static final String BAD_HEADER = """
            SDDP/1.0 404 NOT FOUND\r
            From: "192.168.4.237:1902"\r
            Host: "JVC_PROJECTOR-E0DADC152802"\r
            Max-Age: 1800\r
            Type: "JVCKENWOOD:Projector"\r
            Primary-Proxy: "projector"\r
            Proxies: "projector"\r
            Manufacturer: "JVCKENWOOD"\r
            Model: "DLA-RS3100_NZ8"\r
            Driver: "projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i"\r
            """;

    private static final String BAD_PAYLOAD = """
            SDDP/1.0 200 OK\r
            """;

    private static final String SEARCH_RESPONSE = """
            SDDP/1.0 200 OK\r
            From: "192.168.4.237:1902"\r
            Host: "JVC_PROJECTOR-E0DADC152802"\r
            Max-Age: 1800\r
            Type: "JVCKENWOOD:Projector"\r
            Primary-Proxy: "projector"\r
            Proxies: "projector"\r
            Manufacturer: "JVCKENWOOD"\r
            Model: "DLA-RS3100_NZ8"\r
            Driver: "projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i"\r
            """;

    private @NonNullByDefault({}) NetworkAddressService networkAddressService;

    @BeforeAll
    public void setup() {
        networkAddressService = mock(NetworkAddressService.class);
        when(networkAddressService.getPrimaryIpv4HostAddress()).thenReturn("192.168.1.1");
    }

    @Test
    void testAliveNotification() throws Exception {
        try (SddpDiscoveryService service = new SddpDiscoveryService(null, networkAddressService,
                mock(TranslationProvider.class), mock(LocaleProvider.class))) {
            Optional<SddpDevice> deviceOptional = service.createSddpDevice(ALIVE_NOTIFICATION);
            assertTrue(deviceOptional.isPresent());
            SddpDevice device = deviceOptional.orElse(null);
            assertNotNull(device);
            assertEquals("192.168.4.237:1902", device.from);
            assertEquals("JVC_PROJECTOR-E0DADC152802", device.host);
            assertEquals("1800", device.maxAge);
            assertEquals("JVCKENWOOD:Projector", device.type);
            assertEquals("projector", device.primaryProxy);
            assertEquals("projector", device.proxies);
            assertEquals("JVCKENWOOD", device.manufacturer);
            assertEquals("DLA-RS3100_NZ8", device.model);
            assertEquals("projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i", device.driver);
            assertEquals("192.168.4.237", device.ipAddress);
            assertEquals("e0-da-dc-15-28-02", device.macAddress);
            assertEquals("1902", device.port);
        }
    }

    @Test
    void testIdentifyNotification() throws Exception {
        try (SddpDiscoveryService service = new SddpDiscoveryService(null, networkAddressService,
                mock(TranslationProvider.class), mock(LocaleProvider.class))) {
            Optional<SddpDevice> deviceOptional = service.createSddpDevice(IDENTIFY_NOTIFICATION);
            assertTrue(deviceOptional.isPresent());
            SddpDevice device = deviceOptional.orElse(null);
            assertNotNull(device);
            assertEquals("192.168.4.237:1902", device.from);
            assertEquals("JVC_PROJECTOR-E0:DA:DC:15:28:02", device.host);
            assertTrue(device.maxAge.isBlank());
            assertEquals("JVCKENWOOD:Projector", device.type);
            assertEquals("projector", device.primaryProxy);
            assertEquals("projector", device.proxies);
            assertEquals("JVCKENWOOD", device.manufacturer);
            assertEquals("DLA-RS3100_NZ8", device.model);
            assertEquals("projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i", device.driver);
            assertEquals("192.168.4.237", device.ipAddress);
            assertEquals("e0-da-dc-15-28-02", device.macAddress);
            assertEquals("1902", device.port);
        }
    }

    @Test
    void testBadHeader() throws Exception {
        try (SddpDiscoveryService service = new SddpDiscoveryService(null, networkAddressService,
                mock(TranslationProvider.class), mock(LocaleProvider.class))) {
            Optional<SddpDevice> deviceOptional = service.createSddpDevice(BAD_HEADER);
            assertFalse(deviceOptional.isPresent());
        }
    }

    @Test
    void testBadPayload() throws Exception {
        try (SddpDiscoveryService service = new SddpDiscoveryService(null, networkAddressService,
                mock(TranslationProvider.class), mock(LocaleProvider.class))) {
            Optional<SddpDevice> deviceOptional = service.createSddpDevice(BAD_PAYLOAD);
            assertFalse(deviceOptional.isPresent());
        }
    }

    @Test
    void testSearchResponse() throws Exception {
        try (SddpDiscoveryService service = new SddpDiscoveryService(null, networkAddressService,
                mock(TranslationProvider.class), mock(LocaleProvider.class))) {
            Optional<SddpDevice> deviceOptional = service.createSddpDevice(SEARCH_RESPONSE);
            assertTrue(deviceOptional.isPresent());
            SddpDevice device = deviceOptional.orElse(null);
            assertNotNull(device);
            assertEquals("192.168.4.237:1902", device.from);
            assertEquals("JVC_PROJECTOR-E0DADC152802", device.host);
            assertEquals("1800", device.maxAge);
            assertEquals("JVCKENWOOD:Projector", device.type);
            assertEquals("projector", device.primaryProxy);
            assertEquals("projector", device.proxies);
            assertEquals("JVCKENWOOD", device.manufacturer);
            assertEquals("DLA-RS3100_NZ8", device.model);
            assertEquals("projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i", device.driver);
            assertEquals("192.168.4.237", device.ipAddress);
            assertEquals("e0-da-dc-15-28-02", device.macAddress);
            assertEquals("1902", device.port);
        }
    }
}
