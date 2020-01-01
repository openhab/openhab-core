/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.voice.internal;

import java.util.HashSet;
import java.util.Locale;

import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.voice.KSErrorEvent;
import org.openhab.core.voice.KSEvent;
import org.openhab.core.voice.KSException;
import org.openhab.core.voice.KSListener;
import org.openhab.core.voice.KSService;
import org.openhab.core.voice.KSpottedEvent;
import org.openhab.core.voice.RecognitionStopEvent;
import org.openhab.core.voice.STTEvent;
import org.openhab.core.voice.STTException;
import org.openhab.core.voice.STTListener;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.STTServiceHandle;
import org.openhab.core.voice.SpeechRecognitionErrorEvent;
import org.openhab.core.voice.SpeechRecognitionEvent;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An instance of this class can handle a complete dialog with the user. It orchestrates the keyword spotting, the stt
 * and tts services together with the human language interpreter.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Yannick Schaus - Send commands to an item to indicate the keyword has been spotted
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 */
public class DialogProcessor implements KSListener, STTListener {

    private final Logger logger = LoggerFactory.getLogger(DialogProcessor.class);

    /**
     * If the processor is currently processing a keyword event and thus should not spot further ones.
     */
    private boolean processing = false;

    /**
     * If the STT server is in the process of aborting
     */
    private boolean isSTTServerAborting = false;

    private STTServiceHandle sttServiceHandle;

    private final KSService ks;
    private final STTService stt;
    private final TTSService tts;
    private final HumanLanguageInterpreter hli;
    private final AudioSource source;
    private final AudioSink sink;
    private final Locale locale;
    private final String keyword;
    private final String listeningItem;
    private final EventPublisher eventPublisher;

    private final AudioFormat format;

    public DialogProcessor(KSService ks, STTService stt, TTSService tts, HumanLanguageInterpreter hli,
            AudioSource source, AudioSink sink, Locale locale, String keyword, String listeningItem,
            EventPublisher eventPublisher) {
        this.locale = locale;
        this.ks = ks;
        this.hli = hli;
        this.stt = stt;
        this.tts = tts;
        this.source = source;
        this.sink = sink;
        this.keyword = keyword;
        this.listeningItem = listeningItem;
        this.eventPublisher = eventPublisher;
        this.format = AudioFormat.getBestMatch(source.getSupportedFormats(), sink.getSupportedFormats());
    }

    public void start() {
        try {
            ks.spot(this, source.getInputStream(format), locale, this.keyword);
        } catch (KSException e) {
            logger.error("Encountered error calling spot: {}", e.getMessage());
        } catch (AudioException e) {
            logger.error("Error creating the audio stream", e);
        }
    }

    private void toggleProcessing(boolean value) {
        if (this.processing == value) {
            return;
        }
        this.processing = value;
        if (listeningItem != null && ItemUtil.isValidItemName(listeningItem)) {
            OnOffType command = (value) ? OnOffType.ON : OnOffType.OFF;
            eventPublisher.post(ItemEventFactory.createCommandEvent(listeningItem, command));
        }
    }

    @Override
    public void ksEventReceived(KSEvent ksEvent) {
        if (!processing) {
            this.isSTTServerAborting = false;
            if (ksEvent instanceof KSpottedEvent) {
                toggleProcessing(true);
                if (stt != null) {
                    try {
                        this.sttServiceHandle = stt.recognize(this, source.getInputStream(format), this.locale,
                                new HashSet<>());
                    } catch (STTException e) {
                        say("Error during recognition: " + e.getMessage());
                    } catch (AudioException e) {
                        logger.error("Error creating the audio stream", e);
                    }
                }
            } else if (ksEvent instanceof KSErrorEvent) {
                KSErrorEvent kse = (KSErrorEvent) ksEvent;
                say("Encountered error spotting keywords, " + kse.getMessage());
            }
        }
    }

    @Override
    public synchronized void sttEventReceived(STTEvent sttEvent) {
        if (sttEvent instanceof SpeechRecognitionEvent) {
            if (!this.isSTTServerAborting) {
                this.sttServiceHandle.abort();
                this.isSTTServerAborting = true;
                SpeechRecognitionEvent sre = (SpeechRecognitionEvent) sttEvent;
                String question = sre.getTranscript();
                try {
                    toggleProcessing(false);
                    String answer = hli.interpret(this.locale, question);
                    if (answer != null) {
                        say(answer);
                    }
                } catch (InterpretationException e) {
                    say(e.getMessage());
                }
            }
        } else if (sttEvent instanceof RecognitionStopEvent) {
            toggleProcessing(false);
        } else if (sttEvent instanceof SpeechRecognitionErrorEvent) {
            if (!this.isSTTServerAborting) {
                this.sttServiceHandle.abort();
                this.isSTTServerAborting = true;
                toggleProcessing(false);
                SpeechRecognitionErrorEvent sre = (SpeechRecognitionErrorEvent) sttEvent;
                say("Encountered error: " + sre.getMessage());
            }
        }
    }

    /**
     * Says the passed command
     *
     * @param text The text to say
     */
    protected void say(String text) {
        try {
            Voice voice = null;
            for (Voice currentVoice : tts.getAvailableVoices()) {
                if (this.locale.getLanguage().equals(currentVoice.getLocale().getLanguage())) {
                    voice = currentVoice;
                    break;
                }
            }
            if (null == voice) {
                throw new TTSException("Unable to find a suitable voice");
            }
            AudioStream audioStream = tts.synthesize(text, voice, null);

            if (sink.getSupportedStreams().stream().anyMatch(clazz -> clazz.isInstance(audioStream))) {
                try {
                    sink.process(audioStream);
                } catch (UnsupportedAudioFormatException | UnsupportedAudioStreamException e) {
                    logger.warn("Error saying '{}': {}", text, e.getMessage(), e);
                }
            } else {
                logger.warn("Failed playing audio stream '{}' as audio doesn't support it.", audioStream);
            }
        } catch (TTSException e) {
            logger.error("Error saying '{}': {}", text, e.getMessage());
        }
    }

}
