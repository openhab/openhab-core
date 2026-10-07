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
import org.openhab.core.audio.AudioStream;

/**
 * This is the interface an audio trancoder has to implement for transcoding an {@link AudioStream} from one
 * {@link AudioFormat} to another.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public interface AudioTranscoder {

    /**
     * Returns a simple string that uniquely identifies this transcoder.
     *
     * @return an id that identifies this transcoder
     */
    String getId();

    /**
     * Gets supported source audio formats.
     *
     * @return supported source audio formats
     */
    Set<AudioFormat> getSupportedSourceFormats();

    /**
     * Gets supported target audio formats.
     *
     * @return supported target audio formats
     */
    Set<AudioFormat> getSupportedTargetFormats();

    /**
     * Checks whether this transcoder can transcode from the specified source format to the specified target format.
     *
     * @param sourceFormat the audio format to transcode from
     * @param targetFormat the audio format to transcode to
     * @return true if this transcoder supports transcoding between the given formats
     */
    default boolean canTranscode(AudioFormat sourceFormat, AudioFormat targetFormat) {
        boolean sourceMatch = getSupportedSourceFormats().stream().anyMatch(fmt -> fmt.isCompatible(sourceFormat));
        boolean targetMatch = getSupportedTargetFormats().stream().anyMatch(fmt -> fmt.isCompatible(targetFormat));
        return sourceMatch && targetMatch;
    }

    /**
     * Relative cost/penalty of this transcoder (lower means preferred).
     * The default cost is 100.
     *
     * @implNote Must be in [0, 100].
     *
     * @return the cost metric
     */
    default int getCost() {
        return 100;
    }

    /**
     * Transcodes the given audio stream into the requested target format.
     *
     * @param source the audio stream to transcode
     * @param targetFormat the desired target format
     * @return a new audio stream in the requested target format
     * @throws AudioTranscodingException if transcoding fails or formats are unsupported
     */
    AudioStream transcode(AudioStream source, AudioFormat targetFormat) throws AudioTranscodingException;
}
