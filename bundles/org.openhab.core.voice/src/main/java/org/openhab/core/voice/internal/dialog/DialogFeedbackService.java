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
package org.openhab.core.voice.internal.dialog;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.audio.utils.AudioFormatMatcher;
import org.openhab.core.audio.utils.ToneSynthesizer;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.voice.dialog.DialogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles audio tone synthesis and listening item visual feedback for a dialog.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class DialogFeedbackService {

    private final Logger logger = LoggerFactory.getLogger(DialogFeedbackService.class);

    private final DialogContext dialogContext;
    private final EventPublisher eventPublisher;

    private @Nullable ToneSynthesizer toneSynthesizer;
    private @Nullable List<ToneSynthesizer.Tone> listeningMelody;
    private boolean listening = false;

    public DialogFeedbackService(DialogContext dialogContext, EventPublisher eventPublisher) {
        this.dialogContext = dialogContext;
        this.eventPublisher = eventPublisher;
        initToneSynthesizer(dialogContext.listeningMelody());
    }

    private void initToneSynthesizer(@Nullable String listeningMelodyText) {
        @Nullable
        List<ToneSynthesizer.Tone> listeningMelody = null;
        ToneSynthesizer toneSynthesizer = null;
        if (listeningMelodyText != null && !listeningMelodyText.isBlank()) {
            try {
                listeningMelody = ToneSynthesizer.parseMelody(listeningMelodyText);
                var synthesizerFormat = AudioFormatMatcher.getBestMatch(ToneSynthesizer.getSupportedFormats(),
                        dialogContext.sink().getSupportedFormats());
                if (synthesizerFormat != null) {
                    toneSynthesizer = new ToneSynthesizer(synthesizerFormat);
                    logger.debug("Sounds enabled");
                } else {
                    logger.warn("Sounds disabled, synthesizer is not compatible with this sink");
                }
            } catch (ParseException e) {
                logger.warn("Sounds disabled, unable to parse 'listening' melody: {}", e.getMessage());
            }
        }
        this.toneSynthesizer = toneSynthesizer;
        this.listeningMelody = listeningMelody;
    }

    /**
     * Plays the start sound melody on the configured audio sink.
     */
    public void playStartSound() {
        playNotes(Stream.of(ToneSynthesizer.Note.G, ToneSynthesizer.Note.A, ToneSynthesizer.Note.B)
                .map(note -> ToneSynthesizer.noteTone(note, 100L)).toList());
    }

    /**
     * Plays the stop sound melody on the configured audio sink.
     */
    public void playStopSound() {
        playNotes(Stream.of(ToneSynthesizer.Note.B, ToneSynthesizer.Note.A, ToneSynthesizer.Note.G)
                .map(note -> ToneSynthesizer.noteTone(note, 100L)).toList());
    }

    /**
     * Plays the listening melody on the configured audio sink if configured.
     */
    public void playListeningSound() {
        var melody = this.listeningMelody;
        if (melody != null) {
            playNotes(melody);
        }
    }

    private void playNotes(List<ToneSynthesizer.Tone> notes) {
        var synthesizer = this.toneSynthesizer;
        if (synthesizer != null) {
            try (AudioStream stream = synthesizer.getStream(notes)) {
                var sink = dialogContext.sink();
                if (sink.getSupportedStreams().stream().anyMatch(clazz -> clazz.isInstance(stream))) {
                    sink.process(stream);
                } else {
                    logger.warn("Failed playing synthesizer sound as audio sink doesn't support it.");
                }
            } catch (UnsupportedAudioFormatException | UnsupportedAudioStreamException | IOException e) {
                logger.warn("{} playing synthesizer sound: {}", e.getClass().getName(), e.getMessage());
            }
        }
    }

    /**
     * Updates the listening item indicator state via {@link EventPublisher}.
     *
     * @param listening true when listening, false otherwise
     */
    public void setListening(boolean listening) {
        if (this.listening == listening) {
            return;
        }
        this.listening = listening;
        String item = dialogContext.listeningItem();
        if (ItemUtil.isValidItemName(item)) {
            eventPublisher.post(ItemEventFactory.createCommandEvent(item, OnOffType.from(listening)));
        }
    }
}
