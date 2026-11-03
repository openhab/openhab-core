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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.trigger.DTErrorEvent;
import org.openhab.core.voice.dialog.trigger.DTEvent;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.DTTriggeredEvent;
import org.openhab.core.voice.dialog.trigger.ks.KSErrorEvent;
import org.openhab.core.voice.dialog.trigger.ks.KSEvent;
import org.openhab.core.voice.dialog.trigger.ks.KSListener;
import org.openhab.core.voice.dialog.trigger.ks.KSpottedEvent;
import org.openhab.core.voice.internal.VoiceI18nProvider;
import org.openhab.core.voice.internal.text.InterpretationService;
import org.openhab.core.voice.internal.tts.SpeechSynthesizer;
import org.openhab.core.voice.stt.RecognitionStartEvent;
import org.openhab.core.voice.stt.RecognitionStopEvent;
import org.openhab.core.voice.stt.STTEvent;
import org.openhab.core.voice.stt.STTException;
import org.openhab.core.voice.stt.STTListener;
import org.openhab.core.voice.stt.SpeechRecognitionErrorEvent;
import org.openhab.core.voice.stt.SpeechRecognitionEvent;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.tts.Voice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles a dialog with the user as a pure state machine orchestrating trigger listening, speech recognition,
 * human language interpretation, and speech synthesis.
 * <p>
 * The processor operates through the following states:
 * <ul>
 * <li>{@link DialogState#IDLE}: The initial state and the resting state for one-shot (simple) dialogs. Also reached
 * when the dialog is explicitly stopped.</li>
 * <li>{@link DialogState#WAITING_FOR_TRIGGER}: Active state for persistent dialogs waiting for keyword spotting or
 * an external dialog trigger.</li>
 * <li>{@link DialogState#LISTENING}: Speech-to-text recognition is actively listening and streaming audio from the
 * audio source.</li>
 * <li>{@link DialogState#INTERPRETING}: Audio stream is closed; the recognized transcription text is being processed
 * by the {@link InterpretationService} across configured human language interpreters and LLM tools.</li>
 * <li>{@link DialogState#SPEAKING}: The interpretation result or an error message is synthesized and played to the
 * audio sink via {@link SpeechSynthesizer}.</li>
 * </ul>
 * <p>
 * <b>State Transitions:</b>
 * 
 * <pre>
 *                 +---------------------+
 *                 |        IDLE         |&lt;--------------------------+
 *                 +---------------------+                           |
 *                    | (start)     | (startSimpleDialog)            |
 *                    v             v                                |
 *   +---------------------+   +---------------------+               |
 *   | WAITING_FOR_TRIGGER |   |      LISTENING      |               |
 *   +---------------------+   +---------------------+               |
 *     ^        | (triggered)      ^             |                   |
 *     |        |                  |             |                   |
 *     |        +------------------+             | (STT recognized)  |
 *     |                                         v                   |
 *     |                               +---------------------+       |
 *     |                               |    INTERPRETING     |       |
 *     |                               +---------------------+       |
 *     |                                         | (interpreted)     |
 *     |                                         v                   |
 *     |                               +---------------------+       |
 *     |                               |      SPEAKING       |       |
 *     |                               +---------------------+       |
 *     |                                         |                   |
 *     |                                         v (finishDialog)    |
 *     |                                   [persistent?]             |
 *     |                                      /     \                |
 *     |                              yes    /       \  no           |
 *     +------------------------------------+         +--------------+
 * </pre>
 * 
 * Any active state transitions immediately to {@link DialogState#IDLE} upon calling {@link #stop()}.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Yannick Schaus - Send commands to an item to indicate the keyword has been spotted
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Laurent Garnier - Added stop() + null annotations + resources releasing
 * @author Miguel Álvarez - Close audio streams + use RecognitionStartEvent
 * @author Miguel Álvarez - Use dialog context
 * @author Miguel Álvarez - Add sounds
 * @author Miguel Álvarez - Add dialog groups
 * @author Florian Hotze - Refactor into pure state machine
 */
@NonNullByDefault
public class DialogProcessor implements KSListener, STTListener {

    /**
     * States of the dialog state machine.
     */
    public enum DialogState {
        IDLE,
        WAITING_FOR_TRIGGER,
        LISTENING,
        INTERPRETING,
        SPEAKING
    }

    private final Logger logger = LoggerFactory.getLogger(DialogProcessor.class);

    private final DialogContext dialogContext;
    private final DialogAudioDriver audioDriver;
    private final DialogFeedbackService feedbackService;
    private final InterpretationService interpretationService;
    private final SpeechSynthesizer speechSynthesizer;
    private final VoiceI18nProvider i18nProvider;
    private final DialogEventListener eventListener;

    private DialogState state = DialogState.IDLE;

    public DialogProcessor(DialogContext context, DialogAudioDriver audioDriver, DialogFeedbackService feedbackService,
            InterpretationService interpretationService, SpeechSynthesizer speechSynthesizer,
            VoiceI18nProvider i18nProvider, DialogEventListener eventListener) {
        this.dialogContext = context;
        this.audioDriver = audioDriver;
        this.feedbackService = feedbackService;
        this.interpretationService = interpretationService;
        this.speechSynthesizer = speechSynthesizer;
        this.i18nProvider = i18nProvider;
        this.eventListener = eventListener;
    }

    /**
     * Returns the current state of the dialog state machine.
     */
    public DialogState getState() {
        return state;
    }

    /**
     * Starts a persistent dialog.
     *
     * @return {@link DTServiceHandle} or null if dialog fails to start
     * @throws IllegalStateException if keyword spot service is misconfigured
     */
    public @Nullable DTServiceHandle start() throws IllegalStateException {
        state = DialogState.WAITING_FOR_TRIGGER;
        DTServiceHandle handle = audioDriver.startTrigger(this);
        feedbackService.playStartSound();
        return handle;
    }

    /**
     * Starts a single dialog sequence (trigger spotted or direct listening).
     */
    public void startSimpleDialog() {
        state = DialogState.LISTENING;
        feedbackService.playListeningSound();
        try {
            audioDriver.startRecognition(this);
        } catch (STTException e) {
            String text = i18nProvider.getFormattedText("error.stt-exception", dialogContext.locale(), e.getMessage());
            say(text);
            finishDialog();
        }
    }

    /**
     * Stops any dialog execution.
     */
    public void stop() {
        audioDriver.stopAll();
        feedbackService.setListening(false);
        feedbackService.playStopSound();
        state = DialogState.IDLE;
        eventListener.onDialogStopped(dialogContext);
    }

    /**
     * Returns the dialog context used by this processor.
     */
    public DialogContext getContext() {
        return dialogContext;
    }

    /**
     * Indicates if speech recognition is currently listening.
     */
    public boolean isProcessing() {
        return state == DialogState.LISTENING;
    }

    @Override
    public void dtEventReceived(DTEvent dtEvent) {
        if (dtEvent instanceof DTTriggeredEvent) {
            logger.debug("{} event received",
                    (dtEvent instanceof KSpottedEvent) ? "KSpottedEvent" : "DTTriggeredEvent");
            try {
                startSimpleDialog();
            } catch (IllegalStateException e) {
                logger.warn("{}", e.getMessage());
            }
        } else if (dtEvent instanceof DTErrorEvent dte) {
            logger.debug("{} event received", dte instanceof KSErrorEvent ? "KSErrorEvent" : "DTErrorEvent");
            String text = i18nProvider.getFormattedText("error.ks-error", dialogContext.locale(), dte.getMessage());
            say(text);
        }
    }

    @Override
    public void ksEventReceived(KSEvent ksEvent) {
        dtEventReceived(ksEvent);
    }

    @Override
    public synchronized void sttEventReceived(STTEvent sttEvent) {
        if (sttEvent instanceof SpeechRecognitionEvent sre) {
            logger.debug("SpeechRecognitionEvent event received");
            String question = sre.getTranscript();
            logger.debug("Text recognized: {}", question);
            feedbackService.setListening(false);
            audioDriver.stopRecognition();
            state = DialogState.INTERPRETING;
            eventListener.onBeforeDialogInterpretation(dialogContext);
            try {
                String answer = interpretationService.interpret(question, dialogContext.hlis(),
                        dialogContext.conversation(), dialogContext.llmTools(), dialogContext.locale(),
                        dialogContext.locationItem(), dialogContext.systemPrompt());
                state = DialogState.SPEAKING;
                say(answer);
            } catch (InterpretationException e) {
                String error = e.getMessage();
                state = DialogState.SPEAKING;
                say(error != null ? error : "Unexpected error");
            }
            finishDialog();
        } else if (sttEvent instanceof RecognitionStartEvent) {
            logger.debug("RecognitionStartEvent event received");
            feedbackService.setListening(true);
        } else if (sttEvent instanceof RecognitionStopEvent) {
            logger.debug("RecognitionStopEvent event received");
            feedbackService.setListening(false);
        } else if (sttEvent instanceof SpeechRecognitionErrorEvent sre) {
            String message = sre.getMessage();
            logger.debug("SpeechRecognitionErrorEvent event received: {}", message);
            audioDriver.stopRecognition();
            feedbackService.setListening(false);
            if (!message.isEmpty()) {
                say(message);
            }
            finishDialog();
        }
    }

    private void finishDialog() {
        if (dialogContext.dt() != null && dialogContext.keyword() != null) {
            state = DialogState.WAITING_FOR_TRIGGER;
        } else {
            state = DialogState.IDLE;
            eventListener.onDialogStopped(dialogContext);
        }
    }

    /**
     * Speaks the passed text via {@link SpeechSynthesizer}.
     *
     * @param text The text to say
     */
    protected void say(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            logger.debug("Empty value, nothing to say");
            return;
        }
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
            logger.warn("Unable to find a suitable voice for language {}", dialogContext.locale().getLanguage());
            return;
        }
        speechSynthesizer.say(text, dialogContext.tts(), voice, dialogContext.sink(), null);
    }

    /**
     * Check if other DialogProcessor instance has the same configuration ignoring keyword spotting configuration.
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
         * Runs before starting to interpret the transcription result.
         *
         * @param context used by the dialog processor
         */
        void onBeforeDialogInterpretation(DialogContext context);

        /**
         * Runs whenever the dialog is stopped.
         *
         * @param context used by the dialog processor
         */
        void onDialogStopped(DialogContext context);
    }
}
