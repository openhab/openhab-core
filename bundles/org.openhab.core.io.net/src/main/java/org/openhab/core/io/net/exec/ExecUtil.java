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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some common methods to execute commands on command line.
 *
 * @author Pauli Anttila - Initial contribution
 * @author Kai Kreuzer - added exception logging
 * @author Connor Petty - replaced delimiter usage with argument array
 */
@NonNullByDefault
public class ExecUtil {

    private static Logger logger = LoggerFactory.getLogger(ExecUtil.class);

    private static ExecutorService executor = ThreadPoolManager.getPool("ExecUtil");

    /**
     * <p>
     * Executes <code>commandLine</code>.
     *
     * <p>
     * A possible {@link IOException} gets logged but no further processing is done.
     *
     * @param commandLine the command line to execute
     */
    public static void executeCommandLine(String... commandLine) {
        try {
            new ProcessBuilder(commandLine).redirectError(Redirect.DISCARD).redirectOutput(Redirect.DISCARD).start();
        } catch (IOException e) {
            logger.warn("Error occurred when executing commandLine '{}'", commandLine, e);
        }
    }

    /**
     * <p>
     * Executes <code>commandLine</code> and return its result.
     *
     * <p>
     * A possible {@link IOException} gets logged but no further processing is done.
     *
     * @param timeout the max time to wait for a process to finish, null to wait indefinitely
     * @param commandLine the command line to execute
     * @return response data from executed command line or <code>null</code> if a timeout or error occurred
     */
    public static @Nullable String executeCommandLineAndWaitResponse(@Nullable Duration timeout,
            String... commandLine) {

        Process processTemp = null;
        Future<String> outputFuture = null;
        Future<String> errorFuture = null;
        cleanup: try {
            Process process = processTemp = new ProcessBuilder(commandLine).start();

            outputFuture = executor.submit(() -> {
                try (InputStream inputStream = process.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringWriter output = new StringWriter();
                    reader.transferTo(output);
                    return output.toString();
                }
            });

            errorFuture = executor.submit(() -> {
                try (InputStream inputStream = process.getErrorStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringWriter output = new StringWriter();
                    reader.transferTo(output);
                    return output.toString();
                }
            });
            int exitCode;
            if (timeout == null) {
                exitCode = process.waitFor();
            } else if (process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                exitCode = process.exitValue();
            } else {
                logger.warn("Timeout occurred when executing commandLine '{}'", Arrays.toString(commandLine));
                break cleanup;
            }
            if (exitCode == 0) {
                return outputFuture.get();
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("exit code '{}', result '{}', errors '{}'", exitCode, outputFuture.get(),
                            errorFuture.get());
                }
                return errorFuture.get();
            }
        } catch (ExecutionException e) {
            if (logger.isDebugEnabled()) {
                logger.warn("Error occurred when executing commandLine '{}'", Arrays.toString(commandLine),
                        e.getCause());
            } else {
                logger.warn("Error occurred when executing commandLine '{}'", Arrays.toString(commandLine));
            }
        } catch (InterruptedException e) {
            logger.debug("commandLine '{}' was interrupted", Arrays.toString(commandLine), e);
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.warn("Failed to execute commandLine '{}'", Arrays.toString(commandLine), e);
            } else {
                logger.warn("Failed to execute commandLine '{}'", Arrays.toString(commandLine));
            }
        }
        if (processTemp != null && processTemp.isAlive()) {
            processTemp.destroyForcibly();
        }
        if (outputFuture != null) {
            outputFuture.cancel(true);
        }
        if (errorFuture != null) {
            errorFuture.cancel(true);
        }
        return null;
    }
}
