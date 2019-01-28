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
package org.eclipse.smarthome.core.voice;

import java.util.Locale;
import java.util.Set;

import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioStream;

/**
 * This is the interface that a speech-to-text service has to implement.
 *
 * @author Kelly Davis - Initial contribution and API
 */
public interface STTService {

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
     * Obtain the Locales available from this STTService
     *
     * @return The Locales available from this service
     */
    public Set<Locale> getSupportedLocales();

    /**
     * Obtain the audio formats supported by this STTService
     *
     * @return The audio formats supported by this service
     */
    public Set<AudioFormat> getSupportedFormats();

    /**
     * This method starts the process of speech recognition.
     *
     * The audio data of the passed {@link AudioStream} is passed to the speech
     * recognition engine. The recognition engine attempts to recognize speech
     * as being spoken in the passed {@code Locale} and containing statements
     * specified in the passed {@code grammars}. Recognition is indicated by
     * fired {@link STTEvent} events targeting the passed {@link STTListener}.
     *
     * The passed {@link AudioStream} must be of a supported {@link AudioFormat}.
     * In other words a {@link AudioFormat} compatible with one returned from
     * the {@code getSupportedFormats()} method.
     *
     * The passed {@code Locale} must be supported. That is to say it must be
     * a {@code Locale} returned from the {@code getSupportedLocales()} method.
     *
     * The passed {@code grammars} must consist of a syntactically valid grammar
     * as specified by the JSpeech Grammar Format. If {@code grammars} is null
     * or empty, large vocabulary continuous speech recognition is attempted.
     *
     * @see <a href="https://www.w3.org/TR/jsgf/">JSpeech Grammar Format.</a>
     * @param sttListener Non-null {@link STTListener} that {@link STTEvent} events target
     * @param audioStream The {@link AudioStream} from which speech is recognized
     * @param locale The {@code Locale} in which the target speech is spoken
     * @param grammars The JSpeech Grammar Format grammar specifying allowed statements
     * @return A {@link STTServiceHandle} used to abort recognition
     * @throws A {@link SSTException} if any parameter is invalid or a STT problem occurs
     */
    public STTServiceHandle recognize(STTListener sttListener, AudioStream audioStream, Locale locale,
            Set<String> grammars) throws STTException;
}
