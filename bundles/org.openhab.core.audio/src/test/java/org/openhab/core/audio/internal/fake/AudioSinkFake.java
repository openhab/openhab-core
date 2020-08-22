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
package org.openhab.core.audio.internal.fake;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.FixedLengthAudioStream;
import org.openhab.core.audio.URLAudioStream;
import org.openhab.core.audio.UnsupportedAudioFormatException;
import org.openhab.core.audio.UnsupportedAudioStreamException;
import org.openhab.core.library.types.PercentType;

/**
 * An {@link AudioSink} fake used for the tests.
 *
 * @author Petar Valchev - Initial contribution
 * @author Christoph Weitkamp - Added examples for getSupportedFormats() and getSupportedStreams()
 * @author Christoph Weitkamp - Added parameter to adjust the volume
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public class AudioSinkFake implements AudioSink {

    public @Nullable AudioStream audioStream;
    public @Nullable AudioFormat audioFormat;

    public boolean isStreamProcessed;
    public boolean isStreamStopped;
    public @Nullable PercentType volume;
    public boolean isIOExceptionExpected;
    public boolean isUnsupportedAudioFormatExceptionExpected;
    public boolean isUnsupportedAudioStreamExceptionExpected;

    private static final Set<AudioFormat> SUPPORTED_AUDIO_FORMATS = Set.of(AudioFormat.MP3, AudioFormat.WAV);
    private static final Set<Class<? extends AudioStream>> SUPPORTED_AUDIO_STREAMS = Set
            .of(FixedLengthAudioStream.class, URLAudioStream.class);

    @Override
    public String getId() {
        return "testSinkId";
    }

    @Override
    public @Nullable String getLabel(@Nullable Locale locale) {
        return "testSinkLabel";
    }

    @Override
    public void process(@Nullable AudioStream audioStream)
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

    public @Nullable AudioFormat getAudioFormat() {
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
        PercentType localVolume = volume;
        if (localVolume == null || isIOExceptionExpected) {
            throw new IOException();
        }
        return localVolume;
    }

    @Override
    public void setVolume(PercentType volume) throws IOException {
        if (isIOExceptionExpected) {
            throw new IOException();
        }
        this.volume = volume;
    }
}
