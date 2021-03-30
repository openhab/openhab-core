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
package org.openhab.core.io.rest.auth.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class provides a cache for up to 10 UserSecurityContexts.
 * Entries have a lifetime and are removed from the cache upon the next
 * get call.
 *
 * @author Kai Kreuzer - Initial contribution
 *
 */
@NonNullByDefault
public class ExpiringUserSecurityContextCache {
    final static private int MAX_SIZE = 10;
    final static private int CLEANUP_FREQUENCY = 10;

    final private long keepPeriod;
    final private Map<String, Entry> entryMap;

    private int calls = 0;

    ExpiringUserSecurityContextCache(long expirationTime) {
        this.keepPeriod = expirationTime;
        entryMap = new LinkedHashMap<>() {
            private static final long serialVersionUID = -1220310861591070462L;

            protected boolean removeEldestEntry(Map.@Nullable Entry<String, Entry> eldest) {
                return size() > MAX_SIZE;
            }
        };
    }

    synchronized @Nullable UserSecurityContext get(String key) {
        calls++;
        if (calls >= CLEANUP_FREQUENCY) {
            entryMap.keySet().forEach(k -> getEntry(k));
            calls = 0;
        }
        Entry entry = getEntry(key);
        if (entry != null) {
            return entry.value;
        }
        return null;
    }

    synchronized void put(String key, UserSecurityContext value) {
        entryMap.put(key, new Entry(System.currentTimeMillis(), value));
    }

    synchronized void clear() {
        entryMap.clear();
    }

    private @Nullable Entry getEntry(String key) {
        Entry entry = entryMap.get(key);
        if (entry != null) {
            final long curTimeMillis = System.currentTimeMillis();
            long entryAge = curTimeMillis - entry.timestamp;
            if (entryAge < 0 || entryAge >= keepPeriod) {
                entryMap.remove(key);
                entry = null;
            } else {
                entry.timestamp = curTimeMillis;
            }
        }
        return entry;
    }

    static class Entry {
        public long timestamp;
        final public UserSecurityContext value;

        Entry(long timestamp, UserSecurityContext value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}
