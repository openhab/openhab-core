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
package org.openhab.core.io.net.exec;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for the ExecUtil
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ExecUtilTest {

    @Test
    public void testBasicExecuteCommandLine() {
        if (isWindowsSystem()) {
            ExecUtil.executeCommandLine("dir");
        } else {
            ExecUtil.executeCommandLine("ls");
        }
    }

    @Test
    public void testBasicExecuteCommandLineAndWaitResponse() {
        final String result;
        if (isWindowsSystem()) {
            result = ExecUtil.executeCommandLineAndWaitResponse("dir", 1000);
        } else {
            result = ExecUtil.executeCommandLineAndWaitResponse("ls", 1000);
        }
        assertNotNull(result);
        assertNotEquals("", result);
    }

    @Test
    public void testExecuteCommandLineAndWaitResponseWithArguments() {
        final String result;
        if (isWindowsSystem()) {
            result = ExecUtil.executeCommandLineAndWaitResponse("echo@@test", 1000);
        } else {
            result = ExecUtil.executeCommandLineAndWaitResponse("echo@@'test'", 1000);
        }
        assertNotNull(result);
        assertNotEquals("test", result);
    }

    private boolean isWindowsSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.indexOf("windows") >= 0;
    }
}
