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
package org.openhab.core.voice;

import java.util.Locale;
import java.util.Set;

import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;

/**
 * This is the interface that a text-to-speech service has to implement.
 *
 * @author Kelly Davis - Initial contribution
 * @author Kai Kreuzer - Refactored to use AudioStreams
 */
public interface TTSService {

    /**
     * Returns a simple string that uniquely identifies this service
     *
     * @return an id that identifies this service
     */
    public String getId();

    /**
     * Returns a localized human readable label that can be used within UIs.
     *
     * @param locale the locale to provide the label for
     * @return a localized string to be used in UIs
     */
    public String getLabel(Locale locale);

    /**
     * Obtain the voices available from this TTSService
     *
     * @return The voices available from this service
     */
    public Set<Voice> getAvailableVoices();

    /**
     * Obtain the audio formats supported by this TTSService
     *
     * @return The audio formats supported by this service
     */
    public Set<AudioFormat> getSupportedFormats();

    /**
     * Returns an {@link AudioStream} containing the TTS results. Note, one
     * can only request a supported {@code Voice} and {@link AudioStream} or
     * an exception is thrown.
     *
     * @param text The text to convert to speech
     * @param voice The voice to use for speech
     * @param requestedFormat The audio format to return the results in
     * @return AudioStream containing the TTS results
     * @throws TTSException If {@code voice} and/or {@code requestedFormat}
     *             are not supported or another error occurs while creating an
     *             {@link AudioStream}
     */
    public AudioStream synthesize(String text, Voice voice, AudioFormat requestedFormat) throws TTSException;
}
