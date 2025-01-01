/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.audio;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Definition of an audio output like headphones, a speaker or for writing to
 * a file / clip.
 * Helper class for synchronous sink : when the process() method returns,
 * the source is considered played, and could be disposed.
 * Any delayed tasks can then be performed, such as volume restoration.
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public abstract class AudioSinkSync implements AudioSink {

    private final Logger logger = LoggerFactory.getLogger(AudioSinkSync.class);

    @Override
    public CompletableFuture<@Nullable Void> processAndComplete(@Nullable AudioStream audioStream) {
        try {
            processSynchronously(audioStream);
            return CompletableFuture.completedFuture(null);
        } catch (UnsupportedAudioFormatException | UnsupportedAudioStreamException e) {
            return CompletableFuture.failedFuture(e);
        } finally {
            // as the stream is not needed anymore, we should dispose of it
            if (audioStream instanceof Disposable disposableAudioStream) {
                try {
                    disposableAudioStream.dispose();
                } catch (IOException e) {
                    String fileName = audioStream instanceof FileAudioStream file ? file.toString() : "unknown";
                    if (logger.isDebugEnabled()) {
                        logger.debug("Cannot dispose of stream {}", fileName, e);
                    } else {
                        logger.warn("Cannot dispose of stream {}, reason {}", fileName, e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void process(@Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {
        processSynchronously(audioStream);
    }

    /**
     * Processes the passed {@link AudioStream} and returns only when the playback is ended.
     *
     * If the passed {@link AudioStream} is not supported by this instance, an {@link UnsupportedAudioStreamException}
     * is thrown.
     *
     * If the passed {@link AudioStream} has an {@link AudioFormat} not supported by this instance,
     * an {@link UnsupportedAudioFormatException} is thrown.
     *
     * In case the audioStream is null, this should be interpreted as a request to end any currently playing stream.
     *
     * @param audioStream the audio stream to play or null to keep quiet
     * @throws UnsupportedAudioFormatException If audioStream format is not supported
     * @throws UnsupportedAudioStreamException If audioStream is not supported
     */
    protected abstract void processSynchronously(@Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException;
}
