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
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.junit.Test;
import org.openhab.core.test.java.JavaTest;

/**
 * Tests cases for {@link ExpiringAsyncCache}.
 *
 * @author David Graeff - Initial contribution
 */
public class ExpiringCacheAsyncTest extends JavaTest {

    private double theValue = 0;

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWrongCacheTime() {
        // Fail if cache time is <= 0
        new ExpiringCacheAsync<>(0);
    }

    @Test
    public void testFetchValue() throws InterruptedException, ExecutionException {
        ExpiringCacheAsync<Double> t = new ExpiringCacheAsync<>(500);
        assertTrue(t.isExpired());
        // We should always be able to get the raw value, expired or not
        assertNull(t.getLastKnownValue());

        // Define a supplier which returns a future that is immediately completed.
        @SuppressWarnings({ "unchecked" })
        Supplier<CompletableFuture<Double>> s = mock(Supplier.class);
        when(s.get()).thenReturn(CompletableFuture.completedFuture(10.0));

        // We expect an immediate result with the value 10.0
        assertEquals(10.0, t.getValue(s).get(), 0.0);
        verify(s, times(1)).get();
        // The value should be valid
        assertFalse(t.isExpired());

        // We expect an immediate result with the value 10.0, but not additional call to the supplier
        assertEquals(10.0, t.getValue(s).get(), 0.0);
        verify(s, times(1)).get();

        // Wait for expiration
        waitForAssert(() -> {
            assertTrue(t.isExpired());
        });

        // We expect an immediate result with the value 10.0, and an additional call to the supplier
        assertEquals(10.0, t.getValue(s).get(), 0.0);
        verify(s, times(2)).get();

        // We should always be able to get the raw value, expired or not
        t.invalidateValue();
        assertEquals(10.0, t.getLastKnownValue(), 0.0);
        assertTrue(t.isExpired());
    }

    @Test
    public void testMutipleGetsWhileRefetching() {
        ExpiringCacheAsync<Double> t = new ExpiringCacheAsync<>(100);

        CompletableFuture<Double> v = new CompletableFuture<>();

        // Define a supplier which returns a future that is not yet completed
        Supplier<CompletableFuture<Double>> s = () -> v;

        assertNull(t.currentNewValueRequest);

        // Multiple get requests while the cache is still refreshing
        CompletableFuture<Double> result1 = t.getValue(s);
        CompletableFuture<Double> result2 = t.getValue(s);
        assertFalse(result1.isDone());
        assertFalse(result2.isDone());
        result1.thenAccept(newValue -> theValue = newValue);
        assertNotNull(t.currentNewValueRequest);

        // The refresh is finally done
        v.complete(10.0);
        assertEquals(10.0, theValue, 0.0);
        assertTrue(result1.isDone());
        assertTrue(result2.isDone());
        assertNull(t.currentNewValueRequest);
    }
}
