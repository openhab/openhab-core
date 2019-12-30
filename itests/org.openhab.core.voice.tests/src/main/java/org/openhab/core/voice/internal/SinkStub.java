/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.library.types.PercentType;

/**
 * An {@link AudioSink} stub used for the tests. Since the tests do not cover all the voice's features,
 * some of the methods are not needed. That's why their implementation is left empty.
 *
 * @author Mihaela Memova - Initial contribution
 * @author Velin Yordanov - migrated from groovy to java
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 */
@NonNullByDefault
public class SinkStub implements AudioSink {

    private boolean isStreamProcessed;
    private static final Set<AudioFormat> SUPPORTED_AUDIO_FORMATS = Collections
            .unmodifiableSet(Stream.of(AudioFormat.MP3, AudioFormat.WAV).collect(Collectors.toSet()));
    private static final Set<Class<? extends AudioStream>> SUPPORTED_AUDIO_STREAMS = Collections
            .singleton(AudioStream.class);

    private static final String SINK_STUB_ID = "sinkStubID";
    private static final String SINK_STUB_LABEL = "sinkStubLabel";

    @Override
    public String getId() {
        return SINK_STUB_ID;
    }

    @Override
    public @Nullable String getLabel(@Nullable Locale locale) {
        return SINK_STUB_LABEL;
    }

    public boolean getIsStreamProcessed() {
        return isStreamProcessed;
    }

    @Override
    public void process(@Nullable AudioStream audioStream) throws UnsupportedAudioFormatException {
        isStreamProcessed = true;
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return SUPPORTED_AUDIO_FORMATS;
    }

    @Override
    public PercentType getVolume() throws IOException {
        // this method will no be used in the tests
        return PercentType.ZERO;
    }

    @Override
    public void setVolume(PercentType volume) throws IOException {
        // this method will not be used in the tests
    }

    public boolean isStreamProcessed() {
        return isStreamProcessed;
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public Set<Class<? extends AudioStream>> getSupportedStreams() {
        return SUPPORTED_AUDIO_STREAMS;
    }
}
