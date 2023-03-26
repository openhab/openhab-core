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
package org.openhab.core.voice.internal.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.cache.lru.InputStreamCacheWrapper;
import org.openhab.core.cache.lru.LRUMediaCache;
import org.openhab.core.cache.lru.LRUMediaCacheEntry;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.TTSCache;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.internal.VoiceManagerImpl;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache system to avoid requesting {@link TTSService} for the same utterances.
 * This is a LRU cache (least recently used entry is evicted if the size
 * is exceeded)
 * Size is based on the size on disk (in bytes)
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@Component(configurationPid = VoiceManagerImpl.CONFIGURATION_PID)
@NonNullByDefault
public class TTSLRUCacheImpl implements TTSCache {

    private final Logger logger = LoggerFactory.getLogger(TTSLRUCacheImpl.class);

    // a small default cache size for all the TTS services (in kB)
    private static final long DEFAULT_CACHE_SIZE_TTS = 10240;

    static final String CONFIG_CACHE_SIZE_TTS = "cacheSizeTTS";
    static final String CONFIG_ENABLE_CACHE_TTS = "enableCacheTTS";

    static final String VOICE_TTS_CACHE_PID = "org.openhab.voice.tts";

    private @Nullable LRUMediaCache<AudioFormatInfo> lruMediaCache;

    /**
     * The size limit, in bytes. The size is not a hard one, because the final size of the
     * current request is not known and may or may not exceed the limit.
     */
    protected long cacheSizeTTS = DEFAULT_CACHE_SIZE_TTS * 1024;
    protected boolean enableCacheTTS = true;

    private StorageService storageService;

    /**
     * Constructs a cache system for TTS result.
     */
    @Activate
    public TTSLRUCacheImpl(@Reference StorageService storageService, Map<String, Object> config) {
        this.storageService = storageService;
        modified(config);
    }

    /**
     * @param config Informations about the size of the cache in kB, and to enable the cache or not. The size is not a
     *            hard one, because the final size of the current request is not known and may exceed the limit.
     * @throws IOException when we cannot create the cache directory or if we have not enough space (*2 security margin)
     */
    @Modified
    protected void modified(Map<String, Object> config) {
        this.enableCacheTTS = ConfigParser.valueAsOrElse(config.get(CONFIG_ENABLE_CACHE_TTS), Boolean.class, true);
        this.cacheSizeTTS = ConfigParser.valueAsOrElse(config.get(CONFIG_CACHE_SIZE_TTS), Long.class,
                DEFAULT_CACHE_SIZE_TTS) * 1024;

        if (enableCacheTTS) {
            this.lruMediaCache = new LRUMediaCache<>(storageService, cacheSizeTTS, VOICE_TTS_CACHE_PID,
                    this.getClass().getClassLoader());
        }
    }

    @Override
    public AudioStream get(CachedTTSService tts, String text, Voice voice, AudioFormat requestedFormat)
            throws TTSException {

        LRUMediaCache<AudioFormatInfo> lruMediaCacheLocal = lruMediaCache;
        if (!enableCacheTTS || lruMediaCacheLocal == null) {
            return tts.synthesizeForCache(text, voice, requestedFormat);
        }

        String key = tts.getClass().getSimpleName() + "_" + tts.getCacheKey(text, voice, requestedFormat);

        LRUMediaCacheEntry<AudioFormatInfo> fileAndMetadata;
        try {
            fileAndMetadata = lruMediaCacheLocal.get(key, () -> {
                try {
                    AudioStream audioInputStream = tts.synthesizeForCache(text, voice, requestedFormat);
                    return new LRUMediaCacheEntry<AudioFormatInfo>(key, audioInputStream,
                            new AudioFormatInfo(audioInputStream.getFormat()));
                } catch (TTSException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException re) {
            if (re.getCause() != null && re.getCause() instanceof TTSException ttse) {
                throw ttse;
            } else {
                throw re;
            }
        }

        try {
            InputStream inputStream = fileAndMetadata.getInputStream();
            AudioFormatInfo metadata = fileAndMetadata.getMetadata();
            if (metadata == null) {
                throw new IllegalStateException("Cannot have an audio input stream without audio format information");
            }
            if (inputStream instanceof InputStreamCacheWrapper inputStreamCacheWrapper) {
                // we are sure that the cache is used, and so we can use an AudioStream
                // implementation that use convenient methods for some client, like getClonedStream()
                // or mark /reset
                return new AudioStreamFromCache(inputStreamCacheWrapper, metadata);
            } else {
                // the cache is not used, we can use the original response AudioStream
                return (AudioStream) fileAndMetadata.getInputStream();
            }
        } catch (IOException e) {
            logger.debug("Cannot get audio from cache, fallback to TTS service", e);
            return tts.synthesizeForCache(text, voice, requestedFormat);
        }
    }
}
