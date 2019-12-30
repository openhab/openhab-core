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
 * Test general purpose TTS exception
 *
 * @author Kelly Davis - Initial contribution
 */
public class TTSExceptionTest {

    /**
     * Test TTSException() constructor
     */
    @Test
    public void testConstructor0() {
        TTSException ttsException = new TTSException();
        Assert.assertNotNull("TTSException() constructor failed", ttsException);
    }

    /**
     * Test TTSException(String message, Throwable cause) constructor
     */
    @Test
    public void testConstructor1() {
        TTSException ttsException = new TTSException("Message", new Throwable());
        Assert.assertNotNull("TTSException(String, Throwable) constructor failed", ttsException);
    }

    /**
     * Test TTSException(String message) constructor
     */
    @Test
    public void testConstructor2() {
        TTSException ttsException = new TTSException("Message");
        Assert.assertNotNull("TTSException(String) constructor failed", ttsException);
    }

    /**
     * Test TTSException(Throwable cause) constructor
     */
    @Test
    public void testConstructor3() {
        TTSException ttsException = new TTSException(new Throwable());
        Assert.assertNotNull("TTSException(Throwable) constructor failed", ttsException);
    }
}
