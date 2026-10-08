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

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.transcode.AudioTranscoder;
import org.openhab.core.audio.transcode.AudioTranscodingException;
import org.osgi.service.component.annotations.Component;

/**
 * {@link AudioTranscoder} that decodes a FLAC stream to a raw PCM signed stream via Java Sound / flannel SPI.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = AudioTranscoder.class)
@NonNullByDefault
public class FlacToPcmDecoder implements AudioTranscoder {

    private static final Set<AudioFormat> SOURCES = Set.of(AudioFormat.FLAC);
    private static final Set<AudioFormat> TARGETS = Set.of(AudioFormat.PCM_SIGNED);

    @Override
    public String getId() {
        return "flac-to-pcm";
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
    public boolean canTranscode(AudioFormat sourceFormat, AudioFormat targetFormat) {
        if (!AudioTranscoder.super.canTranscode(sourceFormat, targetFormat)) {
            return false;
        }
        if (sourceFormat.getFrequency() != null && targetFormat.getFrequency() != null
                && !sourceFormat.getFrequency().equals(targetFormat.getFrequency())) {
            return false;
        }
        if (sourceFormat.getChannels() != null && targetFormat.getChannels() != null
                && !sourceFormat.getChannels().equals(targetFormat.getChannels())) {
            return false;
        }
        return true;
    }

    @Override
    public AudioStream transcode(AudioStream source, AudioFormat targetFormat) throws AudioTranscodingException {
        if (!canTranscode(source.getFormat(), targetFormat)) {
            closeQuietly(source);
            throw new AudioTranscodingException(
                    "Transcoding from " + source.getFormat() + " to " + targetFormat + " is not supported");
        }

        // AudioSystem requires mark() and reset() support to probe the stream for format headers.
        // Wrapping the source in a BufferedInputStream ensures these operations are supported.
        BufferedInputStream bis = new BufferedInputStream(source);
        AudioInputStream sourceAis = null;

        try {
            // Parse the FLAC headers and wrap the stream in a Java Sound AudioInputStream
            try {
                sourceAis = AudioSystem.getAudioInputStream(bis);
            } catch (UnsupportedAudioFileException | IOException e) {
                closeQuietly(bis);
                throw new AudioTranscodingException("Failed to read FLAC audio stream: " + e.getMessage(), e);
            }

            // Extract and validate required metadata from FLAC source
            javax.sound.sampled.AudioFormat sourceFormat = sourceAis.getFormat();
            float sampleRate = sourceFormat.getSampleRate();
            int channels = sourceFormat.getChannels();
            if (sampleRate <= 0 || channels <= 0) {
                throw new AudioTranscodingException("FLAC stream has unspecified sample rate or channel count");
            }

            // Calculate the required bit depth and frame size for the uncompressed PCM stream
            int sourceBitDepth = sourceFormat.getSampleSizeInBits() > 0 ? sourceFormat.getSampleSizeInBits() : 16;
            Integer requestedBitDepth = targetFormat.getBitDepth();
            // PCM requires byte alignment, so round up to the next byte
            int bytesPerSample = ((requestedBitDepth != null ? requestedBitDepth : sourceBitDepth) + 7) / 8;
            int bitDepth = bytesPerSample * 8;
            int frameSize = channels * bytesPerSample;

            // Prepare the output Java Sound AudioFormat for the decoded PCM data
            javax.sound.sampled.AudioFormat jOutputFormat = new javax.sound.sampled.AudioFormat(
                    javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, sampleRate, bitDepth, channels, frameSize,
                    sampleRate, false);

            // Request Java Sound to decode the FLAC stream into PCM
            AudioInputStream pcmAis;
            try {
                pcmAis = AudioSystem.getAudioInputStream(jOutputFormat, sourceAis);
            } catch (IllegalArgumentException e) {
                throw new AudioTranscodingException("Failed to decode FLAC stream to PCM: " + e.getMessage(), e);
            }

            // Setup output openHAB AudioFormat metadata
            AudioFormat outputFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED, false,
                    bitDepth, Math.round(sampleRate * bitDepth * channels), (long) sampleRate, channels);

            long frameLength = pcmAis.getFrameLength();
            if (frameLength >= 0) {
                return new SizeableTranscodedAudioStream(outputFormat, source.getId(), pcmAis, frameLength * frameSize);
            }

            // Wrap decoded stream in TranscodedAudioStream to map it back to the openHAB AudioStream
            return new TranscodedAudioStream(outputFormat, source.getId(), pcmAis);
        } catch (AudioTranscodingException ate) {
            closeQuietly(bis);
            closeQuietly(sourceAis);
            throw ate;
        } catch (RuntimeException re) {
            closeQuietly(bis);
            closeQuietly(sourceAis);
            throw new AudioTranscodingException("Failed to transcode FLAC stream: " + re.getMessage(), re);
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
