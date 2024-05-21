/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.discovery.sddp.SddpDevice;
import org.openhab.core.config.discovery.sddp.internal.SddpDiscoveryService;

/**
 * JUnit tests for parsing SDDP discovery results.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class SddpDiscoveryTests {

    private static final String ALIVE_NOTIFICATION = "NOTIFY ALIVE SDDP/1.0\r\n" +
    // @formatter:off
            "From: \"192.168.4.237:1902\"\r\n" +
            "Host: \"JVC_PROJECTOR-E0DADC152802\"\r\n" +
            "Max-Age: 1800\r\n" +
            "Type: \"JVCKENWOOD:Projector\"\r\n" +
            "Primary-Proxy: \"projector\"\r\n" +
            "Proxies: \"projector\"\r\n" +
            "Manufacturer: \"JVCKENWOOD\"\r\n" +
            "Model: \"DLA-RS3100_NZ8\"\r\n" +
            "Driver: \"projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i\"\r\n";
    // @formatter:on

    private static final String BAD_HEADER = "SDDP/1.0 404 NOT FOUND\r\n" +
    // @formatter:off
            "From: \"192.168.4.237:1902\"\r\n" +
            "Host: \"JVC_PROJECTOR-E0DADC152802\"\r\n" +
            "Max-Age: 1800\r\n" +
            "Type: \"JVCKENWOOD:Projector\"\r\n" +
            "Primary-Proxy: \"projector\"\r\n" +
            "Proxies: \"projector\"\r\n" +
            "Manufacturer: \"JVCKENWOOD\"\r\n" +
            "Model: \"DLA-RS3100_NZ8\"\r\n" +
            "Driver: \"projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i\"\r\n";
    // @formatter:on

    private static final String BAD_PAYLOAD = "SDDP/1.0 200 OK\\r\\n";

    private static final String SEARCH_RESPONSE = "SDDP/1.0 200 OK\r\n" +
    // @formatter:off
            "From: \"192.168.4.237:1902\"\r\n" +
            "Host: \"JVC_PROJECTOR-E0DADC152802\"\r\n" +
            "Max-Age: 1800\r\n" +
            "Type: \"JVCKENWOOD:Projector\"\r\n" +
            "Primary-Proxy: \"projector\"\r\n" +
            "Proxies: \"projector\"\r\n" +
            "Manufacturer: \"JVCKENWOOD\"\r\n" +
            "Model: \"DLA-RS3100_NZ8\"\r\n" +
            "Driver: \"projector_JVCKENWOOD_DLA-RS3100_NZ8.c4i\"\r\n";
    // @formatter:on

    @Test
    void testAliveNotification() throws Exception {
        try (SddpDiscoveryService service = new SddpDiscoveryService()) {
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
        }
    }

    @Test
    void testBadHeader() throws Exception {
        try (SddpDiscoveryService service = new SddpDiscoveryService()) {
            Optional<SddpDevice> deviceOptional = service.createSddpDevice(BAD_HEADER);
            assertFalse(deviceOptional.isPresent());
        }
    }

    @Test
    void testBadPayload() throws Exception {
        try (SddpDiscoveryService service = new SddpDiscoveryService()) {
            Optional<SddpDevice> deviceOptional = service.createSddpDevice(BAD_PAYLOAD);
            assertFalse(deviceOptional.isPresent());
        }
    }

    @Test
    void testSearchResponse() throws Exception {
        try (SddpDiscoveryService service = new SddpDiscoveryService()) {
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
        }
    }
}
