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
package org.openhab.core.test.java;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testing the test. Test suite for the JavaTest base test class.
 *
 * @author Henning Treu - Initial contribution.
 */
@NonNullByDefault
public class JavaTestTest {

    private @NonNullByDefault({}) JavaTest javaTest;

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
    @SuppressWarnings("null")
    public void waitForAssertShouldNotCatchNPE() {
        assertThrows(NullPointerException.class, () -> {
            javaTest.waitForAssert(() -> {
                Map.of().get("key").toString();
            });
        });
    }

    @Test
    public void interceptedLoggerShouldNotLogBelowAboveMinLevel() {
        javaTest.setupInterceptedLogger(LogTest.class, JavaTest.LogLevel.INFO);

        LogTest logTest = new LogTest();
        logTest.logDebug("debug message");

        javaTest.stopInterceptedLogger(LogTest.class);
        Assertions.assertThrows(AssertionError.class,
                () -> javaTest.assertLogMessage(LogTest.class, JavaTest.LogLevel.DEBUG, "debug message"));
    }

    @Test
    public void interceptedLoggerShouldLogAboveMinLevel() {
        LogTest logTest = new LogTest();
        javaTest.setupInterceptedLogger(LogTest.class, JavaTest.LogLevel.INFO);

        logTest.logError("error message");

        javaTest.stopInterceptedLogger(LogTest.class);
        javaTest.assertLogMessage(LogTest.class, JavaTest.LogLevel.ERROR, "error message");
    }

    private static class LogTest {
        private final Logger logger = LoggerFactory.getLogger(LogTest.class);

        public LogTest() {
        }

        public void logDebug(String message) {
            logger.debug(message);
        }

        public void logError(String message) {
            logger.error(message);
        }
    }
}
