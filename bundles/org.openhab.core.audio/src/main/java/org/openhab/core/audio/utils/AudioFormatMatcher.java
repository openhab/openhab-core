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
package org.openhab.core.audio.utils;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.transcode.AudioTranscoder;

/**
 * Utility class for audio format negotiation and matching between audio sources, sinks, and voice services.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public final class AudioFormatMatcher {
    private static final int DEFAULT_BIT_DEPTH = 16;
    private static final long DEFAULT_FREQUENCY = 16000;

    private AudioFormatMatcher() {
        // Utility class
    }

    /**
     * Determines a preferred concrete audio format from a set of supported formats.
     * Prefers PCM (uncontainerized signed PCM) over WAVE container formats and fills default attributes if abstract.
     *
     * @param audioFormats set of candidate audio formats
     * @return a preferred concrete audio format, or null if none could be determined
     */
    public static @Nullable AudioFormat getPreferredFormat(Set<AudioFormat> audioFormats) {
        // Prefer concrete PCM format
        for (AudioFormat currentAudioFormat : audioFormats) {
            if (isConcrete(currentAudioFormat) && isPcmFormat(currentAudioFormat)) {
                return currentAudioFormat;
            }
        }

        // Fall back to concrete WAVE container format
        for (AudioFormat currentAudioFormat : audioFormats) {
            if (isConcrete(currentAudioFormat) && isWaveFormat(currentAudioFormat)) {
                return currentAudioFormat;
            }
        }

        // Check for any other concrete format
        for (AudioFormat currentAudioFormat : audioFormats) {
            if (isConcrete(currentAudioFormat)) {
                return currentAudioFormat;
            }
        }

        // Prefer abstract PCM format and make it concrete
        for (AudioFormat currentAudioFormat : audioFormats) {
            if (isPcmFormat(currentAudioFormat) || (AudioFormat.CONTAINER_NONE.equals(currentAudioFormat.getContainer())
                    && currentAudioFormat.getCodec() == null)) {
                return completeFormat(currentAudioFormat, AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED);
            }
        }

        // Fall back to abstract WAVE format and make it concrete
        for (AudioFormat currentAudioFormat : audioFormats) {
            if (isWaveFormat(currentAudioFormat)) {
                return completeFormat(currentAudioFormat, AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED);
            }
        }

        return null;
    }

    private static boolean isConcrete(AudioFormat format) {
        return format.getCodec() != null && format.getContainer() != null && format.isBigEndian() != null
                && format.getBitDepth() != null && format.getBitRate() != null && format.getFrequency() != null
                && format.getChannels() != null;
    }

    public static boolean isPcmFormat(AudioFormat format) {
        String container = format.getContainer();
        String codec = format.getCodec();
        return AudioFormat.CONTAINER_NONE.equals(container) && (codec == null || AudioFormat.isPcmCodec(codec));
    }

    public static boolean isWaveFormat(AudioFormat format) {
        String container = format.getContainer();
        String codec = format.getCodec();
        return AudioFormat.CONTAINER_WAVE.equals(container) && (codec == null || AudioFormat.isPcmCodec(codec));
    }

    private static AudioFormat completeFormat(AudioFormat format, String defaultContainer, String defaultCodec) {
        String container = format.getContainer() != null ? format.getContainer() : defaultContainer;
        String codec = format.getCodec() != null ? format.getCodec() : defaultCodec;
        Boolean bigEndian = format.isBigEndian() != null ? format.isBigEndian() : Boolean.TRUE;

        Integer bitRate = format.getBitRate();
        Long frequency = format.getFrequency();
        Integer bitDepth = format.getBitDepth();

        // These values must be interdependent (bitRate = bitDepth * frequency)
        if (null == bitRate) {
            if (null == bitDepth) {
                bitDepth = DEFAULT_BIT_DEPTH;
            }
            if (null == frequency) {
                frequency = DEFAULT_FREQUENCY;
            }
            bitRate = bitDepth * frequency.intValue();
        } else if (null == bitDepth) {
            if (null == frequency) {
                frequency = DEFAULT_FREQUENCY;
            }
            bitDepth = bitRate / frequency.intValue();
        } else if (null == frequency) {
            frequency = bitRate.longValue() / bitDepth.longValue();
        }

        Integer channels = format.getChannels() != null ? format.getChannels() : 1;

        return new AudioFormat(container, codec, bigEndian, bitDepth, bitRate, frequency, channels);
    }

    /**
     * Determines the best matching audio format between inputs and outputs without transcoding.
     *
     * @param inputs candidate input formats
     * @param outputs candidate output formats
     * @return the best matching audio format, or null if incompatible
     */
    public static @Nullable AudioFormat getBestMatch(Set<AudioFormat> inputs, Set<AudioFormat> outputs) {
        AudioFormat preferredFormat = getPreferredFormat(inputs);
        if (preferredFormat != null) {
            for (AudioFormat output : outputs) {
                if (output.isCompatible(preferredFormat)) {
                    return preferredFormat;
                }
            }
        }

        for (AudioFormat output : outputs) {
            for (AudioFormat input : inputs) {
                if (output.isCompatible(input)) {
                    return input;
                }
            }
        }

        return null;
    }

    /**
     * Determines the best matching audio format between source formats and target formats,
     * taking transcoding capabilities into account.
     *
     * @param sourceSupportedFormats candidate source formats
     * @param targetSupportedFormats candidate target formats
     * @return a format suitable for the source (and transcoding if needed), or null if none
     */
    public static @Nullable AudioFormat getBestMatchWithTranscoding(Set<AudioFormat> sourceSupportedFormats,
            Set<AudioFormat> targetSupportedFormats) {
        AudioFormat bestMatch = getBestMatch(sourceSupportedFormats, targetSupportedFormats);
        if (bestMatch != null) {
            return bestMatch;
        }

        AudioFormat preferredFormat = getPreferredFormat(sourceSupportedFormats);
        if (preferredFormat != null && targetSupportedFormats.stream()
                .anyMatch(targetFormat -> AudioTranscoder.canTranscode(preferredFormat, targetFormat))) {
            return preferredFormat;
        }

        for (AudioFormat sourceFormat : sourceSupportedFormats) {
            if (targetSupportedFormats.stream()
                    .anyMatch(targetFormat -> AudioTranscoder.canTranscode(sourceFormat, targetFormat))) {
                return sourceFormat;
            }
        }

        return null;
    }
}
