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
 * This is the interface that a keyword spotting service has to implement.
 *
 * @author Kelly Davis - Initial contribution
 * @author Kai Kreuzer - Refactored to use AudioStream
 */
public interface KSService {

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
     * Obtain the Locales available from this KSService
     *
     * @return The Locales available from this service
     */
    public Set<Locale> getSupportedLocales();

    /**
     * Obtain the audio formats supported by this KSService
     *
     * @return The audio formats supported by this service
     */
    public Set<AudioFormat> getSupportedFormats();

    /**
     * This method starts the process of keyword spotting
     *
     * The audio data of the passed {@link AudioStream} is passed to the keyword
     * spotting engine. The keyword spotting attempts to spot {@code keyword} as
     * being spoken in the passed {@code Locale}. Spotted keyword is indicated by
     * fired {@link KSEvent} events targeting the passed {@link KSListener}.
     *
     * The passed {@link AudioStream} must be of a supported {@link AudioFormat}.
     * In other words a {@link AudioFormat} compatible with one returned from
     * the {@code getSupportedFormats()} method.
     *
     * The passed {@code Locale} must be supported. That is to say it must be
     * a {@code Locale} returned from the {@code getSupportedLocales()} method.
     *
     * The passed {@code keyword} is the keyword which should be spotted.
     *
     * The method is supposed to return fast, i.e. it should only start the spotting as a background process.
     *
     * @param ksListener Non-null {@link KSListener} that {@link KSEvent} events target
     * @param audioStream The {@link AudioStream} from which keywords are spotted
     * @param locale The {@code Locale} in which the target keywords are spoken
     * @param keyword The keyword which to spot
     * @return A {@link KSServiceHandle} used to abort keyword spotting
     * @throws A {@link KSException} if any parameter is invalid or a problem occurs
     */
    public KSServiceHandle spot(KSListener ksListener, AudioStream audioStream, Locale locale, String keyword)
            throws KSException;
}
