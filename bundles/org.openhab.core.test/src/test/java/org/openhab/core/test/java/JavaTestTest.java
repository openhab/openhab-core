/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.test.java;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testing the test. Test suite for the JavaTest base test class.
 *
 * @author Henning Treu - Initial contribution.
 */
public class JavaTestTest {

    private JavaTest javaTest;

    @BeforeEach
    public void setup() {
        javaTest = new JavaTest();
    }

    @Test
    public void waitForAssertShouldRunAfterLastCallWhenAssertionSucceeds() {
        Runnable afterLastCall = mock(Runnable.class);
        javaTest.waitForAssert(() -> assertTrue(true), null, afterLastCall, 100, 50);
        verify(afterLastCall, times(1)).run();
    }

    @Test
    public void waitForAssertShouldRunAfterLastCallWhenAssertionFails() {
        Runnable afterLastCall = mock(Runnable.class);
        try {
            javaTest.waitForAssert(() -> assertTrue(false), null, afterLastCall, 100, 50);
        } catch (final AssertionError ex) {
        }
        verify(afterLastCall, times(1)).run();
    }

    @Test
    public void waitForAssertShouldNotCatchNPE() {
        assertThrows(NullPointerException.class, () -> {
            javaTest.waitForAssert(() -> getObject().getClass());
        });
    }

    private Object getObject() {
        return null;
    }
}
