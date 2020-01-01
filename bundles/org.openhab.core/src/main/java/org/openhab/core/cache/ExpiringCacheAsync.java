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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Complementary class to {@link org.openhab.core.cache.ExpiringCache}, implementing an asynchronous variant
 * of an expiring cache. An instance returns the cached value immediately to the callback if not expired yet, otherwise
 * issue a fetch in another thread and notify callback implementors asynchronously.
 *
 * @author David Graeff - Initial contribution
 * @author Martin van Wingerden - Add Duration constructor
 *
 * @param <V> the type of the cached value
 */
@NonNullByDefault
public class ExpiringCacheAsync<V> {
    protected final long expiry;
    protected long expiresAt = 0;
    protected @Nullable CompletableFuture<V> currentNewValueRequest = null;
    protected @Nullable V value;

    /**
     * Create a new instance.
     *
     * @param expiry the duration in milliseconds for how long the value stays valid. Must be positive.
     * @throws IllegalArgumentException For an expire value <=0.
     */
    public ExpiringCacheAsync(Duration expiry) {
        if (expiry.isNegative() || expiry.isZero()) {
            throw new IllegalArgumentException("Cache expire time must be greater than 0");
        }
        this.expiry = expiry.toNanos();
    }

    /**
     * Create a new instance.
     *
     * @param expiry the duration in milliseconds for how long the value stays valid. Must be greater than 0.
     * @throws IllegalArgumentException For an expire value <=0.
     */
    public ExpiringCacheAsync(long expiry) {
        this(Duration.ofMillis(expiry));
    }

    /**
     * Returns the value - possibly from the cache, if it is still valid.
     *
     * @param requestNewValueFuture If the value is expired, this supplier is called to supply the cache with a future
     *            that on completion will update the cached value
     * @return the value in form of a CompletableFuture. You can for instance use it this way:
     *         `getValue().thenAccept(value->useYourValueHere(value));`. If you need the value synchronously you can use
     *         `getValue().get()`.
     */
    public CompletableFuture<V> getValue(Supplier<CompletableFuture<V>> requestNewValueFuture) {
        if (isExpired()) {
            return refreshValue(requestNewValueFuture);
        } else {
            return CompletableFuture.completedFuture(value);
        }
    }

    /**
     * Invalidates the value in the cache.
     */
    public void invalidateValue() {
        expiresAt = 0;
    }

    /**
     * Returns an arbitrary time reference in nanoseconds.
     * This is used for the cache to determine if a value has expired.
     */
    protected long getCurrentNanoTime() {
        return System.nanoTime();
    }

    /**
     * Refreshes and returns the value asynchronously. Use the return value like with getValue() to get the refreshed
     * value.
     *
     * @param requestNewValueFuture This supplier is called to supply the cache with a future
     *            that on completion will update the cached value. The supplier will not be used,
     *            if there is already an ongoing refresh.
     * @return the new value in form of a CompletableFuture.
     */
    @SuppressWarnings({ "null", "unused" })
    public synchronized CompletableFuture<V> refreshValue(Supplier<CompletableFuture<V>> requestNewValueFuture) {
        CompletableFuture<V> currentNewValueRequest = this.currentNewValueRequest;

        expiresAt = 0;
        // There is already an ongoing refresh, just return that future
        if (currentNewValueRequest != null) {
            return currentNewValueRequest;
        }
        // We request a value update from the supplier now
        currentNewValueRequest = this.currentNewValueRequest = requestNewValueFuture.get();
        if (currentNewValueRequest == null) {
            throw new IllegalArgumentException("We expect a CompletableFuture for refreshValue() to work!");
        }
        CompletableFuture<V> t = currentNewValueRequest.thenApply(newValue -> {
            // No request is ongoing anymore, update the value and expire time
            this.currentNewValueRequest = null;
            value = newValue;
            expiresAt = getCurrentNanoTime() + expiry;
            return value;
        });
        // The @NonNullbyDefault annotation forces us to check the return value of thenApply.
        if (t == null) {
            throw new IllegalArgumentException("We expect a CompletableFuture for refreshValue() to work!");
        }
        return t;
    }

    /**
     * Checks if the value is expired.
     *
     * @return true if the value is expired
     */
    public boolean isExpired() {
        return expiresAt < getCurrentNanoTime();
    }

    /**
     * Return the raw value, no matter if it is already
     * expired or still valid.
     */
    public @Nullable V getLastKnownValue() {
        return value;
    }
}
