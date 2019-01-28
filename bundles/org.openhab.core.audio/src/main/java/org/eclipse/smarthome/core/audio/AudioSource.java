/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.audio;

import java.util.Locale;
import java.util.Set;

/**
 * This is an audio source, which can provide a continuous live stream of audio.
 * Its main use is for microphones and other "line-in" sources and it can be registered as a service in order to make
 * it available throughout the system.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
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
    String getLabel(Locale locale);

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
