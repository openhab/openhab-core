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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.OpenHAB;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;

/**
 * Test the cache system
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class LRUMediaCacheTest {

    private @TempDir @NonNullByDefault({}) Path tempDir;

    private @Mock @NonNullByDefault({}) InputStream inputStreamMock;
    private @Mock @NonNullByDefault({}) Supplier<LRUMediaCacheEntry<MetadataSample>> supplier;

    private @NonNullByDefault({}) @Mock StorageService storageService;
    private @NonNullByDefault({}) @Mock Storage<MetadataSample> storage;

    @BeforeEach
    public void init() {
        storageService = new StorageService() {
            @SuppressWarnings("unchecked")
            @Override
            public Storage<MetadataSample> getStorage(String name, @Nullable ClassLoader classLoader) {
                return storage;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Storage<MetadataSample> getStorage(String name) {
                return storage;
            }
        };
        System.setProperty(OpenHAB.USERDATA_DIR_PROG_ARGUMENT, tempDir.toString());
    }

    private LRUMediaCache<MetadataSample> createCache(long size) throws IOException {
        LRUMediaCache<MetadataSample> voiceLRUCache = new LRUMediaCache<MetadataSample>(storageService, size,
                "lrucachetest.pid", this.getClass().getClassLoader());
        return voiceLRUCache;
    }

    /**
     * Basic get and set for the LRU cache
     *
     * @throws IOException
     */
    @Test
    public void simpleLRUPutAndGetTest() throws IOException {
        LRUMediaCache<MetadataSample> lruCache = createCache(10);
        LRUMediaCacheEntry<MetadataSample> cacheEntry = new LRUMediaCacheEntry<>("key1");
        lruCache.put(cacheEntry);
        assertEquals(cacheEntry, lruCache.cachedResults.get("key1"));
        assertEquals(null, lruCache.cachedResults.get("key2"));
    }

    /**
     * Test the LRU eviction policy
     *
     * @throws IOException
     */
    @Test
    public void putAndGetAndEvictionOrderLRUTest() throws IOException {
        LRUMediaCache<MetadataSample> lruCache = createCache(10240);
        LRUMediaCacheEntry<MetadataSample> cacheEntry1 = new LRUMediaCacheEntry<>("key1");
        cacheEntry1.setCacheContext(tempDir, storage);
        cacheEntry1.currentSize = 4 * 1024;
        lruCache.cachedResults.put(cacheEntry1.getKey(), cacheEntry1);
        LRUMediaCacheEntry<MetadataSample> cacheEntry2 = new LRUMediaCacheEntry<>("key2");
        cacheEntry2.setCacheContext(tempDir, storage);
        cacheEntry2.currentSize = 4 * 1024;
        lruCache.cachedResults.put(cacheEntry2.getKey(), cacheEntry2);
        LRUMediaCacheEntry<MetadataSample> cacheEntry3 = new LRUMediaCacheEntry<>("key3");
        cacheEntry3.setCacheContext(tempDir, storage);
        cacheEntry3.currentSize = 2 * 1024;
        lruCache.cachedResults.put(cacheEntry3.getKey(), cacheEntry3);
        LRUMediaCacheEntry<MetadataSample> cacheEntry4 = new LRUMediaCacheEntry<>("key4");
        cacheEntry4.setCacheContext(tempDir, storage);
        cacheEntry4.currentSize = 4 * 1024;
        lruCache.cachedResults.put(cacheEntry4.getKey(), cacheEntry4);

        lruCache.makeSpace();
        // cacheEntry1 should be evicted now (size limit is 10, and effective size is 12 when we try to put the
        // cacheEntry4)
        assertEquals(null, lruCache.cachedResults.get("key1"));

        // getting cacheEntry2 will put it in head, cacheEntry3 is now tail
        assertEquals(cacheEntry2, lruCache.cachedResults.get("key2"));

        // putting again cacheEntry1 should expel tail, which is cacheEntry3
        lruCache.cachedResults.put(cacheEntry1.getKey(), cacheEntry1);
        lruCache.makeSpace();
        assertEquals(null, lruCache.cachedResults.get("key3"));
    }

    /**
     * Test the file deletion
     *
     * @throws IOException
     */
    @Test
    public void fileDeletionTest() throws IOException {
        LRUMediaCacheEntry<MetadataSample> cacheEntry1 = new LRUMediaCacheEntry<>("key1");
        cacheEntry1.setCacheContext(tempDir, storage);

        File result1File = tempDir.resolve(cacheEntry1.getKey()).toFile();
        result1File.createNewFile();
        MetadataSample metadataInfo = new MetadataSample("text", 3);
        storage.put("key1", metadataInfo);

        cacheEntry1.deleteFile();

        assertFalse(result1File.exists());
        assertNull(storage.get("key1"));
    }

    /**
     * Test that the eviction policy calls the delete method
     *
     * @throws IOException
     */
    @Test
    public void fileDeletionWhenEvictionTest() throws IOException {
        LRUMediaCache<MetadataSample> voiceLRUCache = createCache(10240);

        @SuppressWarnings("unchecked")
        LRUMediaCacheEntry<MetadataSample> entryToEvict = Mockito.mock(LRUMediaCacheEntry.class);
        when(entryToEvict.getCurrentSize()).thenReturn(15L * 1024);
        when(entryToEvict.getKey()).thenReturn("resultToEvict");
        voiceLRUCache.put(entryToEvict);

        // the cache is already full, so the next put will delete resultToEvict
        LRUMediaCacheEntry<MetadataSample> cacheEntry2 = new LRUMediaCacheEntry<>("key2");
        cacheEntry2.currentSize = 4 * 1024;
        voiceLRUCache.put(cacheEntry2);

        verify(entryToEvict).deleteFile();
    }

    /**
     * This test checks than we can overwrite a previously
     * cached entry and then it is now at the head position.
     *
     * @throws IOException
     */
    @Test
    public void putExistingResultLRUTest() throws IOException {
        LRUMediaCache<MetadataSample> lruCache = createCache(10240);
        LRUMediaCacheEntry<MetadataSample> cacheEntry = new LRUMediaCacheEntry<>("key1");
        cacheEntry.currentSize = 4 * 1024;
        lruCache.cachedResults.put(cacheEntry.getKey(), cacheEntry);
        LRUMediaCacheEntry<MetadataSample> cacheEntry2 = new LRUMediaCacheEntry<>("key2");
        cacheEntry2.currentSize = 10 * 1024;
        lruCache.cachedResults.put(cacheEntry2.getKey(), cacheEntry2);

        // put again key1 --> key2 is now tail
        lruCache.cachedResults.put(cacheEntry.getKey(), cacheEntry);

        LRUMediaCacheEntry<MetadataSample> cacheEntry3 = new LRUMediaCacheEntry<>("key3");
        cacheEntry3.currentSize = 4 * 1024;
        lruCache.cachedResults.put(cacheEntry3.getKey(), cacheEntry3);
        lruCache.makeSpace();

        // key2 should be expelled now
        assertEquals(null, lruCache.cachedResults.get("key2"));

        // key1 and key3 are a hit
        assertEquals(cacheEntry, lruCache.cachedResults.get("key1"));
        assertEquals(cacheEntry3, lruCache.cachedResults.get("key3"));
    }

    /**
     * Simulate a cache miss, then two other hits
     * The supplier service is called only once
     *
     * @throws IOException
     */
    @Test
    public void getCacheMissAndHitTest() throws IOException {
        MetadataSample metadata = new MetadataSample("meta1", 42);

        when(supplier.get()).thenReturn(new LRUMediaCacheEntry<>("key", inputStreamMock, metadata));
        // In this test the stream will return two bytes of data, then an empty stream so signal its end :
        when(inputStreamMock.readNBytes(any(Integer.class))).thenReturn(new byte[2], new byte[0]);

        LRUMediaCache<MetadataSample> lruCache = createCache(1000);

        // first cache miss
        LRUMediaCacheEntry<MetadataSample> resultStream = lruCache.get("key", supplier);
        resultStream.getInputStream().readAllBytes();

        // then cache hit
        resultStream = lruCache.get("key", supplier);
        resultStream.getInputStream().readAllBytes();

        // then cache hit
        resultStream = lruCache.get("key", supplier);
        resultStream.getInputStream().readAllBytes();

        // even with three call to get and getFormat, the service and the underlying stream were called
        // only once :
        verify(supplier, times(1)).get();
        // this is called twice because the second call respond with zero and signal the end of stream
        verify(inputStreamMock, times(2)).readNBytes(any(Integer.class));
    }

    /**
     * Load some cache entries from files on disk
     *
     * @throws IOException
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "null" })
    @Test
    public void loadResultsFromCacheDirectoryTest() throws IOException {
        // prepare cache directory
        Path cacheDirectory = tempDir.resolve("cache/lrucachetest.pid/");
        Files.createDirectories(cacheDirectory);

        // prepare some files
        File file1 = cacheDirectory.resolve("key1").toFile();
        MetadataSample metadataSample1 = new MetadataSample("text1", 1);
        when(storage.get("key1")).thenReturn(metadataSample1);
        try (FileWriter file1Writer = new FileWriter(file1)) {
            file1Writer.write("falsedata");
        }

        // prepare some files
        File file2 = cacheDirectory.resolve("key2").toFile();
        MetadataSample metadataSample2 = new MetadataSample("text2", 2);
        when(storage.get("key2")).thenReturn(metadataSample2);
        try (FileWriter file2Writer = new FileWriter(file2)) {
            file2Writer.write("falsedata");
        }
        when(storage.stream())
                .thenAnswer((invocation) -> Stream.of(new AbstractMap.SimpleImmutableEntry("key1", metadataSample1),
                        new AbstractMap.SimpleImmutableEntry("key2", metadataSample2)));

        // create a LRU cache that will use the above data
        LRUMediaCache<MetadataSample> lruCache = createCache(20);

        LRUMediaCacheEntry<MetadataSample> result1 = lruCache.cachedResults.get("key1");
        assertNotNull(result1);
        assertEquals(result1.getMetadata().getMeta2(), 1);
        assertEquals(result1.getMetadata().getMeta1(), "text1");

        LRUMediaCacheEntry<MetadataSample> result2 = lruCache.cachedResults.get("key2");
        assertNotNull(result2);
        assertEquals(result2.getMetadata().getMeta1(), "text2");
        assertEquals(result2.getMetadata().getMeta2(), 2);

        LRUMediaCacheEntry<MetadataSample> result3 = lruCache.cachedResults.get("key3");
        assertNull(result3);
    }

    @Test
    public void enoughFreeDiskSpaceTest() throws IOException {
        // arbitrary long value, should throw exception because no disk of this size exists
        LRUMediaCache<?> cache = createCache(Long.MAX_VALUE / 10);
        assertFalse(cache.cacheIsOK);
    }

    /**
     * Test the deletion of orphaned element (either data or metadata)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void cleanDirectoryOrphanFilesTest() throws IOException {
        // prepare cache directory
        Path cacheDirectory = tempDir.resolve("cache/lrucachetest.pid/");
        Files.createDirectories(cacheDirectory);

        // prepare some files : normal entry
        File file1 = cacheDirectory.resolve("key1").toFile();
        MetadataSample metadataSample1 = new MetadataSample("text1", 1);
        when(storage.get("key1")).thenReturn(metadataSample1);
        try (FileWriter file1Writer = new FileWriter(file1)) {
            file1Writer.write("falsedata");
        }

        // prepare some files : orphan info
        MetadataSample metadataSample2 = new MetadataSample("text2", 2);
        when(storage.get("key2")).thenReturn(metadataSample2);

        // prepare storage map for stream operation
        when(storage.stream())
                .thenAnswer((invocation) -> Stream.of(new AbstractMap.SimpleImmutableEntry("key1", metadataSample1),
                        new AbstractMap.SimpleImmutableEntry("key2", metadataSample2)));

        // prepare some files : orphan file
        File file3 = cacheDirectory.resolve("key3").toFile();
        when(storage.get("key3")).thenReturn(null);
        try (FileWriter file3Writer = new FileWriter(file3)) {
            file3Writer.write("fake non empty data");
        }

        // create a LRU cache that will use the above data
        createCache(20);

        // the file for entry 1 still exists :
        assertTrue(file1.exists());
        assertNotNull(storage.get("key1"));

        // the file for entry 2 should have been deleted :
        verify(storage).remove("key2");

        // the file for entry 3 should have been deleted :
        assertFalse(file3.exists());
    }

    /**
     * Test the deletion of matadata when file is empty
     */
    @Test
    public void cleanDirectoryEmptyFilesTest() throws IOException {
        // prepare cache directory
        Path cacheDirectory = tempDir.resolve("cache/lrucachetest.pid/");
        Files.createDirectories(cacheDirectory);

        // prepare some files : normal entry
        File file1 = cacheDirectory.resolve("key1").toFile();
        when(storage.get("key1")).thenReturn(new MetadataSample("key1", 2));
        try (FileWriter file1Writer = new FileWriter(file1)) {
            file1Writer.write("falsedata");
        }

        // prepare some files : empty file
        File file2 = cacheDirectory.resolve("key2").toFile();
        file2.createNewFile();
        assertTrue(file2.exists());

        // create a LRU cache that will load the above data
        createCache(20);

        // the file for entry 1 still exists :
        assertTrue(file1.exists());
        assertNotNull(storage.get("key1"));

        // the file for entry 2 should have been deleted (empty file) :
        verify(storage).remove("key2");
        assertFalse(file2.exists());
    }

    /**
     * Test a cache entry which has been deleted. It will be recreated
     */
    @Test
    public void faultyStreamTest() throws IOException {
        MetadataSample metadata = new MetadataSample("meta1", 42);

        when(supplier.get()).thenAnswer((invocation) -> new LRUMediaCacheEntry<>("key", inputStreamMock, metadata));
        // In this test the stream will return two bytes of data, then an empty stream so signal its end.
        // it will be called twice, so return it twice
        when(inputStreamMock.readNBytes(any(Integer.class))).thenReturn(new byte[2], new byte[0], new byte[2],
                new byte[0]);

        LRUMediaCache<MetadataSample> lruCache = createCache(1000);

        // first cache miss
        LRUMediaCacheEntry<MetadataSample> resultEntry = lruCache.get("key", supplier);
        InputStream resultInputStream = resultEntry.getInputStream();
        resultInputStream.readAllBytes();
        resultInputStream.close();

        File dataFile = tempDir.resolve("cache/lrucachetest.pid").resolve("key").toFile();
        assertTrue(dataFile.exists());
        dataFile.delete();
        assertFalse(dataFile.exists());

        // then cache hit
        resultEntry = lruCache.get("key", supplier);
        resultInputStream = resultEntry.getInputStream();

        // but the result is faulty because file is missing
        assertTrue(resultEntry.isFaulty());

        // try to read but we got an exception
        boolean exceptionCatched = false;
        try {
            resultInputStream.readAllBytes();
        } catch (IOException io) {
            if ("Cannot read cache from null file channel or deleted file.".equals(io.getMessage())) {
                exceptionCatched = true;
            }
        }
        assertTrue(exceptionCatched);
        resultInputStream.close();

        // get it another time
        resultEntry = lruCache.get("key", supplier);
        // this time the result is not faulty anymore because it was computed another time
        assertFalse(resultEntry.isFaulty());

        resultInputStream = resultEntry.getInputStream();
        byte[] bytesRead = resultInputStream.readAllBytes();
        assertEquals(2, bytesRead.length);
        resultInputStream.close();

        // the service and the underlying stream were called twice because of a missing file:
        verify(supplier, times(2)).get();
        verify(inputStreamMock, times(4)).readNBytes(any(Integer.class));
    }

    private static class MetadataSample {

        protected String meta1;
        protected int meta2;

        public MetadataSample(String meta1, int meta2) {
            this.meta1 = meta1;
            this.meta2 = meta2;
        }

        public String getMeta1() {
            return meta1;
        }

        public int getMeta2() {
            return meta2;
        }
    }
}
