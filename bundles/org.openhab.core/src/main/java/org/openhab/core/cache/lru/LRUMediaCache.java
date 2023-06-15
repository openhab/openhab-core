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
package org.openhab.core.cache.lru;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache system for media files, and their metadata
 * This is a LRU cache (least recently used entry is evicted if the size
 * is exceeded).
 * Size is based on the size on disk (in bytes)
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public class LRUMediaCache<V> {

    private final Logger logger = LoggerFactory.getLogger(LRUMediaCache.class);

    private static final String CACHE_FOLDER_NAME = "cache";

    final Map<String, @Nullable LRUMediaCacheEntry<V>> cachedResults;

    /**
     * Lock to handle concurrent access to the same entry
     */
    private final Map<String, Lock> lockByEntry = new ConcurrentHashMap<>();

    /**
     * The size limit, in bytes. The size is not a hard one, because the final size of the
     * current request is not known and may or may not exceed the limit.
     */
    protected final long maxCacheSize;

    private final Path cacheFolder;

    /**
     * Store for additional informations along the media file
     */
    private Storage<V> storage;

    protected boolean cacheIsOK = true;

    /**
     * Constructs a cache system.
     *
     * @param storageService Storage service to store metadata
     * @param cacheSize Limit size, in byte
     * @param pid A pid identifying the cache on disk
     */
    public LRUMediaCache(@Reference StorageService storageService, long maxCacheSize, String pid,
            @Nullable ClassLoader clazzLoader) {
        this.storage = storageService.getStorage(pid, clazzLoader);
        this.cachedResults = Collections.synchronizedMap(new LinkedHashMap<>(20, .75f, true));
        this.cacheFolder = Path.of(OpenHAB.getUserDataFolder(), CACHE_FOLDER_NAME, pid);
        this.maxCacheSize = maxCacheSize;

        // creating directory if needed :
        logger.debug("Creating cache folder '{}'", cacheFolder);
        try {
            Files.createDirectories(cacheFolder);
            cleanCacheDirectory();
            loadAll();
        } catch (IOException e) {
            this.cacheIsOK = false;
            logger.warn("Cannot use cache directory", e);
        }

        // check if we have enough space :
        if (getFreeSpace() < (maxCacheSize * 2)) {
            cacheIsOK = false;
            logger.warn("Not enough space for the cache");
        }
        logger.debug("Using cache folder '{}'", cacheFolder);
    }

    private void cleanCacheDirectory() throws IOException {
        try (Stream<Path> files = Files.list(cacheFolder)) {
            List<Path> filesInCacheFolder = new ArrayList<>(files.toList());

            // 1 delete empty files
            Iterator<Path> fileDeleterIterator = filesInCacheFolder.iterator();
            while (fileDeleterIterator.hasNext()) {
                Path path = fileDeleterIterator.next();
                File file = path.toFile();
                if (file.length() == 0) {
                    file.delete();
                    String fileName = path.getFileName().toString();
                    storage.remove(fileName);
                    fileDeleterIterator.remove();
                }
            }

            // 2 clean orphan (part of a pair (file + metadata) without a corresponding partner)
            // 2-a delete a file without its metadata
            for (Path path : filesInCacheFolder) {
                if (path != null) {
                    String fileName = path.getFileName().toString();
                    // check corresponding metadata in storage
                    V metadata = storage.get(fileName);
                    if (metadata == null) {
                        Files.delete(path);
                    }
                }
            }
            // 2-b delete metadata without corresponding file
            for (Entry<String, @Nullable V> entry : storage.stream().toList()) {
                Path correspondingFile = cacheFolder.resolve(entry.getKey());
                if (!Files.exists(correspondingFile)) {
                    storage.remove(entry.getKey());
                }
            }
        } catch (IOException e) {
            logger.warn("Cannot load the cache directory : {}", e.getMessage());
            cacheIsOK = false;
            return;
        }
    }

    private long getFreeSpace() {
        try {
            Path rootPath = Paths.get(new URI("file:///"));
            Path dirPath = rootPath.resolve(cacheFolder.getParent());
            FileStore dirFileStore = Files.getFileStore(dirPath);
            return dirFileStore.getUsableSpace();
        } catch (URISyntaxException | IOException e) {
            logger.error("Cannot compute free disk space for the cache. Reason: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Returns a {@link LRUMediaCacheEntry} from the cache, or if not already in the cache :
     * resolve it, stores it, and returns it.
     * key A unique key identifying the result
     * supplier the data and metadata supplier. It is OK to launch a DataRetrievalException from this, as it will be
     * rethrown.
     */
    public LRUMediaCacheEntry<V> get(String key, Supplier<LRUMediaCacheEntry<V>> supplier) {
        if (!cacheIsOK) {
            return supplier.get();
        }

        // we use a lock with fine granularity, by key, to not lock the entire cache
        // when resolving the supplier (which could be time consuming)
        Lock lockForCurrentEntry = lockByEntry.computeIfAbsent(key, k -> new ReentrantLock());
        if (lockForCurrentEntry == null) {
            cacheIsOK = false;
            logger.error("Cannot compute lock within cache system. Shouldn't happen");
            return supplier.get();
        }
        lockForCurrentEntry.lock();
        try {
            // try to get from cache
            LRUMediaCacheEntry<V> result = cachedResults.get(key);
            if (result != null && result.isFaulty()) { // if previously marked as faulty
                result.deleteFile();
                cachedResults.remove(key);
                result = null;
            }
            if (result == null) { // it's a cache miss or a faulty result, we must (re)create it
                logger.debug("Cache miss {}", key);
                result = supplier.get();
                put(result);
            }
            return result;
        } finally {
            lockForCurrentEntry.unlock();
        }
    }

    protected void put(LRUMediaCacheEntry<V> result) {
        result.setCacheContext(cacheFolder, storage);
        cachedResults.put(result.getKey(), result);
        makeSpace();
    }

    /**
     * Load all {@link LRUMediaCacheEntry} cached to the disk.
     */
    private void loadAll() throws IOException {
        cachedResults.clear();
        storage.stream().map(entry -> new LRUMediaCacheEntry<V>(entry.getKey())).forEach(this::put);
    }

    /**
     * Check if the cache is not already full and make space if needed.
     * We don't use the removeEldestEntry test method from the linkedHashMap because it can only remove one element.
     */
    protected void makeSpace() {
        synchronized (cachedResults) {
            Iterator<@Nullable LRUMediaCacheEntry<V>> iterator = cachedResults.values().iterator();
            Long currentCacheSize = cachedResults.values().stream()
                    .map(result -> result == null ? 0 : result.getCurrentSize()).reduce(0L, (Long::sum));
            int attemptToDelete = 0;
            while (currentCacheSize > maxCacheSize && cachedResults.size() > 1 && attemptToDelete < 10) {
                attemptToDelete++;
                LRUMediaCacheEntry<V> oldestEntry = iterator.next();
                if (oldestEntry != null) {
                    oldestEntry.deleteFile();
                    currentCacheSize -= oldestEntry.getCurrentSize();
                    lockByEntry.remove(oldestEntry.getKey());
                }
                iterator.remove();
            }
        }
    }
}
