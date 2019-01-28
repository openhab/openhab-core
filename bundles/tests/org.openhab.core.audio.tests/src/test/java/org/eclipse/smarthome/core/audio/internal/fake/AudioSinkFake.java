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
package org.eclipse.smarthome.core.audio.internal.fake;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioSink;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.audio.FixedLengthAudioStream;
import org.eclipse.smarthome.core.audio.URLAudioStream;
import org.eclipse.smarthome.core.audio.UnsupportedAudioFormatException;
import org.eclipse.smarthome.core.audio.UnsupportedAudioStreamException;
import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * An {@link AudioSink} fake used for the tests.
 *
 * @author Christoph Weitkamp - Added examples for getSupportedFormats() and getSupportedStreams()
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class AudioSinkFake implements AudioSink {

    public AudioStream audioStream;
    public AudioFormat audioFormat;

    public boolean isStreamProcessed;
    public boolean isStreamStopped;
    public PercentType volume;
    public boolean isIOExceptionExpected;
    public boolean isUnsupportedAudioFormatExceptionExpected;
    public boolean isUnsupportedAudioStreamExceptionExpected;

    private static final Set<AudioFormat> SUPPORTED_AUDIO_FORMATS = Collections
            .unmodifiableSet(Stream.of(AudioFormat.MP3, AudioFormat.WAV).collect(Collectors.toSet()));
    private static final Set<Class<? extends AudioStream>> SUPPORTED_AUDIO_STREAMS = Collections
            .unmodifiableSet(Stream.of(FixedLengthAudioStream.class, URLAudioStream.class).collect(Collectors.toSet()));

    @Override
    public String getId() {
        return "testSinkId";
    }

    @Override
    public String getLabel(Locale locale) {
        return "testSinkLabel";
    }

    @Override
    public void process(AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException {
        if (isUnsupportedAudioFormatExceptionExpected) {
            throw new UnsupportedAudioFormatException("Expected audio format exception", null);
        }
        if (isUnsupportedAudioStreamExceptionExpected) {
            throw new UnsupportedAudioStreamException("Expected audio stream exception", null);
        }
        this.audioStream = audioStream;
        if (audioStream != null) {
            audioFormat = audioStream.getFormat();
        } else {
            isStreamStopped = true;
        }
        isStreamProcessed = true;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return SUPPORTED_AUDIO_FORMATS;
    }

    @Override
    public Set<Class<? extends AudioStream>> getSupportedStreams() {
        return SUPPORTED_AUDIO_STREAMS;
    }

    @Override
    public PercentType getVolume() throws IOException {
        if (isIOExceptionExpected) {
            throw new IOException();
        }
        return volume;
    }

    @Override
    public void setVolume(PercentType volume) throws IOException {
        if (isIOExceptionExpected) {
            throw new IOException();
        }
        this.volume = volume;
    }
}
