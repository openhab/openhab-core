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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some common methods to execute commands on command line.
 *
 * @author Pauli Anttila - Initial contribution
 * @author Kai Kreuzer - added exception logging
 */
public class ExecUtil {

    private static Logger logger = LoggerFactory.getLogger(ExecUtil.class);

    /**
     * Use this to separate between command and parameter, and also between parameters.
     */
    public static final String CMD_LINE_DELIMITER = "@@";

    /**
     * <p>
     * Executes <code>commandLine</code>. Sometimes (especially observed on MacOS) the commandLine isn't executed
     * properly. In that cases another exec-method is to be used. To accomplish this please use the special delimiter
     * '<code>@@</code>'. If <code>commandLine</code> contains this delimiter it is split into a String list and the
     * special exec-method is used.
     *
     * <p>
     * A possible {@link IOException} gets logged but no further processing is done.
     *
     * @param commandLine the command line to execute
     */
    public static void executeCommandLine(String commandLine) {
        internalExecute(commandLine);
    }

    /**
     * <p>
     * Executes <code>commandLine</code> and return its result. Sometimes (especially observed on MacOS) the commandLine
     * isn't executed properly. In that cases another exec-method is to be used. To accomplish this please use the
     * special delimiter '<code>@@</code>'. If <code>commandLine</code> contains this delimiter it is split into a
     * String list and the special exec-method is used.
     *
     * <p>
     * A possible {@link IOException} gets logged but no further processing is done.
     *
     * @param commandLine the command line to execute
     * @param timeout timeout for execution in milliseconds
     * @return response data from executed command line or <code>null</code> if an error occurred
     */
    public static String executeCommandLineAndWaitResponse(String commandLine, int timeout) {
        final Process process = internalExecute(commandLine);
        if (process != null) {
            try {
                process.waitFor(timeout, TimeUnit.MILLISECONDS);
                int exitCode = process.exitValue();
                final StringBuilder result = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        result.append(line).append("\n");
                    }
                }
                logger.debug("exit code '{}', result '{}'", exitCode, result);
                return result.toString();
            } catch (IOException e) {
                logger.warn("I/O exception occurred when executing commandLine '{}'", commandLine, e);
            } catch (InterruptedException e) {
                logger.warn("Timeout occurred when executing commandLine '{}'", commandLine, e);
            }
        }
        return null;
    }

    private static @Nullable Process internalExecute(String commandLine) {
        final Process process;
        try {
            if (commandLine.contains(CMD_LINE_DELIMITER)) {
                final List<String> cmdArray = Arrays.asList(commandLine.split(CMD_LINE_DELIMITER));
                process = new ProcessBuilder(cmdArray).start();
                logger.info("executed commandLine '{}'", cmdArray);
            } else {
                process = new ProcessBuilder(commandLine).start();
                logger.info("executed commandLine '{}'", commandLine);
            }
        } catch (IOException e) {
            logger.error("couldn't execute commandLine '{}'", commandLine, e);
            return null;
        }
        return process;
    }
}
