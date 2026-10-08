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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.transcode.AudioTranscoder;
import org.openhab.core.audio.transcode.AudioTranscodingException;
import org.openhab.core.audio.utils.AudioWaveUtils;
import org.osgi.service.component.annotations.Component;

/**
 * {@link AudioTranscoder} that decodes a WAV stream to its content stream by removing the WAV container header.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = AudioTranscoder.class)
@NonNullByDefault
public class WavContainerDecoder implements AudioTranscoder {
    private static final AudioFormat NONE_CONTAINER = new AudioFormat(AudioFormat.CONTAINER_NONE, null, null, null,
            null, null);
    private static final AudioFormat WAV_CONTAINER = new AudioFormat(AudioFormat.CONTAINER_WAVE, null, null, null, null,
            null);

    private static final Set<AudioFormat> SOURCES = Set.of(WAV_CONTAINER);
    private static final Set<AudioFormat> TARGETS = Set.of(NONE_CONTAINER);

    @Override
    public String getId() {
        return "wav-to-pcm";
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
        return 1;
    }

    @Override
    public boolean canTranscode(AudioFormat sourceFormat, AudioFormat targetFormat) {
        if (!AudioTranscoder.super.canTranscode(sourceFormat, targetFormat)) {
            return false;
        }
        if (sourceFormat.getCodec() != null && targetFormat.getCodec() != null
                && !sourceFormat.getCodec().equals(targetFormat.getCodec())) {
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
        if (sourceFormat.getBitDepth() != null && targetFormat.getBitDepth() != null
                && !sourceFormat.getBitDepth().equals(targetFormat.getBitDepth())) {
            return false;
        }
        if (targetFormat.isBigEndian() != null) {
            boolean sourceBigEndian = Boolean.TRUE.equals(sourceFormat.isBigEndian());
            return Boolean.TRUE.equals(targetFormat.isBigEndian()) == sourceBigEndian;
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

        AudioFormat sourceFormat = source.getFormat();

        try {
            AudioWaveUtils.removeFMT(source);
        } catch (IOException e) {
            closeQuietly(source);
            throw new AudioTranscodingException("Failed to remove WAV header: " + e.getMessage(), e);
        }

        AudioFormat outputFormat = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED,
                sourceFormat.isBigEndian(), sourceFormat.getBitDepth(), sourceFormat.getBitRate(),
                sourceFormat.getFrequency(), sourceFormat.getChannels());

        return new TranscodedAudioStream(outputFormat, source.getId(), source);
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
