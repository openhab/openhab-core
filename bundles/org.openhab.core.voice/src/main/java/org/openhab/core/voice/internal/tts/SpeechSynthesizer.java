/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.voice.internal.tts;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioManager;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.utils.AudioFormatMatcher;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.voice.tts.TTSException;
import org.openhab.core.voice.tts.TTSService;
import org.openhab.core.voice.tts.Voice;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for synthesizing speech and playing it to an audio sink.
 * Encapsulates format matching and delegating synthesis playback to {@link AudioManager}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = SpeechSynthesizer.class)
@NonNullByDefault
public class SpeechSynthesizer {

    private final Logger logger = LoggerFactory.getLogger(SpeechSynthesizer.class);

    private final AudioManager audioManager;

    @Activate
    public SpeechSynthesizer(final @Reference AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    /**
     * Synthesizes and plays the text, catching and logging any {@link TTSException}.
     *
     * @param text text to synthesize
     * @param tts TTS service to use
     * @param voice voice to use
     * @param sink audio sink to play audio on
     * @param volume volume to set during playback, or null to keep the current volume
     * @return a future that completes when playback has finished, or exceptionally if it failed
     */
    public CompletableFuture<@Nullable Void> say(String text, TTSService tts, Voice voice, AudioSink sink,
            @Nullable PercentType volume) {
        try {
            return synthesizeAndPlay(text, tts, voice, sink, volume);
        } catch (TTSException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error saying '{}': {}", text, e.getMessage(), e);
            } else {
                logger.warn("Error saying '{}': {}", text, e.getMessage());
            }
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Synthesizes the given text using the provided TTS service and voice, and plays it on the audio sink.
     *
     * @param text text to synthesize
     * @param tts TTS service to use
     * @param voice voice to use
     * @param sink audio sink to play audio on
     * @param volume volume to set during playback, or null to keep the current volume
     * @return a future that completes when playback has finished, or exceptionally if playback fails
     * @throws TTSException if synthesis fails
     */
    CompletableFuture<@Nullable Void> synthesizeAndPlay(String text, TTSService tts, Voice voice, AudioSink sink,
            @Nullable PercentType volume) throws TTSException {
        Set<AudioFormat> ttsSupportedFormats = tts.getSupportedFormats();
        AudioFormat ttsAudioFormat = AudioFormatMatcher.getBestMatchWithTranscoding(ttsSupportedFormats,
                sink.getSupportedFormats());
        if (ttsAudioFormat == null) {
            throw new TTSException(
                    "No compatible audio format found for TTS '" + tts.getId() + "' and sink '" + sink.getId() + "'");
        }

        AudioStream audioStream = tts.synthesize(text, voice, ttsAudioFormat);
        return audioManager.playAndComplete(audioStream, sink, volume);
    }
}
