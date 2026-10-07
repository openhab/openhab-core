/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.audio.internal.transcode;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.PipedAudioStream;
import org.openhab.core.audio.SizeableAudioStream;
import org.openhab.core.audio.transcode.AudioTranscoder;
import org.openhab.core.audio.transcode.AudioTranscodingException;
import org.osgi.service.component.annotations.Component;

/**
 * Transcoder that converts a raw PCM signed stream into a FLAC stream via Java Sound / flannel SPI.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = AudioTranscoder.class)
@NonNullByDefault
public class PcmToFlacTranscoder implements AudioTranscoder {

    private static final Set<AudioFormat> SOURCES = Set.of(AudioFormat.PCM_SIGNED);
    private static final Set<AudioFormat> TARGETS = Set.of(AudioFormat.FLAC);

    @Override
    public String getId() {
        return "pcm-to-flac";
    }

    @Override
    public Set<AudioFormat> getSupportedSourceFormats() {
        return SOURCES;
    }

    @Override
    public Set<AudioFormat> getSupportedTargetFormats() {
        return TARGETS;
    }

    @Override
    public int getCost() {
        return 15;
    }

    @Override
    public AudioStream transcode(AudioStream source, AudioFormat targetFormat) throws AudioTranscodingException {
        try {
            // Extract and validate required metadata from PCM source
            AudioFormat sourceFormat = source.getFormat();
            Integer bitDepth = sourceFormat.getBitDepth();
            Long frequency = sourceFormat.getFrequency();
            Integer channels = sourceFormat.getChannels();

            if (bitDepth == null || frequency == null || channels == null) {
                throw new AudioTranscodingException(
                        "Source stream is missing required format metadata (bit depth, frequency, channels)");
            }

            // Prepare parameters for Java Sound AudioFormat
            float sampleRate = frequency.floatValue();
            boolean bigEndian = Boolean.TRUE.equals(sourceFormat.isBigEndian());
            int frameSize = (bitDepth / 8) * channels;

            javax.sound.sampled.AudioFormat jSourceFormat = new javax.sound.sampled.AudioFormat(
                    javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, sampleRate, bitDepth, channels, frameSize,
                    sampleRate, bigEndian);

            // Determine the stream length in frames (if known)
            long frameLength = AudioSystem.NOT_SPECIFIED;
            if (source instanceof SizeableAudioStream sizeable) {
                frameLength = sizeable.length() / frameSize;
            }

            // Wrap source AudioStream inside a Java Sound AudioInputStream
            AudioInputStream ais = new AudioInputStream(source, jSourceFormat, frameLength);

            // Setup output openHAB AudioFormat metadata
            AudioFormat flacFormat = new AudioFormat(AudioFormat.CONTAINER_FLAC, AudioFormat.CODEC_FLAC, null, bitDepth,
                    null, frequency, channels);

            // Use openHAB PipedOutputStream to pipe Java Sound transcoding output to the consuming AudioSink
            PipedAudioStream.Group group = PipedAudioStream.newGroup(flacFormat);
            PipedAudioStream outputStream;
            try {
                outputStream = group.getAudioStreamInGroup();
            } catch (IOException e) {
                throw new AudioTranscodingException(
                        "Failed to initialize piped output stream for FLAC transcoding: " + e.getMessage(), e);
            }

            CompletableFuture.runAsync(() -> {
                try (source; ais; group) {
                    AudioFileFormat.Type flacType = new AudioFileFormat.Type("FLAC", "flac");
                    AudioSystem.write(ais, flacType, group);
                } catch (Exception ignored) {
                    // Ignored intentionally. Exceptions here usually mean the consumer closed the stream early.
                }
            });

            return outputStream;
        } catch (AudioTranscodingException ate) {
            closeQuietly(source);
            throw ate;
        } catch (RuntimeException re) {
            closeQuietly(source);
            throw new AudioTranscodingException("Failed to transcode PCM stream: " + re.getMessage(), re);
        }
    }

    /**
     * Safely closes a {@link Closeable} resource without throwing exceptions.
     */
    private static void closeQuietly(@Nullable Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }
}
