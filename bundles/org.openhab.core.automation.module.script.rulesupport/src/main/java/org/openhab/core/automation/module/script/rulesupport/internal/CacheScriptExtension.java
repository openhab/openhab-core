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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.openhab.core.automation.module.script.rulesupport.shared.ValueCache;
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CacheScriptExtension} extends scripts to use a cache shared between rules or subsequent runs of the same
 * rule
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class CacheScriptExtension implements ScriptExtensionProvider {
    static final String PRESET_NAME = "cache";
    static final String SHARED_CACHE_NAME = "sharedCache";
    static final String PRIVATE_CACHE_NAME = "privateCache";

    private final Logger logger = LoggerFactory.getLogger(CacheScriptExtension.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);

    private final Lock cacheLock = new ReentrantLock();
    private final Map<String, Object> sharedCache = new HashMap<>();
    private final Map<String, Set<String>> sharedCacheKeyAccessors = new ConcurrentHashMap<>();

    private final Map<String, ValueCacheImpl> privateCaches = new ConcurrentHashMap<>();

    public CacheScriptExtension() {
    }

    @Override
    public Collection<String> getDefaultPresets() {
        return Set.of();
    }

    @Override
    public Collection<String> getPresets() {
        return Set.of(PRESET_NAME);
    }

    @Override
    public Collection<String> getTypes() {
        return Set.of(PRIVATE_CACHE_NAME, SHARED_CACHE_NAME);
    }

    @Override
    public @Nullable Object get(String scriptIdentifier, String type) throws IllegalArgumentException {
        if (SHARED_CACHE_NAME.equals(type)) {
            return new TrackingValueCacheImpl(scriptIdentifier);
        } else if (PRIVATE_CACHE_NAME.equals(type)) {
            return privateCaches.computeIfAbsent(scriptIdentifier, ValueCacheImpl::new);
        }

        return null;
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if (PRESET_NAME.equals(preset)) {
            Object privateCache = Objects
                    .requireNonNull(privateCaches.computeIfAbsent(scriptIdentifier, ValueCacheImpl::new));
            return Map.of(SHARED_CACHE_NAME, new TrackingValueCacheImpl(scriptIdentifier), PRIVATE_CACHE_NAME,
                    privateCache);
        }

        return Map.of();
    }

    @Override
    public void unload(String scriptIdentifier) {
        cacheLock.lock();
        try {
            // remove the scriptIdentifier from cache-key access list
            sharedCacheKeyAccessors.values().forEach(cacheKey -> cacheKey.remove(scriptIdentifier));
            // remove the key from access list and cache if no accessor left
            Iterator<Map.Entry<String, Set<String>>> it = sharedCacheKeyAccessors.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Set<String>> element = it.next();
                if (element.getValue().isEmpty()) {
                    // accessor list is empty
                    it.remove();
                    // remove from cache and cancel ScheduledFutures or Timer tasks
                    asyncCancelJob(sharedCache.remove(element.getKey()));
                }
            }
        } finally {
            cacheLock.unlock();
        }

        // remove private cache
        ValueCacheImpl privateCache = privateCaches.remove(scriptIdentifier);
        if (privateCache != null) {
            // cancel ScheduledFutures or Timer tasks
            privateCache.values().forEach(this::asyncCancelJob);
        }
    }

    /**
     * Check if object is {@link ScheduledFuture}, {@link java.util.Timer} or
     * {@link org.openhab.core.automation.module.script.action.Timer} and schedule cancellation of those jobs
     *
     * @param o the {@link Object} to check
     */
    private void asyncCancelJob(@Nullable Object o) {
        Runnable cancelJob = null;
        if (o instanceof ScheduledFuture) {
            cancelJob = () -> ((ScheduledFuture<?>) o).cancel(true);
        } else if (o instanceof java.util.Timer) {
            cancelJob = () -> ((java.util.Timer) o).cancel();
        } else if (o instanceof org.openhab.core.automation.module.script.action.Timer) {
            cancelJob = () -> ((org.openhab.core.automation.module.script.action.Timer) o).cancel();
        }
        if (cancelJob != null) {
            // not using execute so ensure this operates in another thread and we don't block here
            scheduler.schedule(cancelJob, 0, TimeUnit.SECONDS);
        }
    }

    private static class ValueCacheImpl implements ValueCache {
        private final Map<String, Object> cache = new HashMap<>();

        public ValueCacheImpl(String scriptIdentifier) {
        }

        @Override
        public @Nullable Object put(String key, Object value) {
            return cache.put(key, value);
        }

        @Override
        public @Nullable Object remove(String key) {
            return cache.remove(key);
        }

        @Override
        public @Nullable Object get(String key) {
            return cache.get(key);
        }

        @Override
        public Object get(String key, Supplier<Object> supplier) {
            return Objects.requireNonNull(cache.computeIfAbsent(key, k -> supplier.get()));
        }

        private Collection<Object> values() {
            return cache.values();
        }
    }

    private class TrackingValueCacheImpl implements ValueCache {
        private final String scriptIdentifier;

        public TrackingValueCacheImpl(String scriptIdentifier) {
            this.scriptIdentifier = scriptIdentifier;
        }

        @Override
        public @Nullable Object put(String key, Object value) {
            cacheLock.lock();
            try {
                rememberAccessToKey(key);
                Object oldValue = sharedCache.put(key, value);
                logger.trace("PUT to cache from '{}': '{}' -> '{}' (was: '{}')", scriptIdentifier, key, value,
                        oldValue);
                return oldValue;
            } finally {
                cacheLock.unlock();
            }
        }

        @Override
        public @Nullable Object remove(String key) {
            cacheLock.lock();
            try {
                sharedCacheKeyAccessors.remove(key);
                Object oldValue = sharedCache.remove(key);

                logger.trace("REMOVE from cache from '{}': '{}' -> '{}'", scriptIdentifier, key, oldValue);
                return oldValue;
            } finally {
                cacheLock.unlock();
            }
        }

        @Override
        public @Nullable Object get(String key) {
            cacheLock.lock();
            try {
                rememberAccessToKey(key);
                Object value = sharedCache.get(key);

                logger.trace("GET to cache from '{}': '{}' -> '{}'", scriptIdentifier, key, value);
                return value;
            } finally {
                cacheLock.unlock();
            }
        }

        @Override
        public Object get(String key, Supplier<Object> supplier) {
            cacheLock.lock();
            try {
                rememberAccessToKey(key);
                Object value = Objects.requireNonNull(sharedCache.computeIfAbsent(key, k -> supplier.get()));

                logger.trace("GET with supplier to cache from '{}': '{}' -> '{}'", scriptIdentifier, key, value);
                return value;
            } finally {
                cacheLock.unlock();
            }
        }

        private void rememberAccessToKey(String key) {
            Objects.requireNonNull(sharedCacheKeyAccessors.computeIfAbsent(key, k -> new HashSet<>()))
                    .add(scriptIdentifier);
        }
    }
}
