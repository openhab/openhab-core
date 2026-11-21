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

import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.trigger.DTErrorEvent;
import org.openhab.core.voice.dialog.trigger.DTEvent;
import org.openhab.core.voice.dialog.trigger.DTException;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.DTTriggeredEvent;
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
import org.openhab.core.voice.tts.TTSException;
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
 * With follow-up listening, a persistent dialog listens again once the answer has played, unless the command
 * couldn't be interpreted, and stops when nothing is heard. The trigger is ignored while the answer plays, and
 * starts a new interaction during a follow-up.
 * <p>
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
 * @author Dan Cunningham - Start speech recognition where the keyword ended
 * @author Dan Cunningham - Listen for a follow-up command after answering
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

    // for sinks that never report the end of playback
    static final long MAX_ANSWER_PLAYBACK_SECONDS = 60;
    // for sources that send no audio, which would otherwise hold the dialog group
    static final long MAX_FOLLOW_UP_SECONDS = 30;

    private final Logger logger = LoggerFactory.getLogger(DialogProcessor.class);

    private final DialogContext dialogContext;
    private final DialogAudioDriver audioDriver;
    private final DialogFeedbackService feedbackService;
    private final InterpretationService interpretationService;
    private final SpeechSynthesizer speechSynthesizer;
    private final VoiceI18nProvider i18nProvider;
    private final DialogEventListener eventListener;
    private final ScheduledExecutorService scheduler;
    private final Executor dialogExecutor;

    private final KSListener ksListener = new KSListener() {
        @Override
        public void dtEventReceived(DTEvent dtEvent) {
            // a trigger ends a follow-up, since some sources stop sending audio once the answer has played
            if (dtEvent instanceof DTTriggeredEvent) {
                endFollowUpOnTrigger();
            }
            // Trigger and error events must only be processed when waiting for triggers so trailing callbacks from
            // aborted streams are discarded when stopped or during an ongoing dialog.
            if (stopped || state != DialogState.WAITING_FOR_TRIGGER) {
                logger.debug("Ignoring {} in state {}", dtEvent.getClass().getSimpleName(), state);
                return;
            }

            if (dtEvent instanceof DTTriggeredEvent) {
                logger.debug("{} event received", dtEvent.getClass().getSimpleName());
                try {
                    startSimpleDialog(dtEvent instanceof KSpottedEvent spotted ? spotted.getAudioAfterKeyword() : null);
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
        private final boolean followUp;
        private volatile @Nullable ScheduledFuture<?> timeout;

        RecognitionSession(boolean followUp) {
            this.followUp = followUp;
        }

        void invalidate() {
            cancelled.set(true);
            ScheduledFuture<?> timeout = this.timeout;
            if (timeout != null) {
                timeout.cancel(false);
            }
        }

        /**
         * Sets the timeout that ends this session, cancelling the previous one.
         */
        void setTimeout(ScheduledFuture<?> timeout) {
            ScheduledFuture<?> previous = this.timeout;
            this.timeout = timeout;
            if (previous != null) {
                previous.cancel(false);
            }
            // the session may have been invalidated before the timeout was stored
            if (isCancelled()) {
                timeout.cancel(false);
            }
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
                    audioDriver.stopRecognition(this);
                    if (!transitionSession(this, DialogState.INTERPRETING)) {
                        logger.debug("Recognition session was cancelled before interpretation, discarding result");
                        return;
                    }
                    eventListener.onBeforeDialogInterpretation(dialogContext);

                    String answer;
                    boolean interpreted = true;
                    try {
                        answer = interpretationService.interpret(question, dialogContext.hlis(),
                                dialogContext.conversation(), dialogContext.llmTools(), dialogContext.locale(),
                                dialogContext.locationItem(), dialogContext.systemPrompt());
                    } catch (InterpretationException e) {
                        answer = e.getMessage() != null ? e.getMessage() : "Unexpected error";
                        interpreted = false;
                    }

                    if (!transitionSession(this, DialogState.SPEAKING)) {
                        logger.debug("Recognition session was cancelled during interpretation, discarding result");
                        return;
                    }
                    // so background talk doesn't keep the follow-up going
                    if (interpreted && triggerStarted && eventListener.shouldListenForFollowUp(dialogContext)) {
                        sayThenListenForFollowUp(this, answer);
                    } else {
                        say(answer);
                        finishDialog(this);
                    }
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
                    audioDriver.stopRecognition(this);
                    feedbackService.setListening(false);
                    // a follow-up normally ends by hearing nothing, so stay quiet
                    if (!followUp && eventListener.shouldSpeakRecognitionErrors(dialogContext)) {
                        if (!transitionSession(this, DialogState.SPEAKING)) {
                            return;
                        }
                        say(message);
                    }
                    finishDialog(this);
                }
                default -> logger.debug("Unknown STT event received: {}", sttEvent);
            }
        }
    }

    private final Object stateLock = new Object();
    private volatile DialogState state = DialogState.IDLE;
    private volatile boolean stopped = false;
    // a one-shot dialog can have a trigger in its context without waiting for it
    private volatile boolean triggerStarted = false;
    private @Nullable RecognitionSession currentSession;

    public DialogProcessor(DialogContext context, DialogAudioDriver audioDriver, DialogFeedbackService feedbackService,
            InterpretationService interpretationService, SpeechSynthesizer speechSynthesizer,
            VoiceI18nProvider i18nProvider, DialogEventListener eventListener) {
        this(context, audioDriver, feedbackService, interpretationService, speechSynthesizer, i18nProvider,
                eventListener, ThreadPoolManager.getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON),
                ThreadPoolManager.getPool("voice-dialog"));
    }

    DialogProcessor(DialogContext context, DialogAudioDriver audioDriver, DialogFeedbackService feedbackService,
            InterpretationService interpretationService, SpeechSynthesizer speechSynthesizer,
            VoiceI18nProvider i18nProvider, DialogEventListener eventListener, ScheduledExecutorService scheduler,
            Executor dialogExecutor) {
        this.scheduler = scheduler;
        this.dialogExecutor = dialogExecutor;
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
            triggerStarted = true;
        }
        feedbackService.playStartSound();
        try {
            return audioDriver.startTrigger(ksListener);
        } catch (DTException e) {
            logger.warn("Failed to start persistent dialog: {}", e.getMessage(), e);
            synchronized (stateLock) {
                triggerStarted = false;
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
        startSimpleDialog(null);
    }

    /**
     * Starts a single dialog sequence.
     *
     * @param audioAfterKeyword how much audio the keyword spotter read after the keyword, or null when unknown
     */
    private void startSimpleDialog(@Nullable Duration audioAfterKeyword) {
        synchronized (stateLock) {
            if (stopped) {
                return;
            }
        }
        if (!eventListener.onAcquireGroup(dialogContext)) {
            logger.warn("Ignoring dialog start event, dialog group '{}' running", dialogContext.dialogGroup());
            return;
        }
        RecognitionSession session = new RecognitionSession(false);
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
            if (audioAfterKeyword != null) {
                audioDriver.startRecognition(session, audioAfterKeyword);
            } else {
                audioDriver.startRecognition(session);
            }
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
        // play in the background: a keyword spotter may call this from the thread that feeds the recognition
        dialogExecutor.execute(() -> {
            if (state == DialogState.LISTENING && !session.isCancelled()) {
                feedbackService.playListeningSound();
            }
        });
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
     * Speaks the answer, then listens for a follow-up once it has played. Keeps the dialog group until the follow-up
     * ends.
     */
    private void sayThenListenForFollowUp(RecognitionSession session, @Nullable String answer) {
        session.setTimeout(scheduler.schedule(() -> {
            logger.debug("Not listening for a follow-up, the answer didn't finish playing within {}s",
                    MAX_ANSWER_PLAYBACK_SECONDS);
            finishDialog(session);
        }, MAX_ANSWER_PLAYBACK_SECONDS, TimeUnit.SECONDS));
        say(answer).whenCompleteAsync((result, error) -> {
            if (error != null) {
                logger.debug("Not listening for a follow-up, playing the answer failed: {}", error.getMessage());
                finishDialog(session);
            } else {
                listenForFollowUp(session);
            }
        }, dialogExecutor);
    }

    private void endFollowUpOnTrigger() {
        RecognitionSession session;
        synchronized (stateLock) {
            session = currentSession;
        }
        if (session != null) {
            endFollowUp(session, "Trigger received while listening for a follow-up");
        }
    }

    /**
     * Ends a follow-up that is still listening, without speaking.
     */
    private void endFollowUp(RecognitionSession session, String reason) {
        synchronized (stateLock) {
            if (stopped || !session.equals(currentSession) || !session.followUp) {
                return;
            }
        }
        // a result from the speech-to-text service may already be ending it
        if (!session.terminalHandled.compareAndSet(false, true)) {
            return;
        }
        logger.debug("{}, ending the follow-up", reason);
        // cancel first, so a recognition that is still starting won't start
        session.invalidate();
        audioDriver.stopRecognition(session);
        feedbackService.setListening(false);
        finishDialog(session);
    }

    private void listenForFollowUp(RecognitionSession previous) {
        RecognitionSession session = new RecognitionSession(true);
        synchronized (stateLock) {
            if (stopped || !previous.equals(currentSession) || previous.isCancelled()) {
                return;
            }
            previous.invalidate();
            currentSession = session;
            state = DialogState.LISTENING;
        }
        logger.debug("Listening for a follow-up command");
        session.setTimeout(scheduler.schedule(
                () -> endFollowUp(session, "Nothing recognized within " + MAX_FOLLOW_UP_SECONDS + "s"),
                MAX_FOLLOW_UP_SECONDS, TimeUnit.SECONDS));
        try {
            audioDriver.startRecognition(session, null, session::isCancelled);
        } catch (STTException e) {
            logger.debug("Failed to start follow-up recognition: {}", e.getMessage());
            finishDialog(session);
            return;
        } catch (RuntimeException e) {
            logger.debug("Unexpected error starting follow-up recognition: {}", e.getMessage(), e);
            finishDialog(session);
            return;
        }
        if (state == DialogState.LISTENING && !session.isCancelled()) {
            feedbackService.playListeningSound();
        }
    }

    /**
     * Speaks the passed text via {@link SpeechSynthesizer}.
     *
     * @param text The text to say
     * @return a future that completes when playback has finished, or exceptionally if the text can't be spoken
     */
    protected CompletableFuture<@Nullable Void> say(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            logger.debug("Empty value, nothing to say");
            return CompletableFuture.completedFuture(null);
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
            return CompletableFuture
                    .failedFuture(new TTSException("No voice for language " + dialogContext.locale().getLanguage()));
        }
        return speechSynthesizer.say(text, dialogContext.tts(), voice, dialogContext.sink(), null);
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
         * Tells whether to speak the speech-to-text error when recognition fails.
         *
         * @param context used by the dialog processor
         * @return true to speak the error
         */
        boolean shouldSpeakRecognitionErrors(DialogContext context);

        /**
         * Runs whenever the dialog is stopped.
         *
         * @param context used by the dialog processor
         */
        void onDialogStopped(DialogContext context);

        /**
         * Tells whether a persistent dialog should listen for a follow-up after answering.
         *
         * @param context used by the dialog processor
         * @return true to listen for a follow-up
         */
        boolean shouldListenForFollowUp(DialogContext context);
    }
}
