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
package org.openhab.core.model.script.actions;

import java.io.IOException;
import java.time.Duration;

import org.openhab.core.io.net.exec.ExecUtil;

/**
 * This class provides static methods that can be used in automation rules for
 * executing commands on command line.
 *
 * @author Pauli Anttila
 */
public class Exec {

    /**
     * <p>
     * Executes <code>commandLine</code>.
     *
     * <p>
     * A possible {@link IOException} gets logged but no further processing is done.
     *
     * @param commandLine
     *            the command line to execute
     * @see http://www.peterfriese.de/running-applescript-from-java/
     */
    public static void executeCommandLine(String... commandLine) {
        ExecUtil.executeCommandLine(commandLine);
    }

    /**
     * <p>
     * Executes <code>commandLine</code>.
     *
     * <p>
     * A possible {@link IOException} gets logged but no further processing is done.
     *
     * @param timeout
     *            timeout for execution, if null will wait indefinitely
     * @param commandLine
     *            the command line to execute
     * @return response data from executed command line
     */
    public static String executeCommandLine(Duration timeout, String... commandLine) {
        return ExecUtil.executeCommandLineAndWaitResponse(timeout, commandLine);
    }

}
