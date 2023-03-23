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
package org.openhab.core.voice.internal;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.audio.utils.ToneSynthesizer;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.items.ItemUtil;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.voice.DialogContext;
import org.openhab.core.voice.KSErrorEvent;
import org.openhab.core.voice.KSEvent;
import org.openhab.core.voice.KSException;
import org.openhab.core.voice.KSListener;
import org.openhab.core.voice.KSService;
import org.openhab.core.voice.KSServiceHandle;
import org.openhab.core.voice.KSpottedEvent;
import org.openhab.core.voice.RecognitionStartEvent;
import org.openhab.core.voice.RecognitionStopEvent;
import org.openhab.core.voice.STTEvent;
import org.openhab.core.voice.STTException;
import org.openhab.core.voice.STTListener;
import org.openhab.core.voice.STTServiceHandle;
import org.openhab.core.voice.SpeechRecognitionErrorEvent;
import org.openhab.core.voice.SpeechRecognitionEvent;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.Voice;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.InterpretationException;
import org.osgi.framework.Bundle;
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
 * @author Laurent Garnier - Added stop() + null annotations + resources releasing
 * @author Miguel Álvarez - Close audio streams + use RecognitionStartEvent
 * @author Miguel Álvarez - Use dialog context
 * @author Miguel Álvarez - Add sounds
 *
 */
@NonNullByDefault
public class DialogProcessor implements KSListener, STTListener {

    private final Logger logger = LoggerFactory.getLogger(DialogProcessor.class);

    public final DialogContext dialogContext;
    private @Nullable List<ToneSynthesizer.Tone> listeningMelody;
    private final EventPublisher eventPublisher;
    private final TranslationProvider i18nProvider;
    private final Bundle bundle;

    private final @Nullable AudioFormat ksFormat;
    private final @Nullable AudioFormat sttFormat;
    private final @Nullable AudioFormat ttsFormat;
    private final DialogEventListener eventListener;

    /**
     * If the processor is currently processing a keyword event and thus should not spot further ones.
     */
    private boolean processing = false;

    /**
     * If the STT server is in the process of aborting
     */
    private boolean isSTTServerAborting = false;

    private @Nullable KSServiceHandle ksServiceHandle;
    private @Nullable STTServiceHandle sttServiceHandle;

    private @Nullable AudioStream streamKS;
    private @Nullable AudioStream streamSTT;
    private @Nullable ToneSynthesizer toneSynthesizer;

    public DialogProcessor(DialogContext context, DialogEventListener eventListener, EventPublisher eventPublisher,
            TranslationProvider i18nProvider, Bundle bundle) {
        this.dialogContext = context;
        this.eventListener = eventListener;
        this.eventPublisher = eventPublisher;
        this.i18nProvider = i18nProvider;
        this.bundle = bundle;
        var ks = context.ks();
        this.ksFormat = ks != null
                ? VoiceManagerImpl.getBestMatch(context.source().getSupportedFormats(), ks.getSupportedFormats())
                : null;
        this.sttFormat = VoiceManagerImpl.getBestMatch(context.source().getSupportedFormats(),
                context.stt().getSupportedFormats());
        this.ttsFormat = VoiceManagerImpl.getBestMatch(context.tts().getSupportedFormats(),
                context.sink().getSupportedFormats());
        initToneSynthesizer(context.listeningMelody());
    }

    private void initToneSynthesizer(@Nullable String listeningMelodyText) {
        @Nullable
        List<ToneSynthesizer.Tone> listeningMelody = null;
        ToneSynthesizer toneSynthesizer = null;
        if (listeningMelodyText != null && !listeningMelodyText.isBlank()) {
            try {
                listeningMelody = ToneSynthesizer.parseMelody(listeningMelodyText);
                var synthesizerFormat = VoiceManagerImpl.getBestMatch(ToneSynthesizer.getSupportedFormats(),
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
     * Starts a persistent dialog
     * 
     * @throws IllegalStateException if keyword spot service is misconfigured
     */
    public void start() throws IllegalStateException {
        KSService ksService = dialogContext.ks();
        String keyword = dialogContext.keyword();
        if (ksService != null && keyword != null) {
            abortKS();
            closeStreamKS();
            AudioFormat fmt = ksFormat;
            if (fmt == null) {
                logger.warn("No compatible audio format found for ks '{}' and source '{}'", ksService.getId(),
                        dialogContext.source().getId());
                return;
            }
            try {
                AudioStream stream = dialogContext.source().getInputStream(fmt);
                streamKS = stream;
                ksServiceHandle = ksService.spot(this, stream, dialogContext.locale(), keyword);
                playStartSound();
            } catch (AudioException e) {
                logger.warn("Encountered audio error: {}", e.getMessage());
            } catch (KSException e) {
                logger.warn("Encountered error calling spot: {}", e.getMessage());
                closeStreamKS();
            }
        } else {
            throw new IllegalStateException("Unable to run persistent dialog ks service is not configured");
        }
    }

    /**
     * Starts a single dialog
     */
    public void startSimpleDialog() {
        abortSTT();
        closeStreamSTT();
        isSTTServerAborting = false;
        AudioFormat fmt = sttFormat;
        if (fmt == null) {
            logger.warn("No compatible audio format found for stt '{}' and source '{}'", dialogContext.stt().getId(),
                    dialogContext.source().getId());
            return;
        }
        playOnListeningSound();
        try {
            AudioStream stream = dialogContext.source().getInputStream(fmt);
            streamSTT = stream;
            sttServiceHandle = dialogContext.stt().recognize(this, stream, dialogContext.locale(), new HashSet<>());
        } catch (AudioException e) {
            logger.warn("Error creating the audio stream: {}", e.getMessage());
        } catch (STTException e) {
            closeStreamSTT();
            String msg = e.getMessage();
            String text = i18nProvider.getText(bundle, "error.stt-exception", null, dialogContext.locale());
            if (msg != null) {
                say(text == null ? msg : text.replace("{0}", msg));
            } else if (text != null) {
                say(text.replace("{0}", ""));
            }
        }
    }

    /**
     * Stops any dialog execution
     */
    public void stop() {
        abortSTT();
        closeStreamSTT();
        abortKS();
        closeStreamKS();
        toggleProcessing(false);
        playStopSound();
        eventListener.onDialogStopped(dialogContext);
    }

    /**
     * Indicates if voice recognition is running.
     */
    public boolean isProcessing() {
        return processing;
    }

    private void abortKS() {
        KSServiceHandle handle = ksServiceHandle;
        if (handle != null) {
            handle.abort();
            ksServiceHandle = null;
        }
    }

    private void closeStreamKS() {
        AudioStream stream = streamKS;
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                logger.debug("IOException closing ks audio stream: {}", e.getMessage(), e);
            }
            streamKS = null;
        }
    }

    private void abortSTT() {
        STTServiceHandle handle = sttServiceHandle;
        if (handle != null) {
            handle.abort();
            sttServiceHandle = null;
        }
        isSTTServerAborting = true;
    }

    private void closeStreamSTT() {
        AudioStream stream = streamSTT;
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                logger.debug("IOException closing stt audio stream: {}", e.getMessage(), e);
            }
            streamSTT = null;
        }
    }

    private void toggleProcessing(boolean value) {
        if (processing == value) {
            return;
        }
        processing = value;
        String item = dialogContext.listeningItem();
        if (item != null && ItemUtil.isValidItemName(item)) {
            OnOffType command = (value) ? OnOffType.ON : OnOffType.OFF;
            eventPublisher.post(ItemEventFactory.createCommandEvent(item, command));
        }
    }

    @Override
    public void ksEventReceived(KSEvent ksEvent) {
        if (!processing) {
            isSTTServerAborting = false;
            if (ksEvent instanceof KSpottedEvent) {
                logger.debug("KSpottedEvent event received");
                try {
                    startSimpleDialog();
                } catch (IllegalStateException e) {
                    logger.warn("{}", e.getMessage());
                }
            } else if (ksEvent instanceof KSErrorEvent) {
                logger.debug("KSErrorEvent event received");
                KSErrorEvent kse = (KSErrorEvent) ksEvent;
                String text = i18nProvider.getText(bundle, "error.ks-error", null, dialogContext.locale());
                say(text == null ? kse.getMessage() : text.replace("{0}", kse.getMessage()));
            }
        }
    }

    @Override
    public synchronized void sttEventReceived(STTEvent sttEvent) {
        if (sttEvent instanceof SpeechRecognitionEvent) {
            logger.debug("SpeechRecognitionEvent event received");
            if (!isSTTServerAborting) {
                SpeechRecognitionEvent sre = (SpeechRecognitionEvent) sttEvent;
                String question = sre.getTranscript();
                logger.debug("Text recognized: {}", question);
                toggleProcessing(false);
                eventListener.onBeforeDialogInterpretation(dialogContext);
                String answer = "";
                String error = null;
                for (HumanLanguageInterpreter interpreter : dialogContext.hlis()) {
                    try {
                        answer = interpreter.interpret(dialogContext.locale(), question);
                        logger.debug("Interpretation result: {}", answer);
                        error = null;
                        break;
                    } catch (InterpretationException e) {
                        logger.debug("Interpretation exception: {}", e.getMessage());
                        error = Objects.requireNonNullElse(e.getMessage(), "Unexpected error");
                    }
                }
                say(error != null ? error : answer);
                abortSTT();
            }
        } else if (sttEvent instanceof RecognitionStartEvent) {
            logger.debug("RecognitionStartEvent event received");
            toggleProcessing(true);
        } else if (sttEvent instanceof RecognitionStopEvent) {
            logger.debug("RecognitionStopEvent event received");
            toggleProcessing(false);
        } else if (sttEvent instanceof SpeechRecognitionErrorEvent) {
            logger.debug("SpeechRecognitionErrorEvent event received");
            if (!isSTTServerAborting) {
                abortSTT();
                toggleProcessing(false);
                SpeechRecognitionErrorEvent sre = (SpeechRecognitionErrorEvent) sttEvent;
                String text = i18nProvider.getText(bundle, "error.stt-error", null, dialogContext.locale());
                say(text == null ? sre.getMessage() : text.replace("{0}", sre.getMessage()));
            }
        }
    }

    /**
     * Says the passed command
     *
     * @param text The text to say
     */
    protected void say(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            logger.debug("Empty value, nothing to say");
            return;
        }
        try {
            Voice voice = null;
            for (Voice currentVoice : dialogContext.tts().getAvailableVoices()) {
                if (!dialogContext.locale().getLanguage().equals(currentVoice.getLocale().getLanguage())) {
                    continue;
                }
                var prefVoice = dialogContext.voice();
                if (voice == null || (prefVoice != null && prefVoice.getUID().equals(currentVoice.getUID()))) {
                    voice = currentVoice;
                }
            }
            if (voice == null) {
                throw new TTSException("Unable to find a suitable voice");
            }

            AudioFormat audioFormat = ttsFormat;
            if (audioFormat == null) {
                throw new TTSException("No compatible audio format found for TTS '" + dialogContext.tts().getId()
                        + "' and sink '" + dialogContext.sink().getId() + "'");
            }

            AudioStream audioStream = dialogContext.tts().synthesize(text, voice, audioFormat);

            if (dialogContext.sink().getSupportedStreams().stream().anyMatch(clazz -> clazz.isInstance(audioStream))) {
                try {
                    dialogContext.sink().process(audioStream);
                } catch (UnsupportedAudioFormatException | UnsupportedAudioStreamException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Error saying '{}': {}", text, e.getMessage(), e);
                    } else {
                        logger.warn("Error saying '{}': {}", text, e.getMessage());
                    }
                }
            } else {
                logger.warn("Failed playing audio stream '{}' as audio doesn't support it.", audioStream);
            }
        } catch (TTSException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error saying '{}': {}", text, e.getMessage(), e);
            } else {
                logger.warn("Error saying '{}': {}", text, e.getMessage());
            }
        }
    }

    private void playStartSound() {
        playNotes(Stream.of(ToneSynthesizer.Note.G, ToneSynthesizer.Note.A, ToneSynthesizer.Note.B)
                .map(note -> ToneSynthesizer.noteTone(note, 100L)).collect(Collectors.toList()));
    }

    private void playStopSound() {
        playNotes(Stream.of(ToneSynthesizer.Note.B, ToneSynthesizer.Note.A, ToneSynthesizer.Note.G)
                .map(note -> ToneSynthesizer.noteTone(note, 100L)).collect(Collectors.toList()));
    }

    private void playOnListeningSound() {
        var listeningMelody = this.listeningMelody;
        if (listeningMelody != null) {
            playNotes(listeningMelody);
        }
    }

    private void playNotes(List<ToneSynthesizer.Tone> notes) {
        var toneSynthesizer = this.toneSynthesizer;
        if (toneSynthesizer != null) {
            try (AudioStream stream = toneSynthesizer.getStream(notes)) {
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
     * Check if other DialogProcessor instance have same configuration ignoring the keyword spotting configuration
     *
     * @param dialogProcessor Other DialogProcessor instance
     */
    public boolean isCompatible(DialogProcessor dialogProcessor) {
        return dialogContext.sink().equals(dialogProcessor.dialogContext.sink())
                && dialogContext.source().equals(dialogProcessor.dialogContext.source())
                && dialogContext.stt().equals(dialogProcessor.dialogContext.stt())
                && dialogContext.tts().equals(dialogProcessor.dialogContext.tts())
                && Objects.equals(dialogContext.voice(), dialogProcessor.dialogContext.voice())
                && dialogContext.hlis().size() == dialogProcessor.dialogContext.hlis().size()
                && dialogContext.hlis().containsAll(dialogProcessor.dialogContext.hlis())
                && dialogContext.locale().equals(dialogProcessor.dialogContext.locale())
                && Objects.equals(dialogContext.listeningItem(), dialogProcessor.dialogContext.listeningItem());
    }

    public interface DialogEventListener {
        /**
         * Runs before starting to interpret the transcription result
         *
         * @param context used by the dialog processor
         */
        void onBeforeDialogInterpretation(DialogContext context);

        /**
         * Runs whenever the dialog it stopped
         *
         * @param context used by the dialog processor
         */
        void onDialogStopped(DialogContext context);
    }
}
