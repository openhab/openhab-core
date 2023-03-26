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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Each cache result instance can handle several {@link InputStream}s.
 * This class is a wrapper for such functionality and can
 * ask the cached entry for data, allowing concurrent access to
 * the source even if it is currently actively read from the supplier service.
 * This class implements the two main read methods (byte by byte, and with an array)
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public class InputStreamCacheWrapper extends InputStream {

    private final Logger logger = LoggerFactory.getLogger(InputStreamCacheWrapper.class);

    private LRUMediaCacheEntry<?> cacheEntry;
    private int offset = 0;

    /***
     * Construct a transparent InputStream wrapper around data from the cache.
     *
     * @param cacheEntry The parent cached {@link LRUMediaCacheEntry}
     */
    public InputStreamCacheWrapper(LRUMediaCacheEntry<?> cacheEntry) {
        this.cacheEntry = cacheEntry;
    }

    @Override
    public int available() throws IOException {
        return cacheEntry.availableFrom(offset);
    }

    @Override
    public int read() throws IOException {
        byte[] bytesRead = cacheEntry.read(offset, 1);
        if (bytesRead.length == 0) {
            return -1;
        } else {
            offset++;
            return bytesRead[0] & 0xff;
        }
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        if (b == null) {
            throw new IOException("Array to write is null");
        }
        Objects.checkFromIndexSize(off, len, b.length);

        if (len == 0) {
            return 0;
        }

        byte[] bytesRead = cacheEntry.read(offset, len);
        offset += bytesRead.length;
        if (bytesRead.length == 0) {
            return -1;
        }
        int i = 0;
        for (; i < len && i < bytesRead.length; i++) {
            b[off + i] = bytesRead[i];
        }
        return i;
    }

    @Override
    public long skip(long n) throws IOException {
        offset += n;
        return n;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            cacheEntry.closeStreamClient();
        }
    }

    public long length() {
        Long totalSize = cacheEntry.getTotalSize();
        if (totalSize > 0L) {
            return totalSize;
        }
        logger.debug("Cannot get the length of the stream");
        return -1;
    }

    public InputStream getClonedStream() throws IOException {
        return cacheEntry.getInputStream();
    }
}
