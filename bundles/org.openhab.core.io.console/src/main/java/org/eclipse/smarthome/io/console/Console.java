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
package org.eclipse.smarthome.io.console;

/**
 * This interface must be implemented by consoles which want to use the {@link ConsoleInterpreter}.
 * It allows basic output commands.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public interface Console {

    public void print(String s);

    public void println(String s);

    /**
     * usage output is treated differently from other output as it might
     * differ between different kinds of consoles
     *
     * @param s the main usage string (console independent)
     */
    public void printUsage(String s);
}
