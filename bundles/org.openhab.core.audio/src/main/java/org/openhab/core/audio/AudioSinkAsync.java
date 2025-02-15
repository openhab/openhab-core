/*
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Definition of an audio output like headphones, a speaker or for writing to
 * a file / clip.
 * Helper class for asynchronous sink : when the process() method returns, the {@link AudioStream}
 * may or may not be played. It is the responsibility of the implementing AudioSink class to
 * complete the CompletableFuture when playing is done. Any delayed tasks will then be performed, such as volume
 * restoration.
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public abstract class AudioSinkAsync implements AudioSink {

    private final Logger logger = LoggerFactory.getLogger(AudioSinkAsync.class);

    protected final Map<AudioStream, CompletableFuture<@Nullable Void>> runnableByAudioStream = new HashMap<>();

    @Override
    public CompletableFuture<@Nullable Void> processAndComplete(@Nullable AudioStream audioStream) {
        CompletableFuture<@Nullable Void> completableFuture = new CompletableFuture<@Nullable Void>();
        if (audioStream != null) {
            runnableByAudioStream.put(audioStream, completableFuture);
        }
        try {
            processAsynchronously(audioStream);
        } catch (UnsupportedAudioFormatException | UnsupportedAudioStreamException e) {
            completableFuture.completeExceptionally(e);
        }
        if (audioStream == null) {
            // No need to delay the post process task
            completableFuture.complete(null);
        }
        return completableFuture;
    }

    @Override
    public void process(@Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {
        processAsynchronously(audioStream);
    }

    /**
     * Processes the passed {@link AudioStream} asynchronously. This method is expected to return before the stream is
     * fully played. This is the sink responsibility to call the {@link AudioSinkAsync#playbackFinished(AudioStream)}
     * when it is.
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
    protected abstract void processAsynchronously(@Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException;

    /**
     * Will complete the future previously returned, allowing the core to run delayed task.
     *
     * @param audioStream The AudioStream is the key to find the delayed CompletableFuture in the storage.
     */
    protected void playbackFinished(AudioStream audioStream) {
        CompletableFuture<@Nullable Void> completableFuture = runnableByAudioStream.remove(audioStream);
        if (completableFuture != null) {
            completableFuture.complete(null);
        }

        // if the stream is not needed anymore, then we should call back the AudioStream to let it a chance
        // to auto dispose.
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
