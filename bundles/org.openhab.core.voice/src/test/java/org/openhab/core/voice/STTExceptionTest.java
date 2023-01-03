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
 * Test general purpose STT exception
 *
 * @author Kelly Davis - Initial contribution
 */
@NonNullByDefault
public class STTExceptionTest {

    /**
     * Test STTException() constructor
     */
    @Test
    public void testConstructor0() {
        STTException ttsException = new STTException();
        assertNotNull(ttsException, "STTException() constructor failed");
    }

    /**
     * Test STTException(String message, Throwable cause) constructor
     */
    @Test
    public void testConstructor1() {
        STTException ttsException = new STTException("Message", new Throwable());
        assertNotNull(ttsException, "STTException(String, Throwable) constructor failed");
    }

    /**
     * Test STTException(String message) constructor
     */
    @Test
    public void testConstructor2() {
        STTException ttsException = new STTException("Message");
        assertNotNull(ttsException, "STTException(String) constructor failed");
    }

    /**
     * Test STTException(Throwable cause) constructor
     */
    @Test
    public void testConstructor3() {
        STTException ttsException = new STTException(new Throwable());
        assertNotNull(ttsException, "STTException(Throwable) constructor failed");
    }
}
