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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.utils.AudioFormatMatcher;
import org.openhab.core.voice.dialog.DialogContext;
import org.openhab.core.voice.dialog.trigger.BasicDTService;
import org.openhab.core.voice.dialog.trigger.DTException;
import org.openhab.core.voice.dialog.trigger.DTService;
import org.openhab.core.voice.dialog.trigger.DTServiceHandle;
import org.openhab.core.voice.dialog.trigger.ks.KSException;
import org.openhab.core.voice.dialog.trigger.ks.KSListener;
import org.openhab.core.voice.dialog.trigger.ks.KSService;
import org.openhab.core.voice.stt.STTException;
import org.openhab.core.voice.stt.STTListener;
import org.openhab.core.voice.stt.STTServiceHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles audio format negotiation, opening and closing audio streams, and managing trigger and speech-to-text
 * service handles for a dialog.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class DialogAudioDriver {

    private final Logger logger = LoggerFactory.getLogger(DialogAudioDriver.class);

    private final DialogContext dialogContext;
    private final @Nullable AudioFormat ksFormat;
    private final @Nullable AudioFormat sttFormat;

    private final Object triggerLock = new Object();
    private volatile long triggerEpoch;
    private @Nullable AudioStream streamKS;
    private @Nullable DTServiceHandle dtServiceHandle;

    private final Object recognitionLock = new Object();
    private volatile long recognitionEpoch;
    private @Nullable AudioStream streamSTT;
    private @Nullable STTServiceHandle sttServiceHandle;

    public DialogAudioDriver(DialogContext dialogContext) {
        this.dialogContext = dialogContext;
        this.ksFormat = dialogContext.dt() instanceof KSService ks
                ? AudioFormatMatcher.getBestMatch(dialogContext.source().getSupportedFormats(),
                        ks.getSupportedFormats())
                : null;
        this.sttFormat = AudioFormatMatcher.getBestMatch(dialogContext.source().getSupportedFormats(),
                dialogContext.stt().getSupportedFormats());
    }

    /**
     * Starts listening for the keyword/trigger.
     *
     * @param listener the listener receiving trigger events
     * @return {@link DTServiceHandle} or null if starting failed or was canceled
     * @throws IllegalStateException if the dialog trigger is not configured
     * @throws DTException if the DT service failed to start listening
     */
    public @Nullable DTServiceHandle startTrigger(final KSListener listener) throws IllegalStateException, DTException {
        final DTService dtService = dialogContext.dt();
        final String keyword = dialogContext.keyword();
        if (dtService == null || keyword == null) {
            throw new IllegalStateException("Unable to run persistent dialog: KS service is not configured");
        }

        stopTrigger();
        final long epoch;
        synchronized (triggerLock) {
            epoch = ++triggerEpoch;
        }

        if (dtService instanceof KSService ksService) {
            final AudioFormat fmt = ksFormat;
            if (fmt == null) {
                throw new KSException("No compatible audio format found for KS '" + ksService.getId() + "' and source '"
                        + dialogContext.source().getId() + "'");
            }

            final AudioStream stream;
            try {
                stream = dialogContext.source().getInputStream(fmt);
            } catch (AudioException e) {
                throw new KSException("Error creating the audio stream: " + e.getMessage(), e);
            }

            boolean installed = false;
            synchronized (triggerLock) {
                if (triggerEpoch == epoch) {
                    streamKS = stream;
                    installed = true;
                }
            }
            if (!installed) {
                closeStream(stream, "KS");
                return null;
            }

            final DTServiceHandle handle;
            try {
                handle = ksService.spot(listener, stream, dialogContext.locale(), keyword);
            } catch (KSException e) {
                detachAndCloseStreamKS(stream);
                throw e;
            }

            synchronized (triggerLock) {
                if (triggerEpoch == epoch) {
                    dtServiceHandle = handle;
                    return handle;
                }
            }

            handle.abort();
            detachAndCloseStreamKS(stream);
            return null;
        } else if (dtService instanceof BasicDTService basicDTService) {
            DTServiceHandle handle = basicDTService.registerListener(listener);
            synchronized (triggerLock) {
                if (triggerEpoch == epoch) {
                    dtServiceHandle = handle;
                    return handle;
                }
            }

            handle.abort();
            return null;
        }
        throw new DTException("DialogAudioDriver is not able to handle this DTService implementation '"
                + dtService.getClass().getName() + "'");
    }

    /**
     * Starts speech recognition stream.
     *
     * @param listener the listener receiving speech-to-text events
     * @return {@link STTServiceHandle} or null if starting failed or was cancelled
     * @throws STTException if the STT service failed to start recognition
     */
    public @Nullable STTServiceHandle startRecognition(final STTListener listener) throws STTException {
        stopRecognition();
        final long epoch;
        synchronized (recognitionLock) {
            epoch = ++recognitionEpoch;
        }

        final AudioFormat fmt = sttFormat;
        if (fmt == null) {
            throw new STTException("No compatible audio format found for STT '" + dialogContext.stt().getId()
                    + "' and source '" + dialogContext.source().getId() + "'");
        }

        final AudioStream stream;
        try {
            stream = dialogContext.source().getInputStream(fmt);
        } catch (AudioException e) {
            throw new STTException("Error creating the audio stream: " + e.getMessage(), e);
        }

        boolean installed = false;
        synchronized (recognitionLock) {
            if (recognitionEpoch == epoch) {
                streamSTT = stream;
                installed = true;
            }
        }
        if (!installed) {
            closeStream(stream, "STT");
            return null;
        }

        final STTServiceHandle handle;
        try {
            handle = dialogContext.stt().recognize(listener, stream, dialogContext.locale());
        } catch (STTException e) {
            detachAndCloseStreamSTT(stream);
            throw e;
        }

        synchronized (recognitionLock) {
            if (recognitionEpoch == epoch) {
                sttServiceHandle = handle;
                return handle;
            }
        }

        handle.abort();
        detachAndCloseStreamSTT(stream);
        return null;
    }

    /**
     * Aborts keyword spotting and closes the keyword spotting audio stream.
     */
    public void stopTrigger() {
        final DTServiceHandle handle;
        final AudioStream stream;
        synchronized (triggerLock) {
            triggerEpoch++;
            handle = dtServiceHandle;
            dtServiceHandle = null;
            stream = streamKS;
            streamKS = null;
        }
        if (handle != null) {
            handle.abort();
        }
        closeStream(stream, "KS");
    }

    /**
     * Aborts speech recognition and closes the speech recognition audio stream.
     */
    public void stopRecognition() {
        final STTServiceHandle handle;
        final AudioStream stream;
        synchronized (recognitionLock) {
            recognitionEpoch++;
            handle = sttServiceHandle;
            sttServiceHandle = null;
            stream = streamSTT;
            streamSTT = null;
        }
        if (handle != null) {
            handle.abort();
        }
        closeStream(stream, "STT");
    }

    /**
     * Stops both trigger and recognition, closing all streams and aborting all handles.
     */
    public void stopAll() {
        stopRecognition();
        stopTrigger();
    }

    private void detachAndCloseStreamKS(AudioStream stream) {
        boolean shouldClose = false;
        synchronized (triggerLock) {
            if (stream.equals(streamKS)) {
                streamKS = null;
                shouldClose = true;
            }
        }
        if (shouldClose) {
            closeStream(stream, "KS");
        }
    }

    private void detachAndCloseStreamSTT(AudioStream stream) {
        boolean shouldClose = false;
        synchronized (recognitionLock) {
            if (stream.equals(streamKS)) {
                streamSTT = null;
                shouldClose = true;
            }
        }
        if (shouldClose) {
            closeStream(stream, "STT");
        }
    }

    private void closeStream(@Nullable AudioStream stream, String type) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                logger.debug("IOException closing {} audio stream: {}", type, e.getMessage(), e);
            }
        }
    }
}
