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

import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.voice.STTException;
import org.openhab.core.voice.STTListener;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.STTServiceHandle;
import org.openhab.core.voice.SpeechRecognitionErrorEvent;
import org.openhab.core.voice.SpeechRecognitionEvent;

/**
 * A {@link STTService} stub used for the tests.
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated from groovy to java
 */
@NonNullByDefault
public class STTServiceStub implements STTService {

    private static final Set<AudioFormat> SUPPORTED_FORMATS = Set.of(AudioFormat.MP3, AudioFormat.WAV);
    private static final Set<Locale> SUPPORTED_LOCALES = Set.of(Locale.ENGLISH);

    private static final String STTSERVICE_STUB_ID = "sttServiceStubID";
    private static final String STTSERVICE_STUB_LABEL = "sttServiceStubLabel";

    private static final String RECOGNIZED_TEXT = "Recognized text";
    private static final String EXCEPTION_MESSAGE = "STT exception";
    private static final String ERROR_MESSAGE = "STT error";

    private boolean exceptionExpected;
    private boolean errorExpected;
    private boolean recognized;

    @Override
    public String getId() {
        return STTSERVICE_STUB_ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return STTSERVICE_STUB_LABEL;
    }

    @Override
    public Set<Locale> getSupportedLocales() {
        return SUPPORTED_LOCALES;
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }

    @Override
    public STTServiceHandle recognize(STTListener sttListener, AudioStream audioStream, Locale locale,
            Set<String> grammars) throws STTException {
        if (exceptionExpected) {
            throw new STTException(EXCEPTION_MESSAGE);
        } else {
            if (errorExpected) {
                sttListener.sttEventReceived(new SpeechRecognitionErrorEvent(ERROR_MESSAGE));
            } else {
                recognized = true;
                sttListener.sttEventReceived(new SpeechRecognitionEvent(RECOGNIZED_TEXT, 0.75f));
            }
            return new STTServiceHandle() {
                // this method will not be used in the tests
                @Override
                public void abort() {
                }
            };
        }
    }

    public void setExceptionExpected(boolean exceptionExpected) {
        this.exceptionExpected = exceptionExpected;
    }

    public void setErrorExpected(boolean errorExpected) {
        this.errorExpected = errorExpected;
    }

    public boolean isRecognized() {
        return recognized;
    }

    @Override
    public String toString() {
        return getId();
    }
}
