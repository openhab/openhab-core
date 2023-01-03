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
import org.openhab.core.voice.KSErrorEvent;
import org.openhab.core.voice.KSException;
import org.openhab.core.voice.KSListener;
import org.openhab.core.voice.KSService;
import org.openhab.core.voice.KSServiceHandle;
import org.openhab.core.voice.KSpottedEvent;

/**
 * A {@link KSService} stub used for the tests.
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated from groovy to java
 */
@NonNullByDefault
public class KSServiceStub implements KSService {

    private static final Set<AudioFormat> SUPPORTED_FORMATS = Set.of(AudioFormat.MP3, AudioFormat.WAV);
    private static final Set<Locale> SUPPORTED_LOCALES = Set.of(Locale.ENGLISH);

    private static final String KSSERVICE_STUB_ID = "ksServiceStubID";
    private static final String KSSERVICE_STUB_LABEL = "ksServiceStubLabel";

    private static final String EXCEPTION_MESSAGE = "keyword spotting exception";
    private static final String ERROR_MESSAGE = "keyword spotting error";

    private boolean exceptionExpected;
    private boolean errorExpected;
    private boolean isWordSpotted;
    private boolean aborted;

    @Override
    public String getId() {
        return KSSERVICE_STUB_ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return KSSERVICE_STUB_LABEL;
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
    public KSServiceHandle spot(KSListener ksListener, AudioStream audioStream, Locale locale, String keyword)
            throws KSException {
        if (exceptionExpected) {
            throw new KSException(EXCEPTION_MESSAGE);
        } else {
            if (errorExpected) {
                ksListener.ksEventReceived(new KSErrorEvent(ERROR_MESSAGE));
            } else {
                isWordSpotted = true;
                ksListener.ksEventReceived(new KSpottedEvent());
            }
            return new KSServiceHandle() {
                @Override
                public void abort() {
                    aborted = true;
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

    public boolean isWordSpotted() {
        return isWordSpotted;
    }

    public boolean isAborted() {
        return aborted;
    }

    @Override
    public String toString() {
        return getId();
    }
}
