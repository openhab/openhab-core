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

/**
 * A {@link STTEvent} fired when the {@link STTService} recognizes speech.
 *
 * @author Kelly Davis - Initial contribution
 */
public class SpeechRecognitionEvent implements STTEvent {
    /**
     * Confidence of recognized speech
     */
    private final float confidence;

    /**
     * Transcript of recognized speech
     */
    private final String transcript;

    /**
     * Constructs an instance with the passed {@code transcript} and {@code confidence}.
     *
     * The confidence represents a numeric estimate between 0 and 1, inclusively, of how
     * confident the recognition engine is of the transcript. A higher number means the
     * system is more confident.
     *
     * @param transcript The transcript of the recognized speech
     * @param confidence The confidence of the transcript
     */
    public SpeechRecognitionEvent(String transcript, float confidence) {
        if ((null == transcript) || (transcript.isEmpty())) {
            throw new IllegalArgumentException("The passed transcript is null or empty");
        }
        if ((confidence < 0.0) || (1.0 < confidence)) {
            throw new IllegalArgumentException("The passed confidence is less than 0.0 or greater than 1.0");
        }

        this.transcript = transcript;
        this.confidence = confidence;
    }

    /**
     * Returns the transcript of the recognized speech.
     *
     * @return The transcript of the recognized speech.
     */
    public String getTranscript() {
        return this.transcript;
    }

    /**
     * Returns the confidence of the transcript.
     *
     * The confidence represents a numeric estimate between 0 and 1, inclusively, of how
     * confident the recognition engine is of the transcript. A higher number means the
     * system is more confident.
     *
     * @return The transcript of the recognized speech.
     */
    public float getConfidence() {
        return this.confidence;
    }
}
