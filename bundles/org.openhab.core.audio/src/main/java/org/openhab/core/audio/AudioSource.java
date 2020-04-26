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
package org.openhab.core.audio;

import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is an audio source, which can provide a continuous live stream of audio.
 * Its main use is for microphones and other "line-in" sources and it can be registered as a service in order to make
 * it available throughout the system.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface AudioSource {

    /**
     * Returns a simple string that uniquely identifies this service
     *
     * @return an id that identifies this service
     */
    String getId();

    /**
     * Returns a localized human readable label that can be used within UIs.
     *
     * @param locale the locale to provide the label for
     * @return a localized string to be used in UIs
     */
    String getLabel(@Nullable Locale locale);

    /**
     * Obtain the audio formats supported by this AudioSource
     *
     * @return The audio formats supported by this service
     */
    Set<AudioFormat> getSupportedFormats();

    /**
     * Gets an AudioStream for reading audio data in supported audio format
     *
     * @param format the expected audio format of the stream
     * @return AudioStream for reading audio data
     * @throws AudioException If problem occurs obtaining the stream
     */
    AudioStream getInputStream(AudioFormat format) throws AudioException;
}
