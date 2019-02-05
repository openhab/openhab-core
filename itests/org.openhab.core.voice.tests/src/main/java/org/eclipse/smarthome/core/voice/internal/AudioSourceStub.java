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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.smarthome.core.audio.AudioException;
import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioSource;
import org.eclipse.smarthome.core.audio.AudioStream;

/**
 * An {@link AudioSource} stub used for the tests
 *
 * @author Mihaela Memova - inital contribution
 *
 * @author Velin Yordanov - migrated from groovy to java
 */
public class AudioSourceStub implements AudioSource {

    private Set<AudioFormat> supportedFormats = new HashSet<AudioFormat>();

    private static final String AUDIOSOURCE_STUB_ID = "audioSouceID";
    private static final String AUDIOSOURCE_STUB_LABEL = "audioSouceLabel";

    @Override
    public String getId() {
        return AUDIOSOURCE_STUB_ID;
    }

    @Override
    public String getLabel(Locale locale) {
        return AUDIOSOURCE_STUB_LABEL;
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        supportedFormats.add(AudioFormat.MP3);
        supportedFormats.add(AudioFormat.WAV);
        return supportedFormats;
    }

    @Override
    public AudioStream getInputStream(AudioFormat format) throws AudioException {
        return new AudioStream() {

            @Override
            public int read() throws IOException {
                // this method will not be used in the tests
                return 0;
            }

            @Override
            public AudioFormat getFormat() {
                return AudioFormat.MP3;
            }
        };
    }

    @Override
    public String toString() {
        return getId();
    }
}
