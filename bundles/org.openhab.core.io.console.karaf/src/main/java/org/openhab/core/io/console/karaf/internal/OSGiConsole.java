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
package org.openhab.core.io.console.karaf.internal;

import java.io.PrintStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.console.Console;

/**
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class OSGiConsole implements Console {

    private final String scope;
    private final PrintStream out;

    public OSGiConsole(final String scope, PrintStream out) {
        this.scope = scope;
        this.out = out;
    }

    @Override
    public void printf(String format, Object... args) {
        out.printf(format, args);
    }

    @Override
    public void print(final String s) {
        out.print(s);
    }

    @Override
    public void println(final String s) {
        out.println(s);
    }

    @Override
    public void printUsage(final String s) {
        out.println(String.format("Usage: %s:%s", scope, s));
    }
}
