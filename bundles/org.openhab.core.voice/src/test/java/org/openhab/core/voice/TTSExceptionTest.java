/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Test general purpose TTS exception
 *
 * @author Kelly Davis - Initial contribution
 */
@NonNullByDefault
public class TTSExceptionTest {

    /**
     * Test TTSException() constructor
     */
    @Test
    public void testConstructor0() {
        TTSException ttsException = new TTSException();
        assertNotNull(ttsException, "TTSException() constructor failed");
    }

    /**
     * Test TTSException(String message, Throwable cause) constructor
     */
    @Test
    public void testConstructor1() {
        TTSException ttsException = new TTSException("Message", new Throwable());
        assertNotNull(ttsException, "TTSException(String, Throwable) constructor failed");
    }

    /**
     * Test TTSException(String message) constructor
     */
    @Test
    public void testConstructor2() {
        TTSException ttsException = new TTSException("Message");
        assertNotNull(ttsException, "TTSException(String) constructor failed");
    }

    /**
     * Test TTSException(Throwable cause) constructor
     */
    @Test
    public void testConstructor3() {
        TTSException ttsException = new TTSException(new Throwable());
        assertNotNull(ttsException, "TTSException(Throwable) constructor failed");
    }
}
