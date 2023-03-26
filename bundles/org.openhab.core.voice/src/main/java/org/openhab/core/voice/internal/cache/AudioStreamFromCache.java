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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.FixedLengthAudioStream;
import org.openhab.core.cache.lru.InputStreamCacheWrapper;

/**
 * Implements AudioStream methods, with an inner stream extracted from cache
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public class AudioStreamFromCache extends FixedLengthAudioStream {

    private InputStreamCacheWrapper inputStream;
    private AudioFormat audioFormat;

    public AudioStreamFromCache(InputStreamCacheWrapper inputStream, AudioFormatInfo audioFormat) {
        this.inputStream = inputStream;
        this.audioFormat = audioFormat.toAudioFormat();
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public AudioFormat getFormat() {
        return audioFormat;
    }

    @Override
    public long length() {
        return inputStream.length();
    }

    @Override
    public InputStream getClonedStream() throws AudioException {
        try {
            return inputStream.getClonedStream();
        } catch (IOException e) {
            throw new AudioException("Cannot get cloned AudioStream", e);
        }
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        inputStream.skipNBytes(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }
}
