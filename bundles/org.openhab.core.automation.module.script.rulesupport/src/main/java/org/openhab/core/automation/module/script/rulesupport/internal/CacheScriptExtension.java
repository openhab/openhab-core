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
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.openhab.core.automation.module.script.rulesupport.shared.LockingCache;
import org.openhab.core.automation.module.script.rulesupport.shared.ObjectCache;
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
    static final String OBJECT_CACHE_NAME = "objectCache";

    private final Logger logger = LoggerFactory.getLogger(CacheScriptExtension.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);

    private final Lock cacheLock = new ReentrantLock();
    private final Map<String, Object> sharedCache = new HashMap<>();
    private final ObjectCache objectCache = new ObjectCacheImpl();
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
        return Set.of(PRIVATE_CACHE_NAME, SHARED_CACHE_NAME, OBJECT_CACHE_NAME);
    }

    @Override
    public @Nullable Object get(String scriptIdentifier, String type) throws IllegalArgumentException {
        switch (type) {
            case SHARED_CACHE_NAME:
                return new TrackingValueCacheImpl(scriptIdentifier);
            case PRIVATE_CACHE_NAME:
                return privateCaches.computeIfAbsent(scriptIdentifier, ValueCacheImpl::new);
            case OBJECT_CACHE_NAME:
                return objectCache;
            default:
                return null;
        }
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if (PRESET_NAME.equals(preset)) {
            Object privateCache = Objects
                    .requireNonNull(privateCaches.computeIfAbsent(scriptIdentifier, ValueCacheImpl::new));
            return Map.of(SHARED_CACHE_NAME, new TrackingValueCacheImpl(scriptIdentifier), PRIVATE_CACHE_NAME,
                    privateCache, OBJECT_CACHE_NAME, objectCache);
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
        if (o instanceof ScheduledFuture future) {
            cancelJob = () -> future.cancel(true);
        } else if (o instanceof java.util.Timer timer) {
            cancelJob = () -> timer.cancel();
        } else if (o instanceof org.openhab.core.automation.module.script.action.Timer timer) {
            cancelJob = () -> timer.cancel();
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

        @Override
        @Nullable
        public Object compute(String key, BiFunction<String, @Nullable Object, @Nullable Object> remappingFunction) {
            return cache.compute(key, (k, v) -> remappingFunction.apply(k, v));
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

        @Override
        @Nullable
        public Object compute(String key, BiFunction<String, @Nullable Object, @Nullable Object> remappingFunction) {
            cacheLock.lock();
            try {
                Object value = sharedCache.compute(key, (k, v) -> remappingFunction.apply(k, v));
                if (value == null) {
                    sharedCacheKeyAccessors.remove(key);
                } else {
                    rememberAccessToKey(key);
                }
                logger.trace("COMPUTE to cache from '{}': '{}' -> '{}'", scriptIdentifier, key, value);
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

    private static class ObjectCacheImpl implements ObjectCache {
        // All access must be guarded by "cache"
        private final Map<String, String> cache = new HashMap<>();

        @Override
        public @Nullable String put(String key, String serializedObject) {
            synchronized (cache) {
                return cache.put(key, serializedObject);
            }
        }

        @Override
        public @Nullable String remove(String key) {
            synchronized (cache) {
                return cache.remove(key);
            }
        }

        @Override
        public @Nullable String get(String key) {
            synchronized (cache) {
                return cache.get(key);
            }
        }

        @Override
        public String get(String key, Supplier<String> supplier) {
            synchronized (cache) {
                return Objects.requireNonNull(cache.computeIfAbsent(key, k -> supplier.get()));
            }
        }

        @Override
        public @Nullable String compute(String key,
                BiFunction<String, @Nullable String, @Nullable String> remappingFunction) {
            synchronized (cache) {
                return cache.compute(key, (k, v) -> remappingFunction.apply(k, v));
            }
        }
    }

    /**
     * A locking cache where individual entries can be locked for exclusive access for the duration
     * of an operation. This requires explicit unlocking when the operation is over, and the potential
     * to cause deadlocks if unlocking isn't done for certain code paths.
     *
     * @implNote Getting the locking right is a bit of a challenge, especially in relation to removal
     *           of cache entries. Then an entry is removed, the lock instance is also removed, making it unavailable
     *           for further unlocks. To avoid locks remaining locked with no way to unlock,
     *           {@link LockingCacheValue#removed}
     *           is used to track removal. As soon as the entry has been removed from the cache, this flag is set.
     *           Any code that acquires locks are required to check this flag immediately after acquiring the lock,
     *           and if it's set, immediately unlock all lock levels held by that thread. Because the thread
     *           that performs the removal must acquire the lock to complete the removal operation, any queued
     *           threads waiting for the lock should thus abort and release immediately if they happen to acquire
     *           the lock after the entry has been removed.
     */
    private static class LockingCacheImpl implements LockingCache {

        // All access must be guarded by "cache"
        private final Map<String, LockingCacheValue> cache = new HashMap<>();

        @Override
        public @Nullable Object put(String key, Object object) {
            LockingCacheValue value;
            boolean created = false;
            synchronized (cache) {
                value = cache.get(key);
                if (value == null || value.removed) {
                    created = true;
                    value = new LockingCacheValue();
                    value.lock.lock();
                    cache.put(key, value);
                }
            }
            Object result;
            if (!created) {
                value.lock.lock();
            }
            try {
                result = value.object;
                value.object = object;
            } finally {
                value.lock.unlock();
            }

            return result;
        }

        @Override
        public @Nullable Object lockAndPut(String key, Object object) {
            LockingCacheValue value;
            boolean created = false;
            synchronized (cache) {
                value = cache.get(key);
                if (value == null || value.removed) {
                    created = true;
                    value = new LockingCacheValue();
                    value.lock.lock();
                    cache.put(key, value);
                }
            }
            Object result;
            if (!created) {
                value.lock.lock();
            }
            result = value.object;
            value.object = object;
            return result;
        }

        @Override
        public @Nullable Object remove(String key) {
            LockingCacheValue value;
            synchronized (cache) {
                value = cache.remove(key);
                if (value == null) {
                    return null;
                }
                value.removed = true;
            }
            Object result;
            value.lock.lock();
            try {
                result = value.object;
            } finally {
                // Release all holds on this lock, since we can't reacquire the lock in the future
                unlockAll(value);
            }
            return result;
        }

        @Override
        public @Nullable Object lockAndGet(String key) {
            LockingCacheValue value;
            synchronized (cache) {
                value = cache.get(key);
            }
            if (value == null) {
                return null;
            }
            value.lock.lock();
            if (value.removed) {
                unlockAll(value);
                return null;
            }
            return value.object;
        }

        @Override
        public Object lockAndGet(String key, Supplier<Object> supplier) {
            LockingCacheValue value;
            synchronized (cache) {
                value = Objects.requireNonNull(cache.computeIfAbsent(key, k -> new LockingCacheValue(supplier.get())));
            }
            value.lock.lock();
            if (value.removed) {
                unlockAll(value);
                synchronized (cache) {
                    value = cache.get(key);
                    if (value == null || value.removed) {
                        value = new LockingCacheValue(supplier.get());
                        cache.put(key, value);
                    }
                }
                value.lock.lock();
            }
            return Objects.requireNonNull(value.object);
        }

        @Override
        public void unlock(String key) {
            LockingCacheValue value;
            synchronized (cache) {
                value = cache.get(key);
            }
            if (value == null) {
                return;
            }
            if (value.removed) {
                unlockAll(value);
            } else {
                try {
                    value.lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    // Ignore
                }
            }
        }

        private void unlockAll(LockingCacheValue value) {
            int holdCount = value.lock.getHoldCount();
            for (int i = 0; i < holdCount; i++) {
                try {
                    value.lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    break;
                }
            }
        }
    }

    private static class LockingCacheValue {
        public final ReentrantLock lock = new ReentrantLock();
        public @Nullable Object object;
        public volatile boolean removed;

        public LockingCacheValue() {
        }

        public LockingCacheValue(@Nullable Object object) {
            this.object = object;
        }
    }
}
