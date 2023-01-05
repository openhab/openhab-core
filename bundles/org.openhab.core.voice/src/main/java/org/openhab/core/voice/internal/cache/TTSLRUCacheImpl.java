/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.voice.TTSCache;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.internal.VoiceManagerImpl;
import org.openhab.core.voice.internal.cache.TTSResult.AudioFormatInfo;
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
@Component(immediate = false, configurationPid = VoiceManagerImpl.CONFIGURATION_PID)
@NonNullByDefault
public class TTSLRUCacheImpl implements TTSCache {

    private final Logger logger = LoggerFactory.getLogger(TTSLRUCacheImpl.class);

    // a small default cache size for all the TTS services (in kB)
    private static final long DEFAULT_CACHE_SIZE_TTS = 10240;

    static final String CONFIG_CACHE_SIZE_TTS = "cacheSizeTTS";
    static final String CONFIG_ENABLE_CACHE_TTS = "enableCacheTTS";

    public static final String SOUND_EXT = ".snd";

    static final String VOICE_TTS_CACHE_PID = "org.openhab.voice.tts";
    private static final String CACHE_FOLDER_NAME = "cache";

    final LinkedHashMap<String, @Nullable TTSResult> ttsResultMap;

    /**
     * Lock the cache to handle concurrency
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * The size limit, in bytes. The size is not a hard one, because the final size of the
     * current request is not known and may or may not exceed the limit.
     */
    protected long cacheSizeTTS = DEFAULT_CACHE_SIZE_TTS * 1024;
    protected boolean enableCacheTTS = true;

    private final Path cacheFolder;

    private Storage<AudioFormatInfo> storage;

    /**
     * Constructs a cache system for TTS result.
     */
    @Activate
    public TTSLRUCacheImpl(@Reference StorageService storageService, Map<String, Object> config) {
        this.storage = storageService.getStorage(VOICE_TTS_CACHE_PID, this.getClass().getClassLoader());
        this.ttsResultMap = new LinkedHashMap<>(20, .75f, true);
        cacheFolder = Path.of(OpenHAB.getUserDataFolder(), CACHE_FOLDER_NAME, VOICE_TTS_CACHE_PID);
        activate(config);
    }

    /**
     * @param config Informations about the size of the cache in kB, and to enable the cache or not. The size is not a
     *            hard one, because the final size of the current request is not known and may exceed the limit.
     * @throws IOException when we cannot create the cache directory or if we have not enough space (*2 security margin)
     */
    @Modified
    protected void activate(Map<String, Object> config) {
        this.enableCacheTTS = ConfigParser.valueAsOrElse(config.get(CONFIG_ENABLE_CACHE_TTS), Boolean.class, true);
        this.cacheSizeTTS = ConfigParser.valueAsOrElse(config.get(CONFIG_CACHE_SIZE_TTS), Long.class,
                DEFAULT_CACHE_SIZE_TTS) * 1024;

        if (enableCacheTTS) {
            try {
                // creating directory if needed :
                logger.debug("Creating TTS cache folder '{}'", cacheFolder);
                Files.createDirectories(cacheFolder);

                // check if we have enough space :
                if (getFreeSpace() < (cacheSizeTTS * 2)) {
                    enableCacheTTS = false;
                    logger.warn("Not enough space for the TTS cache");
                }
                logger.debug("Using TTS cache folder '{}'", cacheFolder);

                cleanCacheDirectory();
                loadAll();
            } catch (IOException | SecurityException e) {
                logger.error("Cannot initialize the TTS cache folder. Reason: {}", e.getMessage());
                enableCacheTTS = false;
            }
        }
    }

    private void cleanCacheDirectory() throws IOException {
        try {
            List<@Nullable Path> filesInCacheFolder = Files.list(cacheFolder).collect(Collectors.toList());

            // 1 delete empty files
            for (Path path : filesInCacheFolder) {
                if (path != null) {
                    File file = path.toFile();
                    if (file.length() == 0) {
                        file.delete();
                    }
                }
            }

            // 2 clean orphan (part of a pair (sound + info files) without a corresponding partner)
            // 2-a delete sound files without AudioFormatInfo
            for (Path path : filesInCacheFolder) {
                if (path != null) {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(SOUND_EXT)) {
                        String fileNameWithoutExtension = fileName.replaceAll("\\.\\w+$", "");
                        ;
                        // check corresponding AudioFormatInfo in storage
                        AudioFormatInfo audioFormatInfo = storage.get(fileNameWithoutExtension);
                        if (audioFormatInfo == null) {
                            Files.delete(path);
                        }
                    }
                }
            }
            // 2-b delete AudioFormatInfo without corresponding sound file
            for (Entry<String, @Nullable AudioFormatInfo> entry : storage.stream().toList()) {
                Path correspondingAudioFile = cacheFolder.resolve(entry.getKey() + SOUND_EXT);
                if (!Files.exists(correspondingAudioFile)) {
                    storage.remove(entry.getKey());
                }
            }
        } catch (

        IOException e) {
            logger.warn("Cannot load the TTS cache directory : {}", e.getMessage());
            return;
        }
    }

    private long getFreeSpace() {
        try {
            URI rootURI = new URI("file:///");
            Path rootPath = Paths.get(rootURI);
            Path dirPath = rootPath.resolve(cacheFolder.getParent());
            FileStore dirFileStore = Files.getFileStore(dirPath);
            return dirFileStore.getUsableSpace();
        } catch (URISyntaxException | IOException e) {
            logger.error("Cannot compute free disk space for the TTS cache. Reason: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public AudioStream get(CachedTTSService tts, String text, Voice voice, AudioFormat requestedFormat)
            throws TTSException {
        if (!enableCacheTTS) {
            return tts.synthesizeForCache(text, voice, requestedFormat);
        }

        // initialize the supplier stream from the TTS service tts
        AudioStreamSupplier ttsSynthesizerSupplier = new AudioStreamSupplier(tts, text, voice, requestedFormat);
        lock.lock(); // (a get operation also need the lock as it will update the head of the cache)
        try {
            String key = tts.getClass().getSimpleName() + "_" + tts.getCacheKey(text, voice, requestedFormat);
            // try to get from cache
            TTSResult ttsResult = ttsResultMap.get(key);
            if (ttsResult == null || !ttsResult.getText().equals(text)) { // it's a cache miss or a false positive, we
                // must (re)create it
                logger.debug("Cache miss {}", key);
                ttsResult = new TTSResult(cacheFolder, key, storage, ttsSynthesizerSupplier);
                ttsResultMap.put(key, ttsResult);
            } else {
                logger.debug("Cache hit {}", key);
            }
            return ttsResult.getAudioStream(ttsSynthesizerSupplier);
        } finally {
            lock.unlock();
        }
    }

    void put(@Nullable TTSResult ttsResult) {
        if (ttsResult != null) {
            ttsResultMap.put(ttsResult.getKey(), ttsResult);
        }
        makeSpace();
    }

    /**
     * Load all {@link TTSResult} cached to the disk.
     */
    private void loadAll() throws IOException {
        ttsResultMap.clear();
        storage.stream().map(entry -> new TTSResult(cacheFolder, entry.getKey(), storage))
                .filter(ttsR -> !ttsR.getText().isBlank()).forEach(this::put);
    }

    /**
     * Check if the cache is not already full and make space if needed.
     * We don't use the removeEldestEntry test method from the linkedHashMap because it can only remove one element.
     */
    private void makeSpace() {
        Iterator<@Nullable TTSResult> iterator = ttsResultMap.values().iterator();
        Long cacheSize = ttsResultMap.values().stream().map(ttsR -> ttsR == null ? 0 : ttsR.getCurrentSize()).reduce(0L,
                (Long::sum));
        while (cacheSize > cacheSizeTTS && ttsResultMap.size() > 1) {
            TTSResult oldestEntry = iterator.next();
            if (oldestEntry != null) {
                oldestEntry.deleteFiles();
                cacheSize -= oldestEntry.getTotalSize();
            }
            iterator.remove();
        }
    }
}
