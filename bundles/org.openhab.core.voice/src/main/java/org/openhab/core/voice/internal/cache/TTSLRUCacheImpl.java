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
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.voice.TTSCache;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.internal.VoiceManagerImpl;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
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

    public static final String INFO_EXT = ".info";
    public static final String SOUND_EXT = ".snd";

    private static final String VOICE_TTS_CACHE_PID = "org.openhab.voice.tts";
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

    /**
     * Constructs a cache system for TTS result.
     */
    public TTSLRUCacheImpl() {
        this.ttsResultMap = new LinkedHashMap<>(20, .75f, true);
        cacheFolder = Path.of(OpenHAB.getUserDataFolder(), CACHE_FOLDER_NAME, VOICE_TTS_CACHE_PID);
    }

    /**
     * @param config Informations about the size of the cache in kB, and to enable the cache or not. The size is not a
     *            hard one, because the final size of the current request is not known and may exceed the limit.
     * @throws IOException when we cannot create the cache directory or if we have not enough space (*2 security margin)
     */
    @Activate
    @Modified
    protected void activate(Map<String, Object> config) throws IOException {
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
                    throw new IOException("Not enough space for the TTS cache");
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

    private void cleanCacheDirectory() {
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
            for (Path path : filesInCacheFolder) {
                if (path != null) {
                    File file = path.toFile();
                    Optional<String> extension = Optional.ofNullable(file.getName()).filter(f -> f.contains("."))
                            .map(f -> f.substring(file.getName().lastIndexOf(".")));
                    if (extension.isPresent()) {
                        String fileNameWithoutExtension = file.getName().replaceAll("\\.\\w+$", "");
                        String otherExt;
                        if (extension.get().equals(INFO_EXT)) {
                            otherExt = SOUND_EXT;
                        } else if (extension.get().equals(SOUND_EXT)) {
                            otherExt = INFO_EXT;
                        } else {
                            continue;
                        }
                        if (!Files.exists(cacheFolder.resolve(fileNameWithoutExtension + otherExt))) {
                            file.delete();
                        }
                    }
                }
            }
        } catch (IOException e) {
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
    public AudioStream getOrSynthetize(TTSCachedService tts, String text, Voice voice, AudioFormat requestedFormat)
            throws TTSException {
        if (!enableCacheTTS) {
            return tts.synthesizeForCache(text, voice, requestedFormat);
        }

        // initialize the supplier stream from the TTS service tts
        AudioStreamSupplier ttsSynthetizerSupplier = new AudioStreamSupplier(tts, text, voice, requestedFormat);
        lock.lock(); // (a get operation also need the lock as it will update the head of the cache)
        try {
            String key = tts.getClass().getSimpleName() + "_" + tts.getCacheKey(text, voice, requestedFormat);
            // try to get from cache
            TTSResult ttsResult = ttsResultMap.get(key);
            if (ttsResult == null || !ttsResult.getText().equals(text)) { // it's a cache miss or a false positive, we
                // must (re)create it
                logger.debug("Cache miss {}", key);
                ttsResult = new TTSResult(cacheFolder, key, ttsSynthetizerSupplier);
                ttsResultMap.put(key, ttsResult);
            } else {
                logger.debug("Cache hit {}", key);
            }
            return ttsResult.getAudioStreamClient(ttsSynthetizerSupplier);
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
    private void loadAll() {
        ttsResultMap.clear();

        try (Stream<Path> stream = Files.list(cacheFolder)) {
            // Load the TTSResult from cache directory, order by date
            stream.filter(path -> path.getFileName().toString().endsWith(INFO_EXT)).map(Path::toFile)
                    .sorted((file1, file2) -> Long.valueOf(file1.lastModified() - file2.lastModified()).intValue())
                    .map(this::buildFromFile).forEachOrdered(this::put);
        } catch (IOException e) {
            logger.warn("Cannot load the TTS cache directory : {}", e.getMessage());
        }
    }

    private @Nullable TTSResult buildFromFile(File file) {
        String fileNameWithoutExtension = file.getName().replaceAll("\\.\\w+$", "");
        try {
            return new TTSResult(cacheFolder, fileNameWithoutExtension);
        } catch (IOException e) {
            logger.info("Cannot get TTS Result cache from file {}", file.getName());
            return null;
        }
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