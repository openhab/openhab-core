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

    private @Nullable AudioStream streamKS;
    private @Nullable AudioStream streamSTT;
    private @Nullable DTServiceHandle dtServiceHandle;
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
     * @return {@link DTServiceHandle} or null if starting failed
     * @throws IllegalStateException if the dialog trigger is not configured
     * @throws DTException if the DT service failed to start listening
     */
    public DTServiceHandle startTrigger(KSListener listener) throws IllegalStateException, DTException {
        DTService dtService = dialogContext.dt();
        String keyword = dialogContext.keyword();
        if (dtService == null || keyword == null) {
            throw new IllegalStateException("Unable to run persistent dialog: KS service is not configured");
        }

        stopTrigger();

        AudioFormat fmt = null;
        if (dtService instanceof KSService ksService) {
            fmt = ksFormat;
            if (fmt == null) {
                throw new KSException("No compatible audio format found for KS '" + ksService.getId() + "' and source '"
                        + dialogContext.source().getId() + "'");
            }
        }

        try {
            if (dtService instanceof KSService ksService) {
                AudioStream stream = dialogContext.source().getInputStream(fmt);
                streamKS = stream;
                return dtServiceHandle = ksService.spot(listener, stream, dialogContext.locale(), keyword);
            } else if (dtService instanceof BasicDTService basicDTService) {
                return dtServiceHandle = basicDTService.registerListener(listener);
            }
            throw new DTException("DialogAudioDriver is not able to handle this DTService implementation '"
                    + dtService.getClass().getName() + "'");
        } catch (AudioException e) {
            throw new KSException("Error creating the audio stream: " + e.getMessage(), e);
        } catch (KSException e) {
            closeStreamKS();
            throw e;
        }
    }

    /**
     * Starts speech recognition stream.
     *
     * @param listener the listener receiving speech-to-text events
     * @return {@link STTServiceHandle} or null if starting failed
     * @throws STTException if the STT service failed to start recognition
     */
    public STTServiceHandle startRecognition(STTListener listener) throws STTException {
        stopRecognition();
        AudioFormat fmt = sttFormat;
        if (fmt == null) {
            throw new STTException("No compatible audio format found for STT '" + dialogContext.stt().getId()
                    + "' and source '" + dialogContext.source().getId() + "'");
        }
        try {
            AudioStream stream = dialogContext.source().getInputStream(fmt);
            streamSTT = stream;
            return sttServiceHandle = dialogContext.stt().recognize(listener, stream, dialogContext.locale());
        } catch (AudioException e) {
            throw new STTException("Error creating the audio stream: " + e.getMessage(), e);
        } catch (STTException e) {
            closeStreamSTT();
            throw e;
        }
    }

    /**
     * Aborts keyword spotting and closes the keyword spotting audio stream.
     */
    public void stopTrigger() {
        DTServiceHandle handle = dtServiceHandle;
        if (handle != null) {
            handle.abort();
            dtServiceHandle = null;
        }
        closeStreamKS();
    }

    /**
     * Aborts speech recognition and closes the speech recognition audio stream.
     */
    public void stopRecognition() {
        STTServiceHandle handle = sttServiceHandle;
        if (handle != null) {
            handle.abort();
            sttServiceHandle = null;
        }
        closeStreamSTT();
    }

    /**
     * Stops both trigger and recognition, closing all streams and aborting all handles.
     */
    public void stopAll() {
        stopRecognition();
        stopTrigger();
    }

    private void closeStreamKS() {
        AudioStream stream = streamKS;
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                logger.debug("IOException closing KS audio stream: {}", e.getMessage(), e);
            }
            streamKS = null;
        }
    }

    private void closeStreamSTT() {
        AudioStream stream = streamSTT;
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                logger.debug("IOException closing STT audio stream: {}", e.getMessage(), e);
            }
            streamSTT = null;
        }
    }
}
