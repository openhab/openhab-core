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
package org.openhab.core.io.net.exec;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecUtil
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ExecUtilTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Test
    public void testBasicExecuteCommandLine() {
        if (isWindowsSystem()) {
            ExecUtil.executeCommandLine("cmd", "/c", "dir");
        } else {
            ExecUtil.executeCommandLine("ls");
        }
    }

    @Test
    public void testBasicExecuteCommandLineAndWaitResponse() {
        final String result;
        if (isWindowsSystem()) {
            result = ExecUtil.executeCommandLineAndWaitResponse(TIMEOUT, "cmd", "/c", "dir");
        } else {
            result = ExecUtil.executeCommandLineAndWaitResponse(TIMEOUT, "ls");
        }
        assertNotNull(result);
        assertNotEquals("", result);
    }

    @Test
    public void testExecuteCommandLineAndWaitResponseWithArguments() {
        final String result;
        if (isWindowsSystem()) {
            result = ExecUtil.executeCommandLineAndWaitResponse(TIMEOUT, "cmd", "/c", "echo", "test");
        } else {
            result = ExecUtil.executeCommandLineAndWaitResponse(TIMEOUT, "echo", "'test'");
        }
        assertNotNull(result);
        assertNotEquals("test", result);
    }

    private boolean isWindowsSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.indexOf("windows") >= 0;
    }

    @Test
    public void testExecuteCommandLineAndWaitStdErrRedirection() {
        final String result;
        if (isWindowsSystem()) {
            result = ExecUtil.executeCommandLineAndWaitResponse(TIMEOUT, "cmd", "/c", "dir", "xxx.xxx", "1>", "nul");
        } else {
            result = ExecUtil.executeCommandLineAndWaitResponse(TIMEOUT, "ls", "xxx.xxx");
        }
        assertNotNull(result);
        assertNotEquals("", result);
    }
}
