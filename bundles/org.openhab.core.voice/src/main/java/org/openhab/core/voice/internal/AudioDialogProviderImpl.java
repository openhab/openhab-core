/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioDialogProvider;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.voice.BasicDTService;
import org.openhab.core.voice.DTListener;
import org.openhab.core.voice.DTService;
import org.openhab.core.voice.DTServiceHandle;
import org.openhab.core.voice.DTTriggeredEvent;
import org.openhab.core.voice.VoiceManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows the audio bundle to register a dialog that can be triggered programmatically.
 *
 * @author Miguel Álvarez - Initial contribution
 */
@Component(service = AudioDialogProvider.class)
@NonNullByDefault
public class AudioDialogProviderImpl implements AudioDialogProvider {
    private final Logger logger = LoggerFactory.getLogger(AudioDialogProviderImpl.class);
    private final VoiceManager voiceManager;

    @Activate
    public AudioDialogProviderImpl(@Reference VoiceManager voiceManager) {
        this.voiceManager = voiceManager;
    }

    @Override
    public @Nullable Runnable startDialog(AudioSink audioSink, AudioSource audioSource, @Nullable String locationItem,
            @Nullable String listeningItem, @Nullable Runnable onAbort) {
        DTService dt = new TriggerService(onAbort);
        TriggerServiceHandle triggerHandle = (TriggerServiceHandle) voiceManager.startDialog( //
                voiceManager.getDialogContextBuilder() //
                        .withSource(audioSource) //
                        .withSink(audioSink) //
                        .withLocationItem(locationItem) //
                        .withListeningItem(listeningItem) //
                        .withDT(dt) //
                        .build() //
        );
        if (triggerHandle == null) {
            return null;
        }
        return triggerHandle::trigger;
    }

    private static class TriggerService implements BasicDTService {
        @Nullable
        Runnable onAbort;

        public TriggerService(@Nullable Runnable onAbort) {
            this.onAbort = onAbort;
        }

        @Override
        public DTServiceHandle registerListener(DTListener dtListener) {
            return new TriggerServiceHandle(dtListener, onAbort);
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

    private static class TriggerServiceHandle implements DTServiceHandle {
        public final DTListener dtListener;
        public @Nullable Runnable abortCallback;

        public TriggerServiceHandle(DTListener dtListener, @Nullable Runnable abortCallback) {
            this.dtListener = dtListener;
            this.abortCallback = abortCallback;
        }

        public void trigger() {
            dtListener.dtEventReceived(new DTTriggeredEvent());
        }

        @Override
        public void abort() {
            if (this.abortCallback != null) {
                this.abortCallback.run();
                this.abortCallback = null;
            }
        }
    }
}
