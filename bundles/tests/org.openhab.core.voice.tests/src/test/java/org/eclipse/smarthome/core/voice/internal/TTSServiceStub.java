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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.voice.TTSException;
import org.eclipse.smarthome.core.voice.TTSService;
import org.eclipse.smarthome.core.voice.Voice;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * A {@link TTSService} stub used for the tests.
 *
 * @author Mihaela Memova - initial contribution
 *
 * @author Velin Yordanov - migrated from groovy to java
 *
 */
public class TTSServiceStub implements TTSService {

    private BundleContext context;

    private Set<Voice> availableVoices;
    private Set<AudioFormat> supportedFormats;

    private static final String TTS_SERVICE_STUB_ID = "ttsServiceStubID";
    private static final String TTS_SERVICE_STUB_LABEL = "ttsServiceStubLabel";

    public TTSServiceStub(BundleContext context) {
        this.context = context;
    }

    public TTSServiceStub() {

    }

    @Override
    public String getId() {
        return TTS_SERVICE_STUB_ID;
    }

    @Override
    public String getLabel(Locale locale) {
        return TTS_SERVICE_STUB_LABEL;
    }

    @Override
    public Set<Voice> getAvailableVoices() {
        availableVoices = new HashSet<Voice>();
        Collection<ServiceReference<Voice>> refs;
        try {
            refs = context.getServiceReferences(Voice.class, null);
            if (refs != null) {
                for (ServiceReference<Voice> ref : refs) {
                    Voice service = context.getService(ref);
                    if (service.getUID().startsWith(getId())) {
                        availableVoices.add(service);
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            // If the specified filter contains an invalid filter expression that cannot be parsed.
            return null;
        }
        return availableVoices;
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        supportedFormats = new HashSet<AudioFormat>();
        supportedFormats.add(AudioFormat.MP3);
        supportedFormats.add(AudioFormat.WAV);
        return supportedFormats;
    }

    @Override
    public AudioStream synthesize(String text, Voice voice, final AudioFormat requestedFormat) throws TTSException {
        return new AudioStream() {

            @Override
            public int read() throws IOException {
                // this method will not be used in the tests
                return 0;
            }

            @Override
            public AudioFormat getFormat() {
                return requestedFormat;
            }
        };
    }

    @Override
    public String toString() {
        return getId();
    }
}
