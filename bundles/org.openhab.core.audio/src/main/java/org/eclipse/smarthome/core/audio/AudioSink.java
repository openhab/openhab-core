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

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * Definition of an audio output like headphones, a speaker or for writing to
 * a file / clip.
 *
 * @author Harald Kuhn - Initial API
 * @author Kelly Davis - Modified to match discussion in #584
 * @author Christoph Weitkamp - Added getSupportedStreams() and UnsupportedAudioStreamException
 * 
 */
@NonNullByDefault
public interface AudioSink {

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
    @Nullable
    public String getLabel(Locale locale);

    /**
     * Processes the passed {@link AudioStream}
     *
     * If the passed {@link AudioStream} is not supported by this instance, an {@link UnsupportedAudioStreamException}
     * is thrown.
     *
     * If the passed {@link AudioStream} has a {@link AudioFormat} not supported by this instance,
     * an {@link UnsupportedAudioFormatException} is thrown.
     *
     * In case the audioStream is null, this should be interpreted as a request to end any currently playing stream.
     *
     * @param audioStream the audio stream to play or null to keep quiet
     * @throws UnsupportedAudioFormatException If audioStream format is not supported
     * @throws UnsupportedAudioStreamException If audioStream is not supported
     */
    void process(@Nullable AudioStream audioStream)
            throws UnsupportedAudioFormatException, UnsupportedAudioStreamException;

    /**
     * Gets a set containing all supported audio formats
     *
     * @return A Set containing all supported audio formats
     */
    public Set<AudioFormat> getSupportedFormats();

    /**
     * Gets a set containing all supported audio stream formats
     * 
     * @return A Set containing all supported audio stream formats
     */
    public Set<Class<? extends AudioStream>> getSupportedStreams();

    /**
     * Gets the volume
     *
     * @return a PercentType value between 0 and 100 representing the actual volume
     * @throws IOException if the volume can not be determined
     */
    public PercentType getVolume() throws IOException;

    /**
     * Sets the volume
     *
     * @param volume a PercentType value between 0 and 100 representing the desired volume
     * @throws IOException if the volume can not be set
     */
    public void setVolume(PercentType volume) throws IOException;
}
