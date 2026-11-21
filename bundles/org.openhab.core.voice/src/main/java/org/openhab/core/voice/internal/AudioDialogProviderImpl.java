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
package org.openhab.core.voice.internal;

import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioDialogProvider;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.voice.VoiceManager;
import org.openhab.core.voice.dialog.trigger.BasicDTService;
import org.openhab.core.voice.dialog.trigger.DTListener;
import org.openhab.core.voice.dialog.trigger.DTService;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.DTTriggeredEvent;
import org.openhab.core.voice.dialog.trigger.ks.KSException;
import org.openhab.core.voice.dialog.trigger.ks.KSListener;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.dialog.trigger.ks.KSServiceHandle;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Allows the audio bundle to register a dialog that can be triggered programmatically or by the default keyword
 * spotter.
 *
 * @author Miguel Álvarez - Initial contribution
 * @author Dan Cunningham - Keyword spotting dialogs
 */
@Component(service = AudioDialogProvider.class)
@NonNullByDefault
public class AudioDialogProviderImpl implements AudioDialogProvider {
    private final VoiceManager voiceManager;
    private final LLMToolRegistry llmToolRegistry;

    @Activate
    public AudioDialogProviderImpl(@Reference VoiceManager voiceManager, @Reference LLMToolRegistry llmToolRegistry) {
        this.voiceManager = voiceManager;
        this.llmToolRegistry = llmToolRegistry;
    }

    @Override
    public @Nullable Runnable startDialog(AudioSink audioSink, AudioSource audioSource, @Nullable String locationItem,
            @Nullable String listeningItem, @Nullable Runnable onAbort) {
        return startDialog(audioSink, audioSource, locationItem, listeningItem, onAbort, null);
    }

    @Override
    public @Nullable Runnable startDialog(AudioSink audioSink, AudioSource audioSource, @Nullable String locationItem,
            @Nullable String listeningItem, @Nullable Runnable onAbort, @Nullable Runnable onIdle) {
        DTService dt = new TriggerService(onAbort, onIdle);
        TriggerServiceHandle triggerHandle = (TriggerServiceHandle) voiceManager.startDialog( //
                voiceManager.getDialogContextBuilder() //
                        .withSource(audioSource) //
                        .withSink(audioSink) //
                        .withLocationItem(locationItem) //
                        .withListeningItem(listeningItem) //
                        .withDT(dt) //
                        .withLLMTools(llmToolRegistry.getAll()) //
                        .build() //
        );
        if (triggerHandle == null) {
            return null;
        }
        return triggerHandle::trigger;
    }

    @Override
    public boolean startKeywordSpottingDialog(AudioSink audioSink, AudioSource audioSource,
            @Nullable String locationItem, @Nullable String listeningItem, @Nullable Runnable onAbort) {
        return startKeywordSpottingDialog(audioSink, audioSource, locationItem, listeningItem, onAbort, null);
    }

    @Override
    public boolean startKeywordSpottingDialog(AudioSink audioSink, AudioSource audioSource,
            @Nullable String locationItem, @Nullable String listeningItem, @Nullable Runnable onAbort,
            @Nullable Runnable onIdle) {
        KSService ks = voiceManager.getKS();
        // the builder starts with the default magic word
        return voiceManager.startDialog( //
                voiceManager.getDialogContextBuilder() //
                        .withSource(audioSource) //
                        .withSink(audioSink) //
                        .withLocationItem(locationItem) //
                        .withListeningItem(listeningItem) //
                        .withKS(ks != null ? new NotifyingKSService(ks, onAbort, onIdle) : null) //
                        .build() //
        ) != null;
    }

    private static class TriggerService implements BasicDTService {
        @Nullable
        Runnable onAbort;
        private final @Nullable Runnable onIdle;

        public TriggerService(@Nullable Runnable onAbort, @Nullable Runnable onIdle) {
            this.onAbort = onAbort;
            this.onIdle = onIdle;
        }

        @Override
        public DTServiceHandle registerListener(DTListener dtListener) {
            return new TriggerServiceHandle(dtListener, onAbort, onIdle);
        }

        @Override
        public String getId() {
            return "audiodialog::anonymous::trigger";
        }

        @Override
        public String getLabel(@Nullable Locale locale) {
            // never shown
            return "Anonymous";
        }
    }

    /**
     * Wraps a keyword spotter to run {@code onAbort} when the dialog stops and {@code onIdle} when it is idle.
     */
    private static class NotifyingKSService implements KSService {
        private final KSService ks;
        private final @Nullable Runnable onAbort;
        private final @Nullable Runnable onIdle;

        NotifyingKSService(KSService ks, @Nullable Runnable onAbort, @Nullable Runnable onIdle) {
            this.ks = ks;
            this.onAbort = onAbort;
            this.onIdle = onIdle;
        }

        @Override
        public String getId() {
            return ks.getId();
        }

        @Override
        public String getLabel(@Nullable Locale locale) {
            return ks.getLabel(locale);
        }

        @Override
        public Set<Locale> getSupportedLocales() {
            return ks.getSupportedLocales();
        }

        @Override
        public Set<AudioFormat> getSupportedFormats() {
            return ks.getSupportedFormats();
        }

        @Override
        public KSServiceHandle spot(KSListener ksListener, AudioStream audioStream, Locale locale, String keyword)
                throws KSException {
            KSServiceHandle handle = ks.spot(ksListener, audioStream, locale, keyword);
            return new KSServiceHandle() {
                private volatile boolean aborted;

                @Override
                public void abort() {
                    aborted = true;
                    handle.abort();
                    Runnable onAbort = NotifyingKSService.this.onAbort;
                    if (onAbort != null) {
                        onAbort.run();
                    }
                }

                @Override
                public void onDialogIdle() {
                    if (aborted) {
                        return;
                    }
                    handle.onDialogIdle();
                    Runnable onIdle = NotifyingKSService.this.onIdle;
                    if (onIdle != null) {
                        onIdle.run();
                    }
                }
            };
        }
    }

    private static class TriggerServiceHandle implements DTServiceHandle {
        private final DTListener dtListener;
        private @Nullable Runnable abortCallback;
        private volatile @Nullable Runnable idleCallback;

        public TriggerServiceHandle(DTListener dtListener, @Nullable Runnable abortCallback,
                @Nullable Runnable idleCallback) {
            this.dtListener = dtListener;
            this.abortCallback = abortCallback;
            this.idleCallback = idleCallback;
        }

        public void trigger() {
            dtListener.dtEventReceived(new DTTriggeredEvent());
        }

        @Override
        public void abort() {
            idleCallback = null;
            var abortCallback = this.abortCallback;
            if (abortCallback != null) {
                abortCallback.run();
                this.abortCallback = null;
            }
        }

        @Override
        public void onDialogIdle() {
            var idleCallback = this.idleCallback;
            if (idleCallback != null) {
                idleCallback.run();
            }
        }
    }
}
