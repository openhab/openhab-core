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
package org.eclipse.smarthome.test.java;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Testing the test. Test suite for the JavaTest base test class.
 *
 * @author Henning Treu - initial contribution.
 *
 */
public class JavaTestTest {

    private JavaTest javaTest;

    @Before
    public void setup() {
        javaTest = new JavaTest();
    }

    @Test
    public void waitForAssertShouldRunAfterLastCall_whenAssertionSucceeds() {
        Runnable afterLastCall = mock(Runnable.class);
        javaTest.waitForAssert(() -> assertTrue(true), null, afterLastCall, 100, 50);
        verify(afterLastCall, times(1)).run();
    }

    @Test
    public void waitForAssertShouldRunAfterLastCall_whenAssertionFails() {
        Runnable afterLastCall = mock(Runnable.class);
        try {
            javaTest.waitForAssert(() -> assertTrue(false), null, afterLastCall, 100, 50);
        } catch (final AssertionError ex) {
        }
        verify(afterLastCall, times(1)).run();
    }

    @Test(expected = NullPointerException.class)
    public void waitForAssertShouldNotCatchNPE() {
        javaTest.waitForAssert(() -> {
            getObject().getClass();
        });
    }

    private Object getObject() {
        return null;
    }

}
