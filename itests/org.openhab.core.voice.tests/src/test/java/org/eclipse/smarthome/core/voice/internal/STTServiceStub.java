/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.voice.internal;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.voice.STTException;
import org.eclipse.smarthome.core.voice.STTListener;
import org.eclipse.smarthome.core.voice.STTService;
import org.eclipse.smarthome.core.voice.STTServiceHandle;

/**
 * A {@link STTService} stub used for the tests.
 *
 * @author Mihaela Memova - initial contribution
 *
 * @author Velin Yordanov - migrated from groovy to java
 *
 */
public class STTServiceStub implements STTService {

    private Set<AudioFormat> supportedFormats;

    private final String STTSERVICE_STUB_ID = "sttServiceStubID";
    private final String STTSERVICE_STUB_LABEL = "sttServiceStubLabel";

    @Override
    public String getId() {
        return STTSERVICE_STUB_ID;
    }

    @Override
    public String getLabel(Locale locale) {
        return STTSERVICE_STUB_LABEL;
    }

    @Override
    public Set<Locale> getSupportedLocales() {
        return null;
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        supportedFormats = new HashSet<AudioFormat>();
        supportedFormats.add(AudioFormat.MP3);
        supportedFormats.add(AudioFormat.WAV);
        return supportedFormats;
    }

    @Override
    public STTServiceHandle recognize(STTListener sttListener, AudioStream audioStream, Locale locale,
            Set<String> grammars) throws STTException {
        return new STTServiceHandle() {
            // this method will not be used in the tests
            @Override
            public void abort() {
            }
        };
    }

    @Override
    public String toString() {
        return getId();
    }
}
