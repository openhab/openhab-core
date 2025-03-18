/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.console;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This interface must be implemented by consoles which want to use the {@link ConsoleInterpreter}.
 * It allows basic output commands.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface Console {

    default void printf(String format, Object... args) {
        print(String.format(format, args));
    }

    void print(String s);

    void println(String s);

    /**
     * usage output is treated differently from other output as it might
     * differ between different kinds of consoles
     *
     * @param s the main usage string (console independent)
     */
    void printUsage(String s);

    /**
     * Reads a line from the console. The prompt is displayed before the line is read.
     *
     * @param prompt the prompt to display
     * @param mask the character to use for masking input (e.g. '*'), or null if no masking is required
     * @return the line read from the console
     * @throws IOException if an I/O error occurs
     */
    default String readLine(String prompt, final @Nullable Character mask) throws IOException {
        throw new UnsupportedOperationException("readLine not supported");
    }
}
