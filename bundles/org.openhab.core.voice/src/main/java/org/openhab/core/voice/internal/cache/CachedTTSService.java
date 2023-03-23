/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.voice.internal.cache;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.voice.TTSException;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.Voice;

/**
 *
 * @author Gwendal Roulleau - Initial contribution
 */
@NonNullByDefault
public interface CachedTTSService extends TTSService {

    /**
     * Construct a uniquely identifying string for the request. Could be overridden by the TTS service if
     * it uses some unique external parameter and wants to identify variability in the cache.
     *
     * @param text The text to convert to speech
     * @param voice The voice to use for speech
     * @param requestedFormat The audio format to return the results in
     * @return A likely unique key identifying the combination of parameters and/or internal state,
     *         as a string suitable to be part of a filename. This will be used in the cache system to store the result.
     */
    String getCacheKey(String text, Voice voice, AudioFormat requestedFormat);

    /**
     * Returns an {@link AudioStream} containing the TTS results. Note, one
     * can only request a supported {@code Voice} and {@link AudioStream} or
     * an exception is thrown.
     * The result will be cached if the TTSCacheService is activated.
     *
     * @param text The text to convert to speech
     * @param voice The voice to use for speech
     * @param requestedFormat The audio format to return the results in
     * @return AudioStream containing the TTS results
     * @throws TTSException If {@code voice} and/or {@code requestedFormat}
     *             are not supported or another error occurs while creating an
     *             {@link AudioStream}
     */
    AudioStream synthesizeForCache(String text, Voice voice, AudioFormat requestedFormat) throws TTSException;
}
