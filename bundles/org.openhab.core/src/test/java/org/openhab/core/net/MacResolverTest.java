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
package org.openhab.core.net;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link MacResolver} class.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
class MacResolverTest {

    class TestMacResolver extends MacResolver {

        @Override
        protected boolean isOnLocalSubnet(String ip) {
            return true; // force all IPs to be treated as local
        }

        protected void testPutCached(String ip, String mac, Instant expires) {
            ExpiringMac entry = new ExpiringMac(mac, expires);
            arpCache.put(ip, entry);
        }
    }

    final TestMacResolver macResolver = new TestMacResolver();

    @BeforeEach
    void setup() throws Exception {
        macResolver.activate();
        macResolver.arpCache.clear();
    }

    @AfterEach
    void teardown() throws Exception {
        macResolver.deactivate();
    }

    // -----------------------------
    // Normalization + Validation
    // -----------------------------

    @Test
    void testNormalizeMac() throws Exception {
        assertEquals("AA:BB:CC:DD:EE:FF", MacResolver.normalizeMac("aa-bb-cc-dd-ee-ff"));
        assertEquals("AA:BB:CC:DD:EE:FF", MacResolver.normalizeMac("AA:BB:CC:DD:EE:FF"));
        assertEquals("AA:BB:CC:DD:EE:FF", MacResolver.normalizeMac("aa:bb:cc:dd:ee:ff"));
    }

    @Test
    void testIsValidMac() throws Exception {
        assertTrue(MacResolver.isValidMac("AA:BB:CC:DD:EE:FF"));
        assertFalse(MacResolver.isValidMac("00:00:00:00:00:00"));
        assertFalse(MacResolver.isValidMac("AA:BB:CC:DD:EE")); // too short
        assertFalse(MacResolver.isValidMac("GG:HH:II:JJ:KK:LL")); // invalid hex
    }

    @Test
    void testNormalizeIP() throws Exception {
        assertEquals("192.168.1.1", MacResolver.normalizeIp("192.168.1.1:1234"));
        assertEquals("192.168.1.1", MacResolver.normalizeIp("192.168.1.1"));
    }

    @Test
    void testIsValidIp() throws Exception {
        assertTrue(MacResolver.isValidIp("192.168.1.1"));
        assertFalse(MacResolver.isValidIp("192.168.1.1:1234"));
        assertTrue(MacResolver.isValidIp(MacResolver.normalizeIp("192.168.1.1:1234")));
        assertFalse(MacResolver.isValidIp("999.999.999.999"));
    }

    // -----------------------------
    // parseLine() tests
    // -----------------------------

    @Test
    void testParseLineLinuxStyle() throws Exception {
        macResolver.arpCache.clear();

        String line = "192.168.1.10    0x1 0x2  aa:bb:cc:dd:ee:ff  *  br0";
        macResolver.parseLine(line);

        assertEquals("AA:BB:CC:DD:EE:FF", macResolver.cacheGet("192.168.1.10"));
    }

    @Test
    void testParseLineWindowsStyle() throws Exception {
        macResolver.arpCache.clear();

        String line = "  192.168.1.50       aa-bb-cc-dd-ee-ff     dynamic";
        macResolver.parseLine(line);

        assertEquals("AA:BB:CC:DD:EE:FF", macResolver.cacheGet("192.168.1.50"));
    }

    @Test
    void testParseLineIgnoresInvalid() throws Exception {
        macResolver.arpCache.clear();

        macResolver.parseLine("this is not an arp entry");
        macResolver.parseLine("999.999.999.999 aa:bb:cc:dd:ee:ff");

        assertTrue(macResolver.arpCache.isEmpty(), "Cache should remain empty after parsing invalid lines");
    }

    // -----------------------------
    // Cache behaviour
    // -----------------------------

    @Test
    void testCacheHitShortCircuitsLookup() throws Exception {
        macResolver.arpCache.clear();
        macResolver.testPutCached("1.2.3.4", "AA:BB:CC:DD:EE:FF", Instant.now().plusSeconds(60));

        String mac = macResolver.resolveMac("1.2.3.4").get(1, TimeUnit.SECONDS);

        assertEquals("AA:BB:CC:DD:EE:FF", mac);
    }

    @Test
    void testCacheExpiry() throws Exception {
        macResolver.arpCache.clear();

        macResolver.testPutCached("1.2.3.4", "AA:BB:CC:DD:EE:FF", Instant.now().minusSeconds(120));

        assertNull(macResolver.cacheGet("1.2.3.4"));
    }

    @Test
    void testBlankIpReturnsNull() throws Exception {
        assertNull(macResolver.resolveMac("").get(1, TimeUnit.SECONDS));
        assertNull(macResolver.resolveMac("   ").get(1, TimeUnit.SECONDS));
    }

    @Test
    void testResolveMacCompletesImmediatelyWhenCached() throws Exception {
        macResolver.arpCache.clear();

        String ip = "1.2.3.4";
        String mac = "AA:BB:CC:DD:EE:FF";

        // Simulate ARP output line
        String arpLine = ip + "     " + mac.replace(":", "-").toLowerCase() + "     dynamic";
        macResolver.parseLine(arpLine);

        // Now resolveMac should return a completed future
        CompletableFuture<@Nullable String> futureMac = macResolver.resolveMac(ip);

        assertTrue(futureMac.isDone(), "Future should be completed immediately");
        assertEquals(mac, futureMac.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testResolveMacLoopbackReturnsNull() throws Exception {
        macResolver.arpCache.clear();

        String ip = "127.0.0.1";

        CompletableFuture<@Nullable String> futureMac = macResolver.resolveMac(ip);

        assertTrue(futureMac.isDone(), "Future should be completed immediately for loopback");
        assertNull(futureMac.get(1, TimeUnit.SECONDS), "Loopback IP should return null MAC");
    }

    @Test
    void testParallelResolveMacSharesPendingFutureEntry() throws Exception {
        macResolver.arpCache.clear();

        // Use a guaranteed-local IP so isOnLocalSubnet(ip) passes
        String ip = "1.2.3.4";

        // Access private pendingFutures map
        Map<String, Set<CompletableFuture<@Nullable String>>> pendingFutureMacs = macResolver.pendingFutureMacs;
        assertNotNull(pendingFutureMacs, "pendingFutureMacs map should not be null");

        // Trigger two parallel resolveMac calls
        CompletableFuture<@Nullable String> futureMac1 = macResolver.resolveMac(ip);
        CompletableFuture<@Nullable String> futureMac2 = macResolver.resolveMac(ip);

        // Assert: two distinct CompletableFuture objects returned
        assertNotSame(futureMac1, futureMac2, "resolveMac must return two different CompletableFutures");

        // Assert: pendingFutures contains exactly ONE entry for this IP
        assertEquals(1, pendingFutureMacs.size(), "pendingFutureMacs must contain exactly one shared entry");
        assertTrue(pendingFutureMacs.containsKey(ip), "pendingFutureMacs must contain the IP key");

        // Assert: the map entry value set contains two CompletableFutures
        Set<CompletableFuture<@Nullable String>> futureMacs = pendingFutureMacs.get(ip);
        assertNotNull(futureMacs, "pendingFutureMacs entry must not be null");
        assertEquals(2, futureMacs.size(), "pendingFutureMacs entry must contain two CompletableFutures");

        assertTrue(futureMacs.contains(futureMac1), "pendingFutureMacs entry must contain futureMac1");
        assertTrue(futureMacs.contains(futureMac2), "pendingFutureMacs entry must contain futureMac2");

        assertFalse(futureMac1.isDone(), "futureMac1 should not be completed yet");
        assertFalse(futureMac2.isDone(), "futureMac2 should not be completed yet");

        // futureMac1 and futureMac2 should both complete when mac is resolved
        macResolver.cachePut(ip, "AA:BB:CC:DD:EE:FF");

        assertEquals("AA:BB:CC:DD:EE:FF", futureMac1.get(1, TimeUnit.SECONDS));
        assertEquals("AA:BB:CC:DD:EE:FF", futureMac2.get(1, TimeUnit.SECONDS));
    }
}
