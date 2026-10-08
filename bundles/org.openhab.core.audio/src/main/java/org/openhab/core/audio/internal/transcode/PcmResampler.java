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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.SizeableAudioStream;
import org.openhab.core.audio.transcode.AudioTranscoder;
import org.openhab.core.audio.transcode.AudioTranscodingException;
import org.osgi.service.component.annotations.Component;

/**
 * {@link AudioTranscoder} that supports resampling raw signed PCM stream via Java Sound,
 * allowing to change the sample rate/frequency, bit-depth, or endianness.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = AudioTranscoder.class)
@NonNullByDefault
public class PcmResampler implements AudioTranscoder {
    public static String ID = "pcm-resample";

    private static final Set<AudioFormat> FORMATS = Set.of(AudioFormat.PCM_SIGNED);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<AudioFormat> getSupportedSourceFormats() {
        return FORMATS;
    }

    @Override
    public Set<AudioFormat> getSupportedTargetFormats() {
        return FORMATS;
    }

    @Override
    public int getCost() {
        return 10;
    }

    @Override
    public boolean canTranscode(AudioFormat sourceFormat, AudioFormat targetFormat) {
        if (!AudioTranscoder.super.canTranscode(sourceFormat, targetFormat)) {
            return false;
        }

        Long targetFreq = targetFormat.getFrequency();
        Long sourceFreq = sourceFormat.getFrequency();
        boolean freqChanges = targetFreq != null && (!targetFreq.equals(sourceFreq));

        Integer targetBitDepth = targetFormat.getBitDepth();
        Integer sourceBitDepth = sourceFormat.getBitDepth();
        boolean bitDepthChanges = targetBitDepth != null && (!targetBitDepth.equals(sourceBitDepth));

        Boolean targetBigEndian = targetFormat.isBigEndian();
        Boolean sourceBigEndian = sourceFormat.isBigEndian();
        boolean endiannessChanges = targetBigEndian != null && (!targetBigEndian.equals(sourceBigEndian));

        if (!freqChanges && !bitDepthChanges && !endiannessChanges) {
            return false;
        }

        if (sourceFormat.getChannels() != null && targetFormat.getChannels() != null
                && !sourceFormat.getChannels().equals(targetFormat.getChannels())) {
            return false;
        }

        if (targetBitDepth != null && targetBitDepth != 8 && targetBitDepth != 16 && targetBitDepth != 24
                && targetBitDepth != 32) {
            return false;
        }

        return true;
    }

    @Override
    public AudioStream transcode(AudioStream source, AudioFormat targetFormat) throws AudioTranscodingException {
        if (targetFormat.isCompatible(source.getFormat())) {
            return source;
        }

        if (!canTranscode(source.getFormat(), targetFormat)) {
            closeQuietly(source);
            throw new AudioTranscodingException(
                    "Resampling from " + source.getFormat() + " to " + targetFormat + " is not supported");
        }

        try {
            AudioFormat sourceFormat = source.getFormat();
            Integer sourceBitDepth = sourceFormat.getBitDepth();
            Long sourceFrequency = sourceFormat.getFrequency();
            Integer sourceChannels = sourceFormat.getChannels();
            Boolean sourceBigEndian = sourceFormat.isBigEndian();

            if (sourceBitDepth == null || sourceFrequency == null || sourceChannels == null) {
                throw new AudioTranscodingException(
                        "Source stream is missing required format metadata (bit depth, frequency, channels)");
            }

            if (targetFormat.getFrequency() == null && targetFormat.getBitDepth() == null
                    && targetFormat.isBigEndian() == null) {
                throw new AudioTranscodingException(
                        "Target format does not specify frequency, bit depth, or endianness to convert to");
            }

            Long targetFrequency = targetFormat.getFrequency();
            Integer targetBitDepth = targetFormat.getBitDepth();
            Boolean targetBigEndian = targetFormat.isBigEndian();

            long outSampleRate = targetFrequency != null ? targetFrequency : sourceFrequency;
            int outBitDepth = targetBitDepth != null ? targetBitDepth : sourceBitDepth;
            boolean outBigEndian = targetBigEndian != null ? targetBigEndian : Boolean.TRUE.equals(sourceBigEndian);

            if (sourceFrequency.equals(targetFrequency) && sourceBitDepth.equals(targetBitDepth)
                    && (sourceBigEndian == null || sourceBigEndian.equals(targetBigEndian))) {
                return source;
            }

            float inSampleRate = sourceFrequency.floatValue();
            int inFrameSize = (sourceBitDepth / 8) * sourceChannels;
            int outFrameSize = (outBitDepth / 8) * sourceChannels;

            javax.sound.sampled.AudioFormat jSourceFormat = new javax.sound.sampled.AudioFormat(
                    javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, inSampleRate, sourceBitDepth, sourceChannels,
                    inFrameSize, inSampleRate, Boolean.TRUE.equals(sourceBigEndian));

            javax.sound.sampled.AudioFormat jTargetFormat = new javax.sound.sampled.AudioFormat(
                    javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, outSampleRate, outBitDepth, sourceChannels,
                    outFrameSize, outSampleRate, outBigEndian);

            long frameLength = AudioSystem.NOT_SPECIFIED;
            if (source instanceof SizeableAudioStream sizeable) {
                frameLength = sizeable.length() / inFrameSize;
            }

            AudioInputStream sourceAis = new AudioInputStream(source, jSourceFormat, frameLength);

            AudioInputStream resampledAis;
            try {
                resampledAis = AudioSystem.getAudioInputStream(jTargetFormat, sourceAis);
            } catch (IllegalArgumentException e) {
                throw new AudioTranscodingException("Failed to resample PCM stream: " + e.getMessage(), e);
            }

            int bitRate = Math.round(outSampleRate * outBitDepth * sourceChannels);
            AudioFormat outputFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED,
                    outBigEndian, outBitDepth, bitRate, outSampleRate, sourceChannels);

            long resampledFrameLength = resampledAis.getFrameLength();
            if (resampledFrameLength >= 0) {
                return new SizeableTranscodedAudioStream(outputFormat, source.getId(), resampledAis,
                        resampledFrameLength * outFrameSize);
            }

            return new TranscodedAudioStream(outputFormat, source.getId(), resampledAis);
        } catch (AudioTranscodingException ate) {
            closeQuietly(source);
            throw ate;
        } catch (RuntimeException re) {
            closeQuietly(source);
            throw new AudioTranscodingException("Failed to resample PCM stream: " + re.getMessage(), re);
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
