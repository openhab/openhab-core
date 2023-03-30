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
package org.openhab.core.audio;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Definition of an audio output like headphones, a speaker or for writing to
 * a file / clip.
 * This version is asynchronous: when the process() method returns, the {@link AudioStream}
 * may or may not be played, and we don't know when the delayed task will be executed.
 * CAUTION : It is the responsibility of the implementing AudioSink class to call the runDelayedTask
 * method when playing is done.
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public abstract class AudioSinkAsync implements AudioSink {

    private final Logger logger = LoggerFactory.getLogger(AudioSinkAsync.class);

    private final Map<AudioStream, Runnable> runnableByAudioStream = new HashMap<>();

    @Override
    public void process(@Nullable AudioStream audioStream, Runnable whenFinished)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {

        try {
            if (audioStream != null) {
                runnableByAudioStream.put(audioStream, whenFinished);
            }
            process(audioStream);
        } finally {
            if (audioStream == null) {
                // No need to delay the post process task
                whenFinished.run();
            }
        }
    }

    /**
     * Will run the delayed task stored previously.
     *
     * @param audioStream The AudioStream is the key to find the delayed Runnable task in the storage.
     */
    protected void runDelayed(AudioStream audioStream) {
        Runnable delayedTask = runnableByAudioStream.remove(audioStream);

        if (delayedTask != null) {
            delayedTask.run();
        }

        // if the stream is not needed anymore, then we should call back the AudioStream to let it a chance
        // to auto dispose:
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
