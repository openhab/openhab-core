/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for the {@link ExpiringCacheMap} class.
 *
 * @author Christoph Weitkamp - Initial contribution
 * @author Martin van Wingerden - Added tests for putIfAbsentAndGet
 */
@NonNullByDefault
public class ExpiringCacheMapTest {
    private static final long CACHE_EXPIRY = TimeUnit.SECONDS.toMillis(2);

    private static final String RESPONSE_1 = "ACTION 1";
    private static final String RESPONSE_2 = "ACTION 2";

    private static final Supplier<@Nullable String> CACHE_ACTION = () -> {
        byte[] array = new byte[8];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    };
    private static final Supplier<@Nullable String> PREDICTABLE_CACHE_ACTION_1 = () -> RESPONSE_1;
    private static final Supplier<@Nullable String> PREDICTABLE_CACHE_ACTION_2 = () -> RESPONSE_2;

    private static final String FIRST_TEST_KEY = "FIRST_TEST_KEY";
    private static final String SECOND_TEST_KEY = "SECOND_TEST_KEY";

    private @NonNullByDefault({}) ExpiringCacheMap<String, String> subject;

    @BeforeEach
    public void setUp() {
        subject = new ExpiringCacheMap<>(CACHE_EXPIRY);
    }

    @Test
    public void testPutIllegalArgumentException3() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> subject.put(null, CACHE_ACTION));
    }

    @Test
    public void testPut() {
        subject.put(FIRST_TEST_KEY, PREDICTABLE_CACHE_ACTION_1);

        assertEquals(RESPONSE_1, subject.get(FIRST_TEST_KEY));

        subject.put(FIRST_TEST_KEY, PREDICTABLE_CACHE_ACTION_2);

        String response = subject.get(FIRST_TEST_KEY);
        assertNotEquals(RESPONSE_1, response);
        assertEquals(RESPONSE_2, response);
    }

    @Test
    public void testPutIfAbsentAndGet() {
        String response = subject.putIfAbsentAndGet(FIRST_TEST_KEY, PREDICTABLE_CACHE_ACTION_1);

        assertEquals(RESPONSE_1, response);
        assertEquals(RESPONSE_1, subject.get(FIRST_TEST_KEY));

        response = subject.putIfAbsentAndGet(FIRST_TEST_KEY, PREDICTABLE_CACHE_ACTION_2);

        assertEquals(RESPONSE_1, response);
        assertNotEquals(RESPONSE_2, response);
    }

    @Test
    public void testPutValueIllegalArgumentException1() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> subject.putValue("KEY_NOT_FOUND", "test"));
    }

    @Test
    public void testPutValue() {
        subject.put(FIRST_TEST_KEY, CACHE_ACTION);

        subject.putValue(FIRST_TEST_KEY, "test");

        String value = subject.get(FIRST_TEST_KEY);
        assertEquals("test", value);
    }

    @Test
    public void testContainsKey() {
        subject.put(FIRST_TEST_KEY, CACHE_ACTION);

        assertTrue(subject.containsKey(FIRST_TEST_KEY));
    }

    @Test
    public void testKeys() {
        subject.put(FIRST_TEST_KEY, CACHE_ACTION);

        // get all keys
        final Set<String> expectedKeys = new LinkedHashSet<>();
        expectedKeys.add(FIRST_TEST_KEY);

        final Set<String> keys = subject.keys();
        assertEquals(expectedKeys, keys);
    }

    @Test
    public void testValues() {
        subject.put(FIRST_TEST_KEY, CACHE_ACTION);

        // use the same key twice
        String value1 = subject.get(FIRST_TEST_KEY);
        assertNotNull(value1);
        String value2 = subject.get(FIRST_TEST_KEY);
        assertNotNull(value2);
        assertEquals(value1, value2);

        subject.put(SECOND_TEST_KEY, CACHE_ACTION);

        // use a different key
        String value3 = subject.get(SECOND_TEST_KEY);
        assertNotNull(value3);
        assertNotEquals(value1, value3);

        if (value1 != null && value3 != null) {
            // get all values
            final Collection<String> expectedValues = new LinkedList<>();
            expectedValues.add(value3);
            expectedValues.add(value1);

            final Collection<@Nullable String> values = subject.values();
            assertEquals(expectedValues, values);
        }

        // use another different key
        String value4 = subject.get("KEY_NOT_FOUND");
        assertNull(value4);
    }

    @Test
    public void testExpired() throws InterruptedException {
        subject.put(FIRST_TEST_KEY, CACHE_ACTION);

        String value1 = subject.get(FIRST_TEST_KEY);

        // wait until cache expires
        Thread.sleep(CACHE_EXPIRY + 100);

        String value2 = subject.get(FIRST_TEST_KEY);
        assertNotEquals(value1, value2);
    }

    @Test
    public void testInvalidate() {
        subject.put(FIRST_TEST_KEY, CACHE_ACTION);

        String value1 = subject.get(FIRST_TEST_KEY);

        // invalidate item
        subject.invalidate(FIRST_TEST_KEY);

        String value2 = subject.get(FIRST_TEST_KEY);
        assertNotEquals(value1, value2);

        // invalidate all
        subject.invalidateAll();

        String value3 = subject.get(FIRST_TEST_KEY);
        assertNotEquals(value2, value3);
    }

    @Test
    public void testRefresh() {
        subject.put(FIRST_TEST_KEY, CACHE_ACTION);

        String value1 = subject.get(FIRST_TEST_KEY);

        // refresh item
        String value2 = subject.refresh(FIRST_TEST_KEY);
        assertNotNull(value2);
        assertNotEquals(value1, value2);

        if (value2 != null) {
            // refresh all
            final Collection<String> expectedValues = new LinkedList<>();
            expectedValues.add(value2);

            final Collection<@Nullable String> values = subject.refreshAll();
            assertNotEquals(expectedValues, values);
        }
    }

    @Test
    public void testRemove() {
        subject.put(FIRST_TEST_KEY, CACHE_ACTION);

        // remove item
        subject.remove(FIRST_TEST_KEY);

        String value1 = subject.get(FIRST_TEST_KEY);
        assertNull(value1);
    }

    @Test
    public void testClear() {
        subject.put(FIRST_TEST_KEY, CACHE_ACTION);

        // clear cache
        subject.clear();

        String value1 = subject.get(FIRST_TEST_KEY);
        assertNull(value1);
    }
}
