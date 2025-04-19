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
package org.openhab.core.io.websocket.audiopcm;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.voice.KSEdgeService;
import org.openhab.core.voice.KSException;
import org.openhab.core.voice.KSListener;
import org.openhab.core.voice.KSServiceHandle;
import org.openhab.core.voice.KSpottedEvent;
import org.openhab.core.voice.VoiceManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This component provides dialog support to the {@link PCMWebSocketAdapter}.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
@SuppressWarnings("unused")
public class PCMWebSocketDialogProvider implements PCMWebSocketAdapter.DialogProvider {
    private final VoiceManager voiceManager;

    @Activate
    public PCMWebSocketDialogProvider(@Reference VoiceManager voiceManager,
            @Reference PCMWebSocketAdapter pcmWebSocketAdapter) {
        this.voiceManager = voiceManager;
        pcmWebSocketAdapter.setDialogProvider(this);
    }

    /**
     * Starts a dialog and returns a runnable instance that triggers the dialog.
     * 
     * @param sink the audio sink
     * @param source the audio source
     * @return a runnable that triggers the dialog
     */
    public Runnable startDialog(PCMWebSocketConnection webSocket, AudioSink sink, AudioSource source,
            @Nullable String locationItem, @Nullable String listeningItem) {
        var ks = new WebSocketKeywordSpotter(webSocket::disconnect);
        voiceManager.startDialog( //
                voiceManager.getDialogContextBuilder() //
                        .withSource(source) //
                        .withSink(sink) //
                        .withKS(ks) //
                        .withLocationItem(locationItem) //
                        .withListeningItem(listeningItem) //
                        .build() //
        );
        return ks::trigger;
    }

    /**
     * Anonymous keyword spotter used to trigger the dialog
     */
    private static class WebSocketKeywordSpotter implements KSEdgeService {
        private final Runnable onAbort;
        private @Nullable KSListener ksListener = null;

        public WebSocketKeywordSpotter(Runnable onAbort) {
            this.onAbort = onAbort;
        }

        public void trigger() {
            var ksListener = this.ksListener;
            if (ksListener != null) {
                ksListener.ksEventReceived(new KSpottedEvent());
            }
        }

        @Override
        public KSServiceHandle spot(KSListener ksListener) throws KSException {
            this.ksListener = ksListener;
            return () -> {
                if (ksListener.equals(this.ksListener)) {
                    this.ksListener = null;
                    this.onAbort.run();
                }
            };
        }

        @Override
        public String getId() {
            return "pcmws::anonymous::ks";
        }

        @Override
        public String getLabel(@Nullable Locale locale) {
            // never shown
            return "Anonymous";
        }
    }
}
