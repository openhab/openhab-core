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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test SpeechRecognitionEvent event
 *
 * @author Kelly Davis - Initial contribution
 */
public class SpeechRecognitionEventTest {

    /**
     * Test SpeechRecognitionEvent(String, float) constructor
     */
    @Test
    public void testConstructor() {
        SpeechRecognitionEvent sRE = new SpeechRecognitionEvent("Message", 0.5f);
        assertNotNull(sRE, "SpeechRecognitionEvent(String, float) constructor failed");
    }

    /**
     * Test SpeechRecognitionEvent.getTranscript() method
     */
    @Test
    public void getTranscriptTest() {
        SpeechRecognitionEvent sRE = new SpeechRecognitionEvent("Message", 0.5f);
        assertEquals("Message", sRE.getTranscript(), "SpeechRecognitionEvent.getTranscript() method failed");
    }

    /**
     * Test SpeechRecognitionEvent.getConfidence() method
     */
    @Test
    public void getConfidenceTest() {
        SpeechRecognitionEvent sRE = new SpeechRecognitionEvent("Message", 0.5f);
        assertEquals((double) 0.5f, (double) sRE.getConfidence(), (double) 0.001f,
                "SpeechRecognitionEvent.getConfidence() method failed");
    }
}
