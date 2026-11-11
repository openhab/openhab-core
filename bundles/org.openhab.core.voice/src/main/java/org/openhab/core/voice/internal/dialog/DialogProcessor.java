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

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.trigger.DTErrorEvent;
import org.openhab.core.voice.dialog.trigger.DTEvent;
import org.openhab.core.voice.dialog.trigger.DTException;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.DTTriggeredEvent;
import org.openhab.core.voice.dialog.trigger.ks.KSEvent;
import org.openhab.core.voice.dialog.trigger.ks.KSListener;
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
public class DialogProcessor {

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

    private final KSListener ksListener = new KSListener() {
        @Override
        public void dtEventReceived(DTEvent dtEvent) {
            // Trigger and error events must only be processed when waiting for triggers so trailing callbacks from
            // aborted streams are discarded when stopped or during an ongoing dialog.
            if (stopped || state != DialogState.WAITING_FOR_TRIGGER) {
                logger.debug("Ignoring {} in state {}", dtEvent.getClass().getSimpleName(), state);
                return;
            }

            if (dtEvent instanceof DTTriggeredEvent) {
                logger.debug("{} event received", dtEvent.getClass().getSimpleName());
                try {
                    startSimpleDialog();
                } catch (IllegalStateException e) {
                    logger.warn("{}", e.getMessage());
                }
            } else if (dtEvent instanceof DTErrorEvent dte) {
                logger.debug("{} event received", dte.getClass().getSimpleName());
                String text = i18nProvider.getFormattedText("error.ks-error", dialogContext.locale(), dte.getMessage());
                say(text);
            }
        }

        @Override
        public void ksEventReceived(KSEvent ksEvent) {
            dtEventReceived(ksEvent);
        }
    };

    /**
     * Tracks the lifecycle of an individual speech recognition episode.
     * <p>
     * Atomic cancellation and session invalidation are used to coordinate recognition callbacks with lifecycle
     * transitions:
     * <ul>
     * <li>{@link #stop()} must remain non-blocking so framework deactivation and dialog teardown complete
     * promptly without waiting on long-running interpretation, network, or LLM calls.</li>
     * <li>Locks must not be held across driver calls such as abort to avoid lock-inversion deadlocks with STT
     * audio streaming threads.</li>
     * <li>Dedicated session tokens allow trailing events from aborted streams (such as stream-closed errors) to be
     * correlated and discarded so they do not affect subsequent recognition attempts or stopped processors.</li>
     * </ul>
     */
    private class RecognitionSession implements STTListener {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean terminalHandled = new AtomicBoolean(false);

        void invalidate() {
            cancelled.set(true);
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public void sttEventReceived(STTEvent sttEvent) {
            if (isCancelled()) {
                logger.debug("Ignoring STT event {} for cancelled session", sttEvent.getClass().getSimpleName());
                return;
            }

            switch (sttEvent) {
                case SpeechRecognitionEvent sre -> {
                    if (!terminalHandled.compareAndSet(false, true)) {
                        return;
                    }
                    logger.debug("SpeechRecognitionEvent event received");
                    String question = sre.getTranscript();
                    logger.debug("Text recognized: {}", question);
                    feedbackService.setListening(false);
                    audioDriver.stopRecognition();
                    if (!transitionSession(this, DialogState.INTERPRETING)) {
                        logger.debug("Recognition session was cancelled before interpretation, discarding result");
                        return;
                    }
                    eventListener.onBeforeDialogInterpretation(dialogContext);

                    String answer;
                    try {
                        answer = interpretationService.interpret(question, dialogContext.hlis(),
                                dialogContext.conversation(), dialogContext.llmTools(), dialogContext.locale(),
                                dialogContext.locationItem(), dialogContext.systemPrompt());
                    } catch (InterpretationException e) {
                        answer = e.getMessage() != null ? e.getMessage() : "Unexpected error";
                    }

                    if (!transitionSession(this, DialogState.SPEAKING)) {
                        logger.debug("Recognition session was cancelled during interpretation, discarding result");
                        return;
                    }
                    say(answer);
                    finishDialog(this);
                }
                case RecognitionStartEvent recognitionStartEvent -> {
                    if (!terminalHandled.get()) {
                        logger.debug("RecognitionStartEvent event received");
                        feedbackService.setListening(true);
                    }
                }
                case RecognitionStopEvent recognitionStopEvent -> {
                    if (!terminalHandled.get()) {
                        logger.debug("RecognitionStopEvent event received");
                        feedbackService.setListening(false);
                    }
                }
                case SpeechRecognitionErrorEvent sre -> {
                    if (!terminalHandled.compareAndSet(false, true)) {
                        return;
                    }
                    String message = sre.getMessage();
                    logger.debug("SpeechRecognitionErrorEvent event received: {}", message);
                    audioDriver.stopRecognition();
                    feedbackService.setListening(false);
                    if (!transitionSession(this, DialogState.SPEAKING)) {
                        return;
                    }
                    say(message);
                    finishDialog(this);
                }
                default -> logger.debug("Unknown STT event received: {}", sttEvent);
            }
        }
    }

    private final Object stateLock = new Object();
    private volatile DialogState state = DialogState.IDLE;
    private volatile boolean stopped = false;
    private @Nullable RecognitionSession currentSession;

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
        synchronized (stateLock) {
            if (stopped) {
                return null;
            }
            state = DialogState.WAITING_FOR_TRIGGER;
        }
        feedbackService.playStartSound();
        try {
            return audioDriver.startTrigger(ksListener);
        } catch (DTException e) {
            logger.warn("Failed to start persistent dialog: {}", e.getMessage(), e);
            synchronized (stateLock) {
                if (state == DialogState.WAITING_FOR_TRIGGER) {
                    state = DialogState.IDLE;
                }
            }
            return null;
        }
    }

    /**
     * Starts a single dialog sequence (trigger spotted or direct listening).
     */
    public void startSimpleDialog() {
        synchronized (stateLock) {
            if (stopped) {
                return;
            }
        }
        if (!eventListener.onAcquireGroup(dialogContext)) {
            logger.warn("Ignoring dialog start event, dialog group '{}' running", dialogContext.dialogGroup());
            return;
        }
        RecognitionSession session = new RecognitionSession();
        synchronized (stateLock) {
            if (stopped) {
                session.invalidate();
            } else {
                if (currentSession != null) {
                    currentSession.invalidate();
                }
                currentSession = session;
                state = DialogState.LISTENING;
            }
        }
        if (session.isCancelled()) {
            eventListener.onReleaseGroup(dialogContext);
            return;
        }
        try {
            audioDriver.startRecognition(session);
        } catch (STTException e) {
            logger.warn("Failed to start recognition, stopping dialog: {}", e.getMessage(), e);
            if (transitionSession(session, DialogState.SPEAKING)) {
                String text = i18nProvider.getFormattedText("error.stt-exception", dialogContext.locale(),
                        e.getMessage());
                say(text);
            }
            finishDialog(session);
            return;
        }
        if (state == DialogState.LISTENING && !session.isCancelled()) {
            feedbackService.playListeningSound();
        }
    }

    /**
     * Stops any dialog execution.
     */
    public void stop() {
        // Invalidate the active recognition session and commit IDLE state before aborting so any synchronous or
        // asynchronous callbacks emitted during abort are rejected.
        synchronized (stateLock) {
            stopped = true;
            if (currentSession != null) {
                currentSession.invalidate();
                currentSession = null;
            }
            state = DialogState.IDLE;
        }
        audioDriver.stopAll();
        feedbackService.setListening(false);
        feedbackService.playStopSound();
        eventListener.onReleaseGroup(dialogContext);
        eventListener.onDialogStopped(dialogContext);
    }

    /**
     * Returns the dialog context used by this processor.
     */
    public DialogContext getContext() {
        return dialogContext;
    }

    /**
     * Indicates if the dialog is actively processing (listening, interpreting, or speaking).
     *
     * @return true if the dialog is in {@link DialogState#LISTENING}, {@link DialogState#INTERPRETING}, or
     *         {@link DialogState#SPEAKING}
     */
    public boolean isProcessing() {
        DialogState s = state;
        return s == DialogState.LISTENING || s == DialogState.INTERPRETING || s == DialogState.SPEAKING;
    }

    private boolean transitionSession(RecognitionSession session, DialogState nextState) {
        synchronized (stateLock) {
            if (stopped || !session.equals(currentSession) || session.isCancelled()) {
                return false;
            }
            state = nextState;
            return true;
        }
    }

    private void finishDialog(RecognitionSession session) {
        boolean notifyStopped = false;
        synchronized (stateLock) {
            if (stopped || !session.equals(currentSession)) {
                return;
            }
            session.invalidate();
            currentSession = null;
            if (dialogContext.dt() != null && dialogContext.keyword() != null) {
                state = DialogState.WAITING_FOR_TRIGGER;
            } else {
                state = DialogState.IDLE;
                notifyStopped = true;
            }
        }
        eventListener.onReleaseGroup(dialogContext);
        if (notifyStopped) {
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
                && new HashSet<>(dialogContext.hlis()).containsAll(dialogProcessor.dialogContext.hlis())
                && dialogContext.locale().equals(dialogProcessor.dialogContext.locale())
                && Objects.equals(dialogContext.listeningItem(), dialogProcessor.dialogContext.listeningItem());
    }

    public interface DialogEventListener {
        /**
         * Attempts to acquire the dialog group for the dialog interaction.
         *
         * @param context used by the dialog processor
         * @return true if the group was acquired, false if another dialog is already active in that group
         */
        boolean onAcquireGroup(DialogContext context);

        /**
         * Releases the dialog group when an active dialog interaction finishes.
         *
         * @param context used by the dialog processor
         */
        void onReleaseGroup(DialogContext context);

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
