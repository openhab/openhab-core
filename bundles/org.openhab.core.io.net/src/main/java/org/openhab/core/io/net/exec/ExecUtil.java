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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * @author Connor Petty - rewrote
 */
@NonNullByDefault
public class ExecUtil {

    private static Logger logger = LoggerFactory.getLogger(ExecUtil.class);

    private static ExecutorService executor = ThreadPoolManager.getPool("ExecUtil");

    /**
     * <p>
     * Executes <code>command</code>.
     *
     * <p>
     * A possible {@link IOException} gets logged but no further processing is done.
     *
     * @param command the command line to execute
     * @param arguments optional arguments to pass to the command
     */
    public static void executeCommandLine(String command, String... arguments) {
        List<String> commandLine = Stream.concat(Stream.of(command), Stream.of(arguments)).collect(Collectors.toList());
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
     * @param command the command line to execute
     * @param timeout timeout for execution in milliseconds, 0 to wait indefinitely
     * @param arguments optional arguments to pass to the command
     * @return response data from executed command line or <code>null</code> if an error occurred
     */
    public static @Nullable String executeCommandLineAndWaitResponse(String command, int timeout, String... arguments) {
        List<String> commandLine = Stream.concat(Stream.of(command), Stream.of(arguments)).collect(Collectors.toList());

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
            if (timeout == 0) {
                exitCode = process.waitFor();
            } else if (process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
                exitCode = process.exitValue();
            } else {
                logger.warn("Timeout occurred when executing commandLine '{}'", commandLine);
                break cleanup;
            }
            if (exitCode == 0) {
                return outputFuture.get();
            } else {
                logger.debug("exit code '{}', result '{}', errors '{}'", exitCode, outputFuture.get(),
                        errorFuture.get());
                return null;
            }
        } catch (ExecutionException e) {
            logger.warn("Error occurred when executing commandLine '{}'", commandLine, e.getCause());
        } catch (InterruptedException e) {
            logger.debug("commandLine '{}' was interrupted", commandLine, e);
        } catch (IOException e) {
            logger.warn("Error occurred when executing commandLine '{}'", commandLine, e);
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
