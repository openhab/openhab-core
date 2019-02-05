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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test SpeechRecognitionEvent event
 *
 * @author Kelly Davis - Initial contribution and API
 */
public class SpeechRecognitionEventTest {

    /**
     * Test SpeechRecognitionEvent(String, float) constructor
     */
    @Test
    public void testConstructor() {
        SpeechRecognitionEvent sRE = new SpeechRecognitionEvent("Message", 0.5f);
        Assert.assertNotNull("SpeechRecognitionEvent(String, float) constructor failed", sRE);
    }

    /**
     * Test SpeechRecognitionEvent.getTranscript() method
     */
    @Test
    public void getTranscriptTest() {
        SpeechRecognitionEvent sRE = new SpeechRecognitionEvent("Message", 0.5f);
        Assert.assertEquals("SpeechRecognitionEvent.getTranscript() method failed", "Message", sRE.getTranscript());
    }

    /**
     * Test SpeechRecognitionEvent.getConfidence() method
     */
    @Test
    public void getConfidenceTest() {
        SpeechRecognitionEvent sRE = new SpeechRecognitionEvent("Message", 0.5f);
        Assert.assertEquals("SpeechRecognitionEvent.getConfidence() method failed", (double) 0.5f,
                (double) sRE.getConfidence(), (double) 0.001f);
    }
}
