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
package org.openhab.core.graal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.Nullable;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class with Graal-related functionality.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class GraalUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraalUtil.class);
    private static final Object LANGUAGE_INIT_LOCK = new Object();
    private static final Map<String, Language> CACHED_LANGUAGES = new ConcurrentHashMap<>();

    /**
     * Thread-safe retrieval of a language from an engine.
     * Ensures that language initialization happens serially, preventing race conditions
     * in Graal's internal language cache.
     */
    public static @Nullable Language getLanguage(Engine engine, String languageId) {
        Language cached = CACHED_LANGUAGES.get(languageId);
        if (cached != null) {
            return cached;
        }

        synchronized (LANGUAGE_INIT_LOCK) {
            // Check again after acquiring lock (another thread may have just populated it)
            cached = CACHED_LANGUAGES.get(languageId);
            if (cached != null) {
                return cached;
            }

            try {
                Language lang = engine.getLanguages().get(languageId);
                if (lang != null) {
                    CACHED_LANGUAGES.put(languageId, lang);
                }
                return lang;
            } catch (RuntimeException e) {
                LOGGER.error("Graal: Failed to initialize language: {}", languageId, e);
                return null;
            }
        }
    }

    /**
     * Reset the cache (useful for testing or if bundles are dynamically unloaded/reloaded).
     */
    public static void clearCache() {
        synchronized (LANGUAGE_INIT_LOCK) {
            CACHED_LANGUAGES.clear();
        }
    }
}
