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
package org.openhab.core.automation.module.script.rulesupport.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.module.script.rulesupport.shared.ValueCache;

/**
 * The {@link CacheScriptExtensionTest} contains tests for {@link CacheScriptExtension}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class CacheScriptExtensionTest {
    private static final String SCRIPT1 = "script1";
    private static final String SCRIPT2 = "script2";

    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";

    private static final String VALUE1 = "value1";
    private static final String VALUE2 = "value2";

    @Test
    public void sharedCacheBasicFunction() {
        CacheScriptExtension se = new CacheScriptExtension();
        ValueCache cache = getCache(se, SCRIPT1, CacheScriptExtension.SHARED_CACHE_NAME);

        testCacheBasicFunctions(cache);
    }

    @Test
    public void privateCacheBasicFunction() {
        CacheScriptExtension se = new CacheScriptExtension();
        ValueCache cache = getCache(se, SCRIPT1, CacheScriptExtension.PRIVATE_CACHE_NAME);

        testCacheBasicFunctions(cache);
    }

    @Test
    public void sharedCacheIsSharedBetweenTwoRuns() {
        CacheScriptExtension se = new CacheScriptExtension();
        ValueCache cache1 = getCache(se, SCRIPT1, CacheScriptExtension.SHARED_CACHE_NAME);
        Objects.requireNonNull(cache1);

        cache1.put(KEY1, VALUE1);
        assertThat(cache1.get(KEY1), is(VALUE1));

        ValueCache cache2 = getCache(se, SCRIPT1, CacheScriptExtension.SHARED_CACHE_NAME);
        assertThat(cache2, not(sameInstance(cache1)));

        assertThat(cache2.get(KEY1), is(VALUE1));
    }

    @Test
    public void sharedCacheIsClearedIfScriptUnloaded() {
        CacheScriptExtension se = new CacheScriptExtension();
        ValueCache cache1 = getCache(se, SCRIPT1, CacheScriptExtension.SHARED_CACHE_NAME);

        cache1.put(KEY1, VALUE1);
        assertThat(cache1.get(KEY1), is(VALUE1));

        se.unload(SCRIPT1);

        ValueCache cache1new = getCache(se, SCRIPT2, CacheScriptExtension.SHARED_CACHE_NAME);
        assertThat(cache1new.get(KEY1), nullValue());
    }

    @Test
    public void sharedCachesIsSharedBetweenTwoScripts() {
        CacheScriptExtension se = new CacheScriptExtension();
        ValueCache cache1 = getCache(se, SCRIPT1, CacheScriptExtension.SHARED_CACHE_NAME);
        ValueCache cache2 = getCache(se, SCRIPT2, CacheScriptExtension.SHARED_CACHE_NAME);

        assertThat(cache1, not(is(cache2)));

        cache1.put(KEY1, VALUE1);
        assertThat(cache1.get(KEY1), is(VALUE1));
        assertThat(cache2.get(KEY1), is(VALUE1));

        cache2.remove(KEY1);
        assertThat(cache2.get(KEY1), nullValue());
        assertThat(cache1.get(KEY1), nullValue());
    }

    @Test
    public void privateCacheIsSharedBetweenTwoRuns() {
        CacheScriptExtension se = new CacheScriptExtension();
        ValueCache cache1 = getCache(se, SCRIPT1, CacheScriptExtension.PRIVATE_CACHE_NAME);

        cache1.put(KEY1, VALUE1);
        assertThat(cache1.get(KEY1), is(VALUE1));

        ValueCache cache2 = getCache(se, SCRIPT1, CacheScriptExtension.PRIVATE_CACHE_NAME);
        assertThat(cache2, sameInstance(cache1));

        assertThat(cache2.get(KEY1), is(VALUE1));
    }

    @Test
    public void privateCacheIsClearedIfScriptUnloaded() {
        CacheScriptExtension se = new CacheScriptExtension();
        ValueCache cache1 = getCache(se, SCRIPT1, CacheScriptExtension.PRIVATE_CACHE_NAME);

        cache1.put(KEY1, VALUE1);
        assertThat(cache1.get(KEY1), is(VALUE1));

        se.unload(SCRIPT1);

        ValueCache cache1new = getCache(se, SCRIPT2, CacheScriptExtension.PRIVATE_CACHE_NAME);
        assertThat(cache1new, not(sameInstance(cache1)));
        assertThat(cache1new.get(KEY1), nullValue());
    }

    @Test
    public void privateCachesIsNotSharedBetweenTwoScripts() {
        CacheScriptExtension se = new CacheScriptExtension();
        ValueCache cache1 = getCache(se, SCRIPT1, CacheScriptExtension.PRIVATE_CACHE_NAME);
        ValueCache cache2 = getCache(se, SCRIPT2, CacheScriptExtension.PRIVATE_CACHE_NAME);

        assertThat(cache1, not(is(cache2)));

        cache1.put(KEY1, VALUE1);
        assertThat(cache1.get(KEY1), is(VALUE1));
        assertThat(cache2.get(KEY1), nullValue());
    }

    @Test
    public void jobsInSharedCacheAreCancelledOnUnload() {
        testJobCancellation(CacheScriptExtension.SHARED_CACHE_NAME);
    }

    @Test
    public void jobsInPrivateCacheAreCancelledOnUnload() {
        testJobCancellation(CacheScriptExtension.PRIVATE_CACHE_NAME);
    }

    public void testJobCancellation(String cacheType) {
        CacheScriptExtension se = new CacheScriptExtension();
        ValueCache cache = getCache(se, SCRIPT1, cacheType);

        Timer timerMock = mock(Timer.class);
        ScheduledFuture<?> futureMock = mock(ScheduledFuture.class);

        cache.put(KEY1, timerMock);
        cache.put(KEY2, futureMock);

        // ensure jobs are not cancelled on removal
        cache.remove(KEY1);
        cache.remove(KEY2);
        verifyNoMoreInteractions(timerMock, futureMock);

        cache.put(KEY1, timerMock);
        cache.put(KEY2, futureMock);
        se.unload(SCRIPT1);
        verify(timerMock, timeout(1000)).cancel();
        verify(futureMock, timeout(1000)).cancel(true);
    }

    public void testCacheBasicFunctions(ValueCache cache) {
        // cache is initially empty
        assertThat(cache.get(KEY1), nullValue());

        // return value is null if no value before and new value can be retrieved
        assertThat(cache.put(KEY1, VALUE1), nullValue());
        assertThat(cache.get(KEY1), is(VALUE1));

        // value returns old value on update and updated value can be retrieved
        assertThat(cache.put(KEY1, VALUE2), is(VALUE1));
        assertThat(cache.get(KEY1), is(VALUE2));

        // old value is returned on removal and cache empty afterwards
        assertThat(cache.remove(KEY1), is(VALUE2));
        assertThat(cache.get(KEY1), nullValue());

        // new value is inserted from supplier
        assertThat(cache.get(KEY1, () -> VALUE1), is(VALUE1));
        assertThat(cache.get(KEY1), is(VALUE1));

        // different keys return different values
        cache.put(KEY2, VALUE2);
        assertThat(cache.get(KEY1), is(VALUE1));
        assertThat(cache.get(KEY2), is(VALUE2));
    }

    private ValueCache getCache(CacheScriptExtension se, String scriptIdentifier, String type) {
        ValueCache cache = (ValueCache) se.get(scriptIdentifier, type);
        Objects.requireNonNull(cache);

        return cache;
    }
}
