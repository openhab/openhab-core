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
package org.openhab.core.io.rest.auth.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openhab.core.io.rest.auth.internal.ExpiringUserSecurityContextCache.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.GenericUser;

/**
 * Tests {@link ExpiringUserSecurityContextCache}.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class ExpiringUserSecurityContextCacheTest {

    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private ExpiringUserSecurityContextCache createCache(Duration expirationDuration) {
        return new ExpiringUserSecurityContextCache(expirationDuration.toMillis());
    }

    private Map<String, UserSecurityContext> createValues(int count) {
        Map<String, UserSecurityContext> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String userName = "user" + i;
            UserSecurityContext userSecurityContext = new UserSecurityContext(new GenericUser(userName),
                    new Authentication(userName), userName + " token");
            map.put("key" + i, userSecurityContext);
        }
        return map;
    }

    private void addValues(ExpiringUserSecurityContextCache cache, Map<String, UserSecurityContext> values) {
        values.entrySet().stream().forEach(entry -> cache.put(entry.getKey(), entry.getValue()));
    }

    private void assertValuesAreCached(Map<String, UserSecurityContext> values,
            ExpiringUserSecurityContextCache cache) {
        for (Entry<String, UserSecurityContext> entry : values.entrySet()) {
            assertThat(cache.get(entry.getKey()), is(entry.getValue()));
        }
    }

    private void assertValuesAreNotCached(Map<String, UserSecurityContext> values,
            ExpiringUserSecurityContextCache cache) {
        for (Entry<String, UserSecurityContext> entry : values.entrySet()) {
            assertThat(cache.get(entry.getKey()), is(nullValue()));
        }
    }

    @Test
    public void cachedValuesAreReturned() {
        ExpiringUserSecurityContextCache cache = createCache(ONE_HOUR);
        Map<String, UserSecurityContext> values = createValues(MAX_SIZE);
        addValues(cache, values);
        assertValuesAreCached(values, cache);
    }

    @Test
    public void nonCachedValuesAreNotReturned() {
        ExpiringUserSecurityContextCache cache = createCache(ONE_HOUR);
        Map<String, UserSecurityContext> values = createValues(MAX_SIZE);
        assertValuesAreNotCached(values, cache);
    }

    @Test
    public void clearedValuesAreNotReturned() {
        ExpiringUserSecurityContextCache cache = createCache(ONE_HOUR);
        Map<String, UserSecurityContext> values = createValues(MAX_SIZE);
        addValues(cache, values);
        cache.clear();
        assertValuesAreNotCached(values, cache);
    }

    @Test
    public void eldestEntriesAreRemovedWhenMaxSizeIsExceeded() {
        ExpiringUserSecurityContextCache cache = createCache(ONE_HOUR);
        int removed = 20;
        Map<String, UserSecurityContext> values = createValues(MAX_SIZE + removed);
        addValues(cache, values);

        Map<String, UserSecurityContext> removedValues = new LinkedHashMap<>();
        Map<String, UserSecurityContext> cachedValues = new LinkedHashMap<>();

        int i = 0;
        for (Entry<String, UserSecurityContext> entry : values.entrySet()) {
            if (i < removed) {
                removedValues.put(entry.getKey(), entry.getValue());
            } else {
                cachedValues.put(entry.getKey(), entry.getValue());
            }
            i++;
        }

        assertValuesAreNotCached(removedValues, cache);
        assertValuesAreCached(cachedValues, cache);
    }

    @Test
    public void expiredEntriesAreRemoved() {
        ExpiringUserSecurityContextCache cache = createCache(Duration.ZERO);
        Map<String, UserSecurityContext> values = createValues(MAX_SIZE);
        addValues(cache, values);

        for (int i = 0; i < CLEANUP_FREQUENCY; i++) {
            cache.get("key");
        }

        assertValuesAreNotCached(values, cache);
    }
}
