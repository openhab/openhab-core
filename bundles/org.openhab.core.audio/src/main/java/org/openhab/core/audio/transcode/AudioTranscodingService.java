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
package org.openhab.core.audio.transcode;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioStream;

/**
 * Service for transcoding {@link AudioStream}s from one format to another, supporting direct or multistep
 * transcoding paths.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface AudioTranscodingService {

    /**
     * Checks whether a transcoding route (either direct or chained) exists between the two formats.
     *
     * @param sourceFormat the input format
     * @param targetFormat the desired target format
     * @return true if transcoding is possible, false otherwise
     */
    boolean canTranscode(AudioFormat sourceFormat, AudioFormat targetFormat);

    /**
     * Transcodes an audio stream to the requested target format.
     *
     * @param source the audio stream to transcode
     * @param targetFormat the target format
     * @return a transcoded audio stream
     * @throws AudioTranscodingException if no route exists or transcoding fails
     */
    AudioStream transcode(AudioStream source, AudioFormat targetFormat) throws AudioTranscodingException;

    /**
     * Transcodes the audio stream to a format supported by <code>candidateFormats</code> (e.g., from an
     * {@link AudioSink}).
     * If the source stream's format is already compatible with one of <code>candidateFormats</code>, the stream is
     * returned
     * as-is without transcoding.
     *
     * @param source the audio stream to transcode
     * @param candidateFormats the acceptable target formats
     * @return an audio stream compatible with one of the candidateFormats
     * @throws AudioTranscodingException if no route to any candidate format exists or transcoding fails
     */
    AudioStream transcodeToSupported(AudioStream source, Set<AudioFormat> candidateFormats)
            throws AudioTranscodingException;
}
