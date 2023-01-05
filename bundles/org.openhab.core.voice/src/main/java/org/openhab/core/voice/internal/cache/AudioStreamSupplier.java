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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;

/**
 * Custom supplier class to defer synthesizing with a TTS service
 * This allows calling the {@link TTSService} by the client reading the stream only when needed
 * and thus not blocking the caching service during its standard operation get/push
 *
 * @author Gwendal Roulleau - Initial Contribution
 *
 */
@NonNullByDefault
public class AudioStreamSupplier {

    private final CachedTTSService tts;
    private final String text;
    private final Voice voice;
    private final AudioFormat requestedAudioFormat;
    private boolean resolved = false;

    public AudioStreamSupplier(CachedTTSService tts, String text, Voice voice, AudioFormat requestedAudioFormat) {
        super();
        this.tts = tts;
        this.text = text;
        this.voice = voice;
        this.requestedAudioFormat = requestedAudioFormat;
    }

    public boolean isResolved() {
        return resolved;
    }

    public String getText() {
        return text;
    }

    /**
     * Resolve this supplier. You should use this method only once.
     *
     * @return the {@link AudioStream}
     * @throws TTSException
     */
    public AudioStream resolve() throws TTSException {
        if (resolved) {
            throw new TTSException("This TTS request result have already been supplied");
        }
        AudioStream audioStream = tts.synthesizeForCache(text, voice, requestedAudioFormat);
        resolved = true;
        return audioStream;
    }

    /**
     * If, for any unrecoverable reason, the cache fails, use this method to get the TTS {@link AudioStream} directly
     *
     * @return the AudioStream
     * @throws TTSException
     */
    public AudioStream fallBackDirectResolution() throws TTSException {
        return tts.synthesizeForCache(text, voice, requestedAudioFormat);
    }
}
