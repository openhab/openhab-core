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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Definition of an audio output like headphones, a speaker or for writing to
 * a file / clip.
 * This version is synchronous: when the process() method returns,
 * the source is considered played, and could be disposed.
 * Any delayed tasks can then be performed, such as volume restoration
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public abstract class AudioSinkSync implements AudioSink {

    private final Logger logger = LoggerFactory.getLogger(AudioSinkSync.class);

    @Override
    public void process(@Nullable AudioStream audioStream, Runnable whenFinished)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {

        try {
            process(audioStream);
        } finally {

            whenFinished.run();

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
}
