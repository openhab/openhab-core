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

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioException;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.audio.AudioStream;

/**
 * An {@link AudioSource} stub used for the tests
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - Migrated from groovy to java
 */
@NonNullByDefault
public class AudioSourceStub implements AudioSource {

    private static final Set<AudioFormat> SUPPORTED_FORMATS = Set.of(AudioFormat.MP3, AudioFormat.WAV);

    private static final String AUDIOSOURCE_STUB_ID = "audioSouceID";
    private static final String AUDIOSOURCE_STUB_LABEL = "audioSouceLabel";

    @Override
    public String getId() {
        return AUDIOSOURCE_STUB_ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return AUDIOSOURCE_STUB_LABEL;
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return SUPPORTED_FORMATS;
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
