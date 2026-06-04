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

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the {@link MacResolver} class.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
class MacResolverTest {

    final MacResolver macResolver = new MacResolver();

    @BeforeEach
    void setup() throws Exception {
        macResolver.activate();
        macResolver.testClearCache();
    }

    // -----------------------------
    // Normalization + Validation
    // -----------------------------

    @Test
    void testNormalizeMac() throws Exception {
        assertEquals("AA:BB:CC:DD:EE:FF", normalizeMac("aa-bb-cc-dd-ee-ff"));
        assertEquals("AA:BB:CC:DD:EE:FF", normalizeMac("AA:BB:CC:DD:EE:FF"));
        assertEquals("AA:BB:CC:DD:EE:FF", normalizeMac("aa:bb:cc:dd:ee:ff"));
    }

    @Test
    void testIsValidMac() throws Exception {
        assertTrue(isValidMac("AA:BB:CC:DD:EE:FF"));
        assertFalse(isValidMac("00:00:00:00:00:00"));
        assertFalse(isValidMac("AA:BB:CC:DD:EE")); // too short
        assertFalse(isValidMac("GG:HH:II:JJ:KK:LL")); // invalid hex
    }

    @Test
    void testNormalizeIP() throws Exception {
        assertEquals("192.168.1.1", normalizeIp("192.168.1.1:1234"));
        assertEquals("192.168.1.1", normalizeIp("192.168.1.1"));
    }

    @Test
    void testIsValidIp() throws Exception {
        assertTrue(isValidIp("192.168.1.1"));
        assertFalse(isValidIp("192.168.1.1:1234"));
        assertTrue(isValidIp(normalizeIp("192.168.1.1:1234")));
        assertFalse(isValidIp("999.999.999.999"));
    }

    // -----------------------------
    // parseLine() tests
    // -----------------------------

    @Test
    void testParseLineLinuxStyle() throws Exception {
        macResolver.testClearCache();

        String line = "192.168.1.10    0x1 0x2  aa:bb:cc:dd:ee:ff  *  br0";
        parseLine(line);

        assertEquals("AA:BB:CC:DD:EE:FF", macResolver.testGetCached("192.168.1.10"));
    }

    @Test
    void testParseLineWindowsStyle() throws Exception {
        macResolver.testClearCache();

        String line = "  192.168.1.50       aa-bb-cc-dd-ee-ff     dynamic";
        parseLine(line);

        assertEquals("AA:BB:CC:DD:EE:FF", macResolver.testGetCached("192.168.1.50"));
    }

    @Test
    void testParseLineIgnoresInvalid() throws Exception {
        macResolver.testClearCache();

        parseLine("this is not an arp entry");
        parseLine("999.999.999.999 aa:bb:cc:dd:ee:ff");

        assertTrue(macResolver.testCacheIsEmpty());
    }

    // -----------------------------
    // Cache behaviour
    // -----------------------------

    @Test
    void testCacheHitShortCircuitsLookup() throws Exception {
        macResolver.testClearCache();
        macResolver.testPutCached("127.0.0.1", "AA:BB:CC:DD:EE:FF", Instant.now().plusSeconds(60));

        String mac = macResolver.resolveMac("127.0.0.1").get(1, TimeUnit.SECONDS);

        assertEquals("AA:BB:CC:DD:EE:FF", mac);
    }

    @Test
    void testCacheExpiry() throws Exception {
        macResolver.testClearCache();

        macResolver.testPutCached("1.2.3.4", "AA:BB:CC:DD:EE:FF", Instant.now().minusSeconds(120));

        assertNull(macResolver.testGetCached("1.2.3.4"));
    }

    @Test
    void testBlankIpReturnsNull() throws Exception {
        assertNull(macResolver.resolveMac("").get(1, TimeUnit.SECONDS));
        assertNull(macResolver.resolveMac("   ").get(1, TimeUnit.SECONDS));
    }

    // -----------------------------
    // runCommandAndParse() test
    // -----------------------------

    @Test
    void testRunCommandAndParse() throws Exception {
        macResolver.testClearCache();

        String fakeLine = "192.168.1.77 aa:bb:cc:dd:ee:ff";
        String prop = System.getProperty("os.name");
        assertNotNull(prop);

        if (prop.toLowerCase().contains("win")) {
            runCommand(Duration.ofSeconds(1), "cmd", "/c", "echo " + fakeLine);
        } else {
            runCommand(Duration.ofSeconds(1), "echo", fakeLine);
        }

        assertEquals("AA:BB:CC:DD:EE:FF", macResolver.testGetCached("192.168.1.77"));
    }

    @Test
    void testResolveMacCompletesImmediatelyWhenCached() throws Exception {
        macResolver.testClearCache();

        String ip = "127.0.0.1";
        String mac = "AA:BB:CC:DD:EE:FF";

        // Simulate ARP output line
        String arpLine = ip + "     " + mac.replace(":", "-").toLowerCase() + "     dynamic";
        invokePrivate("parseLine", String.class, arpLine);

        // Now resolveMac should return a completed future
        CompletableFuture<@Nullable String> future = macResolver.resolveMac(ip);

        assertTrue(future.isDone(), "Future should be completed immediately");
        assertEquals(mac, future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testResolveMacCompletionStageRunsOnceWhenAlreadyCached() throws Exception {
        macResolver.testClearCache();

        String ip = "127.0.0.1";
        String mac = "AA:BB:CC:DD:EE:FF";

        // First ARP line
        invokePrivate("parseLine", String.class, ip + " " + mac);

        CompletableFuture<@Nullable String> future = macResolver.resolveMac(ip);
        assertEquals(mac, future.get(1, TimeUnit.SECONDS));

        // Reset detection state
        AtomicInteger counter = new AtomicInteger(0);

        // Wrap resolveMac to count completions
        CompletableFuture<@Nullable String> futureMac = macResolver.resolveMac(ip).thenApply(m -> {
            counter.incrementAndGet();
            return m;
        });

        // Second ARP line (duplicate)
        invokePrivate("parseLine", String.class, ip + " " + mac);

        // Future should still complete only once
        assertEquals(mac, futureMac.get(1, TimeUnit.SECONDS));
        assertEquals(1, counter.get(), "MAC resolution should only complete once");
    }

    @Test
    void testParallelResolveMacSharesPendingFutureEntry() throws Exception {
        macResolver.testClearCache();

        // Use a guaranteed-local IP so isOnLocalSubnet(ip) passes
        String ip = "127.0.0.1";

        // Access private pendingFutures map
        Map<String, CompletableFuture<@Nullable String>> pendingFutureMacs = macResolver.testGetPendingFutureMacs();
        assertNotNull(pendingFutureMacs, "pendingFutureMacs map should not be null");

        // Trigger two parallel resolveMac calls
        CompletableFuture<@Nullable String> futureMac1 = macResolver.resolveMac(ip);
        CompletableFuture<@Nullable String> futureMac2 = macResolver.resolveMac(ip);

        // Assert: two distinct CompletableFuture objects returned
        assertNotSame(futureMac1, futureMac2, "resolveMac must return two different CompletableFutures");

        // Assert: pendingFutures contains exactly ONE entry for this IP
        assertEquals(1, pendingFutureMacs.size(), "pendingFutureMacs must contain exactly one shared entry");
        assertTrue(pendingFutureMacs.containsKey(ip), "pendingFutureMacs must contain the IP key");

        // Assert: both returned futures wrap the SAME underlying pending future
        CompletableFuture<@Nullable String> sharedFutureMac = pendingFutureMacs.get(ip);
        assertNotNull(sharedFutureMac, "pendingFutureMacs entry must not be null");

        assertFalse(futureMac1.isDone(), "futureMac1 should not be completed yet");
        assertFalse(futureMac2.isDone(), "futureMac2 should not be completed yet");

        // futureMac1 and futureMac2 should both complete when shared completes
        sharedFutureMac.complete("AA:BB:CC:DD:EE:FF");

        assertEquals("AA:BB:CC:DD:EE:FF", futureMac1.get(1, TimeUnit.SECONDS));
        assertEquals("AA:BB:CC:DD:EE:FF", futureMac2.get(1, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------
    // Reflection helpers to call private methods
    // -------------------------------------------------------

    private @Nullable Object invokePrivate(String method, Class<?> paramType, Object arg) throws Exception {
        Method m = MacResolver.class.getDeclaredMethod(method, paramType);
        m.setAccessible(true);
        return m.invoke(macResolver, arg);
    }

    private void parseLine(String line) throws Exception {
        invokePrivate("parseLine", String.class, line);
    }

    private String normalizeMac(String mac) throws Exception {
        Object result = invokePrivate("normalizeMac", String.class, mac);
        assertNotNull(result, "normalizeMac returned null");
        return (String) result;
    }

    private String normalizeIp(String ip) throws Exception {
        Object result = invokePrivate("normalizeIp", String.class, ip);
        assertNotNull(result, "normalizeIp returned null");
        return (String) result;
    }

    private boolean isValidMac(String mac) throws Exception {
        Object result = invokePrivate("isValidMac", String.class, mac);
        assertNotNull(result, "isValidMac returned null");
        return (Boolean) result;
    }

    private boolean isValidIp(String ip) throws Exception {
        Object result = invokePrivate("isValidIp", String.class, ip);
        assertNotNull(result, "isValidIp returned null");
        return (Boolean) result;
    }

    private void runCommand(Duration timeout, String... cmd) throws Exception {
        Method m = MacResolver.class.getDeclaredMethod("runCommandAndParse", Duration.class, String[].class);
        m.setAccessible(true);
        m.invoke(macResolver, timeout, (Object) cmd);
    }
}
