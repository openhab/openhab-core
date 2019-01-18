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
 * Test general purpose STT exception
 *
 * @author Kelly Davis - Initial contribution and API
 */
public class STTExceptionTest {

    /**
     * Test STTException() constructor
     */
    @Test
    public void testConstructor0() {
        STTException ttsException = new STTException();
        Assert.assertNotNull("STTException() constructor failed", ttsException);
    }

    /**
     * Test STTException(String message, Throwable cause) constructor
     */
    @Test
    public void testConstructor1() {
        STTException ttsException = new STTException("Message", new Throwable());
        Assert.assertNotNull("STTException(String, Throwable) constructor failed", ttsException);
    }

    /**
     * Test STTException(String message) constructor
     */
    @Test
    public void testConstructor2() {
        STTException ttsException = new STTException("Message");
        Assert.assertNotNull("STTException(String) constructor failed", ttsException);
    }

    /**
     * Test STTException(Throwable cause) constructor
     */
    @Test
    public void testConstructor3() {
        STTException ttsException = new STTException(new Throwable());
        Assert.assertNotNull("STTException(Throwable) constructor failed", ttsException);
    }
}
