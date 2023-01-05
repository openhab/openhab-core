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
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FixedLengthAudioStream;
import org.openhab.core.voice.TTSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Each {@link TTSResult} instance can handle several {@link AudioStream}s.
 * This class is a wrapper for such functionality and can
 * ask the cached TTSResult for data, allowing concurrent access to
 * the audio stream even if it is currently actively read from the TTS service.
 * If the cached TTSResult is faulty, then it can take data from the
 * fallback supplier (which should be a direct call to the TTS service).
 * This class implements the two main read methods (byte by byte, and with an array)
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public class AudioStreamCacheWrapper extends FixedLengthAudioStream {

    private final Logger logger = LoggerFactory.getLogger(AudioStreamCacheWrapper.class);

    private TTSResult ttsResult;
    private int offset = 0;

    // A fallback mechanism : if reading from the cache fails,
    // It will therefore try to fallback to a direct tts stream
    private AudioStreamSupplier fallbackDirectSupplier;
    private @Nullable AudioStream fallbackDirectAudioStream;

    /***
     * Construct a transparent AudioStream wrapper around the data from the cache.
     *
     * @param ttsResult The parent cached {@link TTSResult}
     * @param supplier A fallback {@link AudioStreamSupplier}, if something goes wrong with the cache mechanism
     */
    public AudioStreamCacheWrapper(TTSResult ttsResult, AudioStreamSupplier supplier) {
        this.ttsResult = ttsResult;
        this.fallbackDirectSupplier = supplier;
    }

    @Override
    public AudioFormat getFormat() {
        return ttsResult.getAudioFormat();
    }

    @Override
    public int available() throws IOException {
        return ttsResult.availableFrom(offset);
    }

    @Override
    public int read() throws IOException {
        if (fallbackDirectAudioStream == null) {
            try {
                byte[] bytesRead = ttsResult.read(offset, 1);
                if (bytesRead.length == 0) {
                    return -1;
                } else {
                    offset++;
                    return bytesRead[0] & 0xff;
                }
            } catch (IOException e) {
                logger.debug("Cannot read from tts cache. Will use the fallback TTS mechanism", e);
            }
        }

        // beyond this point, we failed, so the fallback must be active :
        enableFallback();
        AudioStream fallbackDirectAudioStreamLocal = fallbackDirectAudioStream;
        if (fallbackDirectAudioStreamLocal != null) {
            return fallbackDirectAudioStreamLocal.read();
        }

        throw new IOException("Neither TTS cache nor TTS fallback method succeed");
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        if (fallbackDirectAudioStream == null) {
            if (b == null) {
                throw new IOException("Array to write is null");
            }
            Objects.checkFromIndexSize(off, len, b.length);

            if (len == 0) {
                return 0;
            }

            try {
                byte[] bytesRead = ttsResult.read(offset, len);
                offset += bytesRead.length;
                if (bytesRead.length == 0) {
                    return -1;
                }
                int i = 0;
                for (; i < len && i < bytesRead.length; i++) {
                    b[off + i] = bytesRead[i];
                }
                return i;
            } catch (IOException e) {
                logger.debug("Cannot read from tts cache. Will use the fallback TTS mechanism", e);
            }
        }

        // beyond this point, we failed, so fallback must be active :
        enableFallback();
        AudioStream fallbackDirectAudioStreamLocal = fallbackDirectAudioStream;
        if (fallbackDirectAudioStreamLocal != null) {
            return fallbackDirectAudioStreamLocal.read(b, off, len);
        }

        throw new IOException("Neither TTS cache nor TTS fallback method succeed");
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
            ttsResult.closeAudioStreamClient();
        }
    }

    private void enableFallback() throws IOException {
        if (fallbackDirectAudioStream == null) {
            try {
                AudioStream fallBackDirectResolutionLocal = fallbackDirectSupplier.fallBackDirectResolution();
                this.fallbackDirectAudioStream = fallBackDirectResolutionLocal;
                fallBackDirectResolutionLocal.skip(offset);
            } catch (TTSException e) {
                throw new IOException("Cannot read from TTS service", e);
            }
        }
    }

    @Override
    public long length() {
        Long totalSize = ttsResult.getTotalSize();
        if (totalSize > 0L) {
            return totalSize;
        }
        try {
            enableFallback();

            AudioStream fallbackDirectAudioStreamLocal = this.fallbackDirectAudioStream;
            if (fallbackDirectAudioStreamLocal instanceof FixedLengthAudioStream fixedLengthAudioStream) {
                return fixedLengthAudioStream.length();
            }
        } catch (IOException e) {
            logger.debug("Cannot get the length of the AudioStream");
        }
        return 0;
    }

    @Override
    public InputStream getClonedStream() throws AudioException {
        return ttsResult.getAudioStream(fallbackDirectSupplier);
    }
}
