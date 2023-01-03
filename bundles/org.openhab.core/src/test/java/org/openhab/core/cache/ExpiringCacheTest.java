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
package org.openhab.core.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for the {@link ExpiringCache} class.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ExpiringCacheTest {
    private static final long CACHE_EXPIRY = TimeUnit.SECONDS.toMillis(2);

    private static final Supplier<@Nullable String> CACHE_ACTION = () -> {
        byte[] array = new byte[8];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    };

    private @NonNullByDefault({}) ExpiringCache<String> subject;

    @BeforeEach
    public void setUp() {
        subject = new ExpiringCache<>(CACHE_EXPIRY, CACHE_ACTION);
    }

    @Test
    public void testGetValue() {
        // use the same key twice
        String value1 = subject.getValue();

        assertNotNull(value1);

        String value2 = subject.getValue();

        assertNotNull(value2);
        assertEquals(value1, value2);
    }

    @Test
    public void testPutValue() {
        String value1 = subject.getValue();

        // put new value
        subject.putValue("test");

        String value2 = subject.getValue();
        assertEquals("test", value2);
        assertNotEquals(value1, value2);
    }

    @Test
    public void testExpired() throws InterruptedException {
        String value1 = subject.getValue();
        assertFalse(subject.isExpired());

        // wait until cache expires
        Thread.sleep(CACHE_EXPIRY + 100);
        assertTrue(subject.isExpired());

        String value2 = subject.getValue();
        assertFalse(subject.isExpired());
        assertNotEquals(value1, value2);
    }

    @Test
    public void testInvalidate() {
        String value1 = subject.getValue();

        // invalidate item
        subject.invalidateValue();

        String value2 = subject.getValue();
        assertNotEquals(value1, value2);
    }

    @Test
    public void testRefresh() {
        String value1 = subject.getValue();

        // refresh item
        String value2 = subject.refreshValue();
        assertNotEquals(value1, value2);
    }
}
