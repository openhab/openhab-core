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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test SpeechRecognitionErrorEvent event
 *
 * @author Kelly Davis - Initial contribution
 */
public class SpeechRecognitionErrorEventTest {

    /**
     * Test SpeechRecognitionErrorEvent(String) constructor
     */
    @Test
    public void testConstructor() {
        SpeechRecognitionErrorEvent sRE = new SpeechRecognitionErrorEvent("Message");
        Assert.assertNotNull("SpeechRecognitionErrorEvent(String) constructor failed", sRE);
    }

    /**
     * Test SpeechRecognitionErrorEvent.getMessage() method
     */
    @Test
    public void getMessageTest() {
        SpeechRecognitionErrorEvent sRE = new SpeechRecognitionErrorEvent("Message");
        Assert.assertEquals("SpeechRecognitionErrorEvent.getMessage() method failed", "Message", sRE.getMessage());
    }
}
