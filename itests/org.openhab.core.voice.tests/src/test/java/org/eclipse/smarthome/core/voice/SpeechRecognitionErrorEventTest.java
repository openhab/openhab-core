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
 * Test SpeechRecognitionErrorEvent event
 *
 * @author Kelly Davis - Initial contribution and API
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
