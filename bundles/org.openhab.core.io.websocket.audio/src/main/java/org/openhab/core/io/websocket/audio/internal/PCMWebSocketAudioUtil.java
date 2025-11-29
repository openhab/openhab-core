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
package org.openhab.core.io.websocket.audio.internal;

import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Audio utils.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
public final class PCMWebSocketAudioUtil {
    private PCMWebSocketAudioUtil() {
    }

    /**
     * Ensure right PCM format by converting if needed (sample rate, channel)
     *
     * @param sampleRate Stream sample rate
     * @param stream PCM input stream
     * @return A PCM normalized stream at the desired format
     */
    public static AudioInputStream getPCMStreamNormalized(InputStream stream, int sampleRate, int bitDepth,
            int channels, int targetSampleRate, int targetBitDepth, int targetChannels) {
        javax.sound.sampled.AudioFormat jFormat = new javax.sound.sampled.AudioFormat( //
                (float) sampleRate, //
                bitDepth, //
                channels, //
                true, //
                false //
        );
        javax.sound.sampled.AudioFormat fixedJFormat = new javax.sound.sampled.AudioFormat( //
                (float) targetSampleRate, //
                targetBitDepth, //
                targetChannels, //
                true, //
                false //
        );
        return AudioSystem.getAudioInputStream(fixedJFormat, new AudioInputStream(stream, jFormat, -1));
    }
}
