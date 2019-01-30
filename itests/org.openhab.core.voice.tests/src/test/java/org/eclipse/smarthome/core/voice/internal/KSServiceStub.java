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
import org.eclipse.smarthome.core.voice.KSException;
import org.eclipse.smarthome.core.voice.KSListener;
import org.eclipse.smarthome.core.voice.KSService;
import org.eclipse.smarthome.core.voice.KSServiceHandle;

/**
 * A {@link KSService} stub used for the tests.
 *
 * @author Mihaela Memova - initial contribution
 *
 * @author Velin Yordanov - migrated from groovy to java
 *
 */
public class KSServiceStub implements KSService {

    private Set<AudioFormat> supportedFormats = new HashSet<AudioFormat>();

    private boolean isWordSpotted;
    private boolean isKSExceptionExpected;

    private static final String KSSERVICE_STUB_ID = "ksServiceStubID";
    private static final String KSSERVICE_STUB_LABEL = "ksServiceStubLabel";

    @Override
    public String getId() {
        return KSSERVICE_STUB_ID;
    }

    public void setIsKsExceptionExpected(boolean value) {
        this.isKSExceptionExpected = value;
    }

    @Override
    public String getLabel(Locale locale) {
        return KSSERVICE_STUB_LABEL;
    }

    @Override
    public Set<Locale> getSupportedLocales() {
        return null;
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        supportedFormats.add(AudioFormat.MP3);
        supportedFormats.add(AudioFormat.WAV);
        return supportedFormats;
    }

    @Override
    public KSServiceHandle spot(KSListener ksListener, AudioStream audioStream, Locale locale, String keyword)
            throws KSException {

        if (isKSExceptionExpected) {
            throw new KSException("Expected KSException");
        } else {
            isWordSpotted = true;
            return new KSServiceHandle() {
                @Override
                public void abort() {
                }
            };
        }
    }

    public boolean isWordSpotted() {
        return isWordSpotted;
    }

    @Override
    public String toString() {
        return getId();
    }
}
