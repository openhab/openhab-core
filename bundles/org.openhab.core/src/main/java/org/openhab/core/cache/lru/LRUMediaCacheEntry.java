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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cached media entry resulting from a call to a supplier or a load from disk
 * This class also adds the capability to serve multiple InputStream concurrently
 * without asking already retrieved data to the wrapped stream.
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public class LRUMediaCacheEntry<V> {

    private final Logger logger = LoggerFactory.getLogger(LRUMediaCacheEntry.class);

    /**
     * Arbitrary chunk size. Small is less latency but more small calls and CPU load.
     */
    private static final int CHUNK_SIZE = 10000;

    /**
     * Take count of the number of {@link InputStreamCacheWrapper} currently using this {@link LRUMediaCacheEntry}
     */
    private int countStreamClient = 0;

    /**
     * A unique key to identify the result
     * (used to build the filename)
     */
    private final String key;

    // The inner InputStream
    private @Nullable InputStream inputStream;
    private @Nullable V metadata;

    // The data file where the media is stored:
    private @Nullable File file;
    // optional metadata is stored here:
    private @Nullable Storage<V> storage;

    protected long currentSize = 0;
    private boolean completed;
    private boolean faulty = false;

    private @Nullable FileChannel fileChannel;
    private final Lock fileOperationLock = new ReentrantLock();

    /**
     * This constructor is used when the file is fully cached on disk.
     * The file on disk will provide the data, and the storage will
     * provide metadata.
     *
     * @param key A unique key to identify the produced data
     */
    public LRUMediaCacheEntry(String key) {
        this.key = key;
        this.completed = true;
    }

    /**
     * This constructor is used when the file is not yet cached on disk.
     * Data is provided by the arguments
     *
     * @param key A unique key to identify the produced data
     * @param inputStream The data stream
     * @param metadata optional metadata to store along the stream
     */
    public LRUMediaCacheEntry(String key, InputStream inputStream, @Nullable V metadata) {
        this.key = key;
        this.inputStream = inputStream;
        this.metadata = metadata;
        this.completed = false;
    }

    /**
     * Link this cache entry to the underlying storage facility (disk for data, storage service for metadata)
     *
     * @param cacheDirectory
     * @param storage
     */
    protected void setCacheContext(Path cacheDirectory, Storage<V> storage) {
        File fileLocal = cacheDirectory.resolve(key).toFile();
        this.file = fileLocal;
        this.storage = storage;
        V actualDataInStorage = storage.get(key);
        if (actualDataInStorage == null) {
            storage.put(key, metadata);
        } else {
            this.metadata = actualDataInStorage;
        }
        this.currentSize = fileLocal.length();
    }

    /**
     * Get total size of the underlying stream.
     * If not already completed, will query the stream inside,
     * or get all the data.
     *
     * @return
     */
    protected Long getTotalSize() {
        if (completed) { // we already know the total size of the sound
            return currentSize;
        } else {
            // we must force-read all the stream to get the real size
            try {
                read(0, Integer.MAX_VALUE);
            } catch (IOException e) {
                logger.debug("Cannot read the total size of the cache result. Using 0", e);
            }
            return currentSize;
        }
    }

    /**
     * Get the current size
     *
     * @return
     */
    protected long getCurrentSize() {
        return currentSize;
    }

    /**
     * Get the key identifying this cache entry
     *
     * @return
     */
    protected String getKey() {
        return key;
    }

    /**
     * Open an InputStream wrapped around the file
     * There could be several clients InputStream on the same cache result
     *
     * @return A new InputStream with data from the cache
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {

        File localFile = file;
        if (localFile == null) { // the cache entry is not tied to the disk. The cache is not ready or not to be used.
            InputStream inputStreamLocal = inputStream;
            if (inputStreamLocal != null) {
                return inputStreamLocal;
            } else {
                throw new IllegalStateException(
                        "Shouldn't happen. This cache entry is not tied to a file on disk and the inner input stream is null.");
            }
        }
        logger.debug("Trying to open a cache inputstream for {}", localFile.getName());

        fileOperationLock.lock();
        try {
            countStreamClient++;
            // we could have to open the fileChannel
            FileChannel fileChannelLocal = fileChannel;
            if (fileChannelLocal == null || !localFile.exists()) {
                fileChannelLocal = FileChannel.open(localFile.toPath(),
                        EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE));
                fileChannel = fileChannelLocal;
                // if the file size is 0 but the completed boolean is true, THEN it means the file have
                // been deleted. We must mark the file as to be recreated :
                if (completed && fileChannelLocal.size() == 0) {
                    logger.debug("The cached file {} is not present anymore. We will have to recreate it",
                            localFile.getName());
                    this.faulty = true;
                }
            }
        } finally {
            fileOperationLock.unlock();
        }
        return new InputStreamCacheWrapper(this);
    }

    /**
     * This method is called by a wrapper when it has been closed by a client
     * The file and the inner stream could then be closed, if and only if no other client are accessing it.
     *
     * @throws IOException
     */
    protected void closeStreamClient() throws IOException {
        File fileLocal = file;
        if (fileLocal == null) {
            logger.debug("Trying to close a non existent-file. Is there a bug");
            return;
        }
        logger.debug("Trying to close a cached inputstream client for {}", fileLocal.getName());
        fileOperationLock.lock();
        try {
            countStreamClient--;
            if (countStreamClient <= 0) {// no more client reading or writing : closing the filechannel
                try {
                    FileChannel fileChannelLocal = fileChannel;
                    if (fileChannelLocal != null) {
                        try {
                            logger.debug("Effectively close the cache filechannel for {}", fileLocal.getName());
                            fileChannelLocal.close();
                        } finally {
                            fileChannel = null;
                        }
                    }
                } finally {
                    InputStream inputStreamLocal = inputStream;
                    if (inputStreamLocal != null) {
                        inputStreamLocal.close();
                    }
                }
            }
        } finally {
            fileOperationLock.unlock();
        }
    }

    /**
     * Get metadata for this cache result.
     *
     * @return metadata
     */
    public @Nullable V getMetadata() {
        return this.metadata;
    }

    /**
     * Read from the cached file. If there is not enough bytes to read in the file, the supplier will be queried.
     *
     * @param start The offset to read the file from
     * @param sizeToRead the number of byte to read
     * @return A byte array from the file. The size may or may not be the sizeToRead requested
     * @throws IOException
     */
    protected byte[] read(int start, int sizeToRead) throws IOException {
        FileChannel fileChannelLocal = fileChannel;
        if (fileChannelLocal == null || isFaulty()) {
            throw new IOException("Cannot read cache from null file channel or deleted file.");
        }

        // check if we need to get data from the inner stream.
        if (start + sizeToRead > fileChannelLocal.size() && !completed) {
            logger.trace("Maybe need to get data from inner stream");
            // try to get new bytes from the inner stream
            InputStream streamLocal = inputStream;
            if (streamLocal != null) {
                logger.trace("Trying to synchronize for reading inner inputstream");
                synchronized (streamLocal) {
                    // now that we really have the lock, test again if we really need data from the stream
                    while (start + sizeToRead > fileChannelLocal.size() && !completed) {
                        logger.trace("Really need to get data from inner stream");
                        byte[] readFromSupplierStream = streamLocal.readNBytes(CHUNK_SIZE);
                        if (readFromSupplierStream.length == 0) { // we read all the stream
                            logger.trace("End of the stream reached");
                            completed = true;
                        } else {
                            fileChannelLocal.write(ByteBuffer.wrap(readFromSupplierStream), currentSize);
                            logger.trace("writing {} bytes to {}", readFromSupplierStream.length, key);
                            currentSize += readFromSupplierStream.length;
                        }
                    }
                }
            } else {
                faulty = true;
                logger.warn("Shouldn't happen : trying to get data from upstream for {} but original stream is null",
                        key);
            }
        }
        // the cache file is now filled, get bytes from it.
        long maxToRead = Math.min(currentSize, sizeToRead);
        ByteBuffer byteBufferFromChannelFile = ByteBuffer.allocate((int) maxToRead);
        int byteReadNumber = fileChannelLocal.read(byteBufferFromChannelFile, Integer.valueOf(start).longValue());
        logger.trace("Read {} bytes from the filechannel", byteReadNumber);
        if (byteReadNumber > 0) {
            byte[] resultByteArray = new byte[byteReadNumber];
            byteBufferFromChannelFile.rewind();
            byteBufferFromChannelFile.get(resultByteArray);
            return resultByteArray;
        } else {
            return new byte[0];
        }
    }

    /**
     * Return the number of bytes that we can actually read without calling
     * the underlying stream
     *
     * @param offset
     * @return
     */
    protected int availableFrom(int offset) {
        FileChannel fileChannelLocal = fileChannel;
        if (fileChannelLocal == null) {
            return 0;
        }
        try {
            return Math.max(0, Long.valueOf(fileChannelLocal.size() - offset).intValue());
        } catch (IOException e) {
            logger.debug("Cannot get file length for cache file {}", key);
            return 0;
        }
    }

    /**
     * Delete the cache file linked to this entry
     */
    protected void deleteFile() {
        logger.debug("Receiving call to delete the cache file {}", key);
        fileOperationLock.lock();
        try {
            // check if a client is actually reading the file
            if (countStreamClient <= 0) {
                logger.debug("Effectively deleting the cached file {}", key);
                // delete the file :
                File fileLocal = file;
                if (fileLocal != null) {
                    fileLocal.delete();
                }
                // and the associated info
                Storage<V> storageLocal = storage;
                if (storageLocal != null) {
                    storageLocal.remove(key);
                }
            }
        } finally {
            fileOperationLock.unlock();
        }
    }

    public boolean isFaulty() {
        return faulty;
    }
}
