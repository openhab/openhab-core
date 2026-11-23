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
package org.openhab.core.voice.internal.stt;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.utils.AudioFormatMatcher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.voice.stt.RecognitionStartEvent;
import org.openhab.core.voice.stt.RecognitionStopEvent;
import org.openhab.core.voice.stt.STTException;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.stt.STTServiceHandle;
import org.openhab.core.voice.stt.SpeechRecognitionErrorEvent;
import org.openhab.core.voice.stt.SpeechRecognitionEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for transcribing audio sources and streams to text using a speech-to-text service.
 * Encapsulates audio format matching, listener lifecycle, and timeout/abort management.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = AudioTranscriber.class)
@NonNullByDefault
public class AudioTranscriber {

    private final Logger logger = LoggerFactory.getLogger(AudioTranscriber.class);

    private final LocaleProvider localeProvider;

    @Activate
    public AudioTranscriber(final @Reference LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    /**
     * Transcribes speech from the given audio source.
     *
     * @param audioSource the audio source to record from
     * @param sttService the speech-to-text service to use
     * @param locale the locale to recognize
     * @return the transcribed text or empty string on failure
     */
    public String transcribe(AudioSource audioSource, STTService sttService, @Nullable Locale locale) {
        var sttFormat = AudioFormatMatcher.getBestMatch(audioSource.getSupportedFormats(),
                sttService.getSupportedFormats());
        if (sttFormat == null) {
            logger.warn("No compatible audio format found for STT '{}' and the provided audio source '{}'",
                    sttService.getId(), audioSource.getId());
            return "";
        }
        AudioStream audioStream;
        try {
            audioStream = audioSource.getInputStream(sttFormat);
        } catch (AudioException e) {
            logger.warn("AudioException creating source audio stream: {}", e.getMessage());
            return "";
        }
        return doTranscribe(audioStream, sttService, locale);
    }

    /**
     * Transcribes speech from the given audio stream.
     *
     * @param audioStream the audio stream to transcribe
     * @param sttService the speech-to-text service to use
     * @param locale the locale to recognize
     * @return the transcribed text or empty string on failure
     */
    public String transcribe(AudioStream audioStream, STTService sttService, @Nullable Locale locale) {
        var sttFormat = AudioFormatMatcher.getBestMatch(Set.of(audioStream.getFormat()),
                sttService.getSupportedFormats());
        if (sttFormat == null) {
            logger.warn("No compatible audio format found for STT '{}' and the provided audio stream",
                    sttService.getId());
            return "";
        }
        return doTranscribe(audioStream, sttService, locale);
    }

    private String doTranscribe(AudioStream audioStream, STTService sttService, @Nullable Locale locale) {
        Locale nullSafeLocale = locale != null ? locale : localeProvider.getLocale();
        CompletableFuture<String> transcriptionResult = new CompletableFuture<>();
        STTServiceHandle sttServiceHandle;
        try {
            sttServiceHandle = sttService.recognize(sttEvent -> {
                switch (sttEvent) {
                    case SpeechRecognitionEvent sre -> {
                        logger.debug("SpeechRecognitionEvent event received");
                        String transcript = sre.getTranscript();
                        logger.debug("Text recognized: {}", transcript);
                        transcriptionResult.complete(transcript);
                    }
                    case RecognitionStartEvent recognitionStartEvent ->
                        logger.debug("RecognitionStartEvent event received");
                    case RecognitionStopEvent recognitionStopEvent ->
                        logger.debug("RecognitionStopEvent event received");
                    case SpeechRecognitionErrorEvent sre -> {
                        logger.debug("SpeechRecognitionErrorEvent event received");
                        transcriptionResult.completeExceptionally(
                                new IOException("SpeechRecognitionErrorEvent emitted: " + sre.getMessage()));
                    }
                    default -> logger.debug("Unknown STT event received: {}", sttEvent);
                }
            }, audioStream, nullSafeLocale);
        } catch (STTException e) {
            logger.warn("STT service failed to transcribe: {}", e.getMessage());
            return "";
        }
        try {
            return transcriptionResult.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Interrupted waiting for transcription: {}", e.getMessage());
            sttServiceHandle.abort();
        } catch (ExecutionException e) {
            var cause = e.getCause();
            logger.warn("ExecutionException running transcription: {}",
                    cause != null ? cause.getMessage() : e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("Timed out while waiting for transcription");
            sttServiceHandle.abort();
        }
        return "";
    }
}
