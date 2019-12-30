/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the {@link ExpiringCache} class.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ExpiringCacheTest {
    private static final long CACHE_EXPIRY = TimeUnit.SECONDS.toMillis(2);
    private static final Supplier<String> CACHE_ACTION = () -> RandomStringUtils.random(8);

    private ExpiringCache<String> subject;

    @Before
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
