/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
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
public class LRUMediaCacheEntryTest {

    private @TempDir @NonNullByDefault({}) Path tempDir;

    private @Mock @NonNullByDefault({}) LRUMediaCache<MetadataSample> ttsServiceMock;
    private @Mock @NonNullByDefault({}) Supplier<LRUMediaCacheEntry<MetadataSample>> supplier;

    private @NonNullByDefault({}) @Mock StorageService storageService;
    private @NonNullByDefault({}) @Mock Storage<MetadataSample> storage;

    @BeforeEach
    public void init() {
        System.setProperty(OpenHAB.USERDATA_DIR_PROG_ARGUMENT, tempDir.toString());
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
    }

    private LRUMediaCache<MetadataSample> createCache(long size) throws IOException {
        return new LRUMediaCache<>(storageService, size, "lrucachetest.pid", this.getClass().getClassLoader());
    }

    public static class FakeStream extends InputStream {

        ByteArrayInputStream innerInputStream;
        private boolean closed = false;

        public FakeStream(byte[] byteToReturn) {
            innerInputStream = new ByteArrayInputStream(byteToReturn);
        }

        @Override
        public int read() throws IOException {
            return innerInputStream.read();
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            innerInputStream.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }

    @Test
    public void getInputStreamTwiceWithOnlyOneCallToTheSupplierAndCompareTest() throws IOException {
        LRUMediaCache<MetadataSample> lruMediaCache = createCache(1000);

        InputStream fakeStream = new FakeStream("This is a false string to simulate some data".getBytes());
        MetadataSample metadata = new MetadataSample("meta1", 42);
        when(supplier.get()).thenReturn(new LRUMediaCacheEntry<>("key1", fakeStream, metadata));

        LRUMediaCacheEntry<MetadataSample> lruMediaCacheEntry = lruMediaCache.get("key1", supplier);

        // get InputStream wrapped by the cache system
        InputStream actualAudioStream = lruMediaCacheEntry.getInputStream();
        String actuallyRead = new String(actualAudioStream.readAllBytes(), StandardCharsets.UTF_8);
        actualAudioStream.close();
        // ensure that the data are not corrupted
        assertEquals("This is a false string to simulate some data", actuallyRead);

        // get again the InputStream wrapped by the cache system
        actualAudioStream = lruMediaCacheEntry.getInputStream();
        actuallyRead = new String(actualAudioStream.readAllBytes(), StandardCharsets.UTF_8);
        actualAudioStream.close();
        // ensure that the data are not corrupted
        assertEquals("This is a false string to simulate some data", actuallyRead);

        // Ensure the TTS service was called only once :
        verify(supplier, times(1)).get();
    }

    @Test
    public void loadTwoStreamsAtTheSameTimeFromTheSameSupplierTest() throws IOException {
        LRUMediaCache<MetadataSample> lruMediaCache = createCache(1000);

        // init simulated data stream
        FakeStream fakeStream = new FakeStream(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        MetadataSample metadata = new MetadataSample("meta1", 42);
        when(supplier.get()).thenReturn(new LRUMediaCacheEntry<>("key1", fakeStream, metadata));

        LRUMediaCacheEntry<MetadataSample> lruMediaCacheEntry = lruMediaCache.get("key1", supplier);

        // get a first InputStream wrapped by the cache system
        InputStream actualAudioStream1 = lruMediaCacheEntry.getInputStream();

        // get a second, concurrent, InputStream wrapped by the cache system
        InputStream actualAudioStream2 = lruMediaCacheEntry.getInputStream();

        // read bytes from the two stream concurrently
        byte[] byteReadFromStream1 = actualAudioStream1.readNBytes(4);
        byte[] byteReadFromStream2 = actualAudioStream2.readNBytes(4);

        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, byteReadFromStream1);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, byteReadFromStream2);

        // second read, from the two stream concurrently
        byteReadFromStream1 = actualAudioStream1.readNBytes(4);
        byteReadFromStream2 = actualAudioStream2.readNBytes(4);

        actualAudioStream1.close();
        assertFalse(fakeStream.isClosed()); // not closed because there is still one open
        actualAudioStream2.close();
        assertTrue(fakeStream.isClosed()); // all client closed, the main stream should also be closed

        assertArrayEquals(new byte[] { 5, 6, 7, 8 }, byteReadFromStream1);
        assertArrayEquals(new byte[] { 5, 6, 7, 8 }, byteReadFromStream1);

        // we call the TTS service only once
        verify(supplier, times(1)).get();
        verifyNoMoreInteractions(supplier);
    }

    @Test
    public void loadTwoThreadsAtTheSameTimeFromTheSameSupplierTest() throws IOException {
        LRUMediaCache<MetadataSample> lruMediaCache = createCache(1000);

        // init simulated data stream
        byte[] randomData = getRandomData(10 * 10240);
        FakeStream fakeStream = new FakeStream(randomData);
        MetadataSample metadata = new MetadataSample("meta1", 42);
        when(supplier.get()).thenReturn(new LRUMediaCacheEntry<>("key1", fakeStream, metadata));

        LRUMediaCacheEntry<MetadataSample> lruMediaCacheEntry = lruMediaCache.get("key1", supplier);

        // get a first InputStream wrapped by the cache system
        InputStream actualAudioStream1 = lruMediaCacheEntry.getInputStream();

        // get a second, concurrent, InputStream wrapped by the cache system
        InputStream actualAudioStream2 = lruMediaCacheEntry.getInputStream();

        // read bytes from the two stream concurrently
        Mutable<@Nullable IOException> exceptionCatched = new MutableObject<>();
        List<InputStream> parallelAudioStreamList = List.of(actualAudioStream1, actualAudioStream2);
        List<byte[]> bytesResultList = parallelAudioStreamList.parallelStream().map(stream -> {
            try {
                return stream.readAllBytes();
            } catch (IOException e) {
                exceptionCatched.setValue(e);
                return new byte[0];
            }
        }).toList();

        IOException possibleException = exceptionCatched.getValue();
        if (possibleException != null) {
            throw possibleException;
        }
        assertArrayEquals(randomData, bytesResultList.get(0));
        assertArrayEquals(randomData, bytesResultList.get(1));

        actualAudioStream1.close();
        assertFalse(fakeStream.isClosed()); // not closed because there is still one open
        actualAudioStream2.close();
        assertTrue(fakeStream.isClosed()); // all client closed, the main stream should also be closed

        // we call the TTS service only once
        verify(supplier).get();
        verifyNoMoreInteractions(ttsServiceMock);
    }

    private byte[] getRandomData(int length) {
        Random random = new Random();
        byte[] randomBytes = new byte[length];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    @SuppressWarnings("null")
    @Test
    public void streamAndMetadataTest() throws IOException {
        LRUMediaCache<MetadataSample> lruMediaCache = createCache(1000);

        // init simulated data stream
        byte[] randomData = getRandomData(2 * 10240);

        FakeStream fakeStream = new FakeStream(randomData);
        MetadataSample metadata = new MetadataSample("meta1", 42);
        when(supplier.get()).thenReturn(new LRUMediaCacheEntry<>("key1", fakeStream, metadata));
        when(storage.get("key1")).thenReturn(metadata);

        LRUMediaCacheEntry<MetadataSample> lruMediaCacheEntry = lruMediaCache.get("key1", supplier);

        InputStream audioStreamClient = lruMediaCacheEntry.getInputStream();
        byte[] bytesRead = audioStreamClient.readAllBytes();
        audioStreamClient.close();
        assertTrue(fakeStream.isClosed());

        assertEquals(metadata.meta1, lruMediaCacheEntry.getMetadata().meta1);
        assertEquals(metadata.meta2, lruMediaCacheEntry.getMetadata().meta2);
        assertArrayEquals(randomData, bytesRead);
    }

    @Test
    public void getTotalSizeByForcingReadAllTest() throws IOException {
        LRUMediaCache<MetadataSample> lruMediaCache = createCache(1000);

        // init simulated data stream
        byte[] randomData = getRandomData(10 * 10240);
        FakeStream fakeStream = new FakeStream(randomData);
        MetadataSample metadata = new MetadataSample("meta1", 42);
        when(supplier.get()).thenReturn(new LRUMediaCacheEntry<>("key1", fakeStream, metadata));

        LRUMediaCacheEntry<MetadataSample> lruMediaCacheEntry = lruMediaCache.get("key1", supplier);

        InputStream audioStreamClient = lruMediaCacheEntry.getInputStream();
        Long totalSize = lruMediaCacheEntry.getTotalSize();
        audioStreamClient.close();
        assertEquals(10 * 10240, totalSize);
    }

    private static class MetadataSample {
        protected String meta1;
        protected int meta2;

        public MetadataSample(String meta1, int meta2) {
            this.meta1 = meta1;
            this.meta2 = meta2;
        }
    }
}
