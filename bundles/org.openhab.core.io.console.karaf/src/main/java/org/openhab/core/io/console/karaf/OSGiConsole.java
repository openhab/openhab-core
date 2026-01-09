/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.console.karaf;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.karaf.shell.api.console.Session;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jline.reader.LineReader;
import org.openhab.core.io.console.Console;

/**
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class OSGiConsole implements Console {

    private final String scope;
    private final PrintStream out;
    private final Session session;

    public OSGiConsole(final String scope, PrintStream out, Session session) {
        this.scope = scope;
        this.out = out;
        this.session = session;
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

    @Override
    public String readLine(String prompt, final @Nullable Character mask) throws IOException {
        // Prevent readLine() from logging a warning
        // see:
        // https://github.com/apache/karaf/blob/ad427cd12543dc78e095bbaa4608d7ca3d5ea4d8/shell/core/src/main/java/org/apache/karaf/shell/impl/console/ConsoleSessionImpl.java#L549
        // https://github.com/jline/jline3/blob/ee4886bf24f40288a4044f9b4b74917b58103e49/reader/src/main/java/org/jline/reader/LineReaderBuilder.java#L90
        String previousSetting = System.setProperty(LineReader.PROP_SUPPORT_PARSEDLINE, "true");
        try {
            return session.readLine(prompt, mask);
        } finally {
            if (previousSetting != null) {
                System.setProperty(LineReader.PROP_SUPPORT_PARSEDLINE, previousSetting);
            } else {
                System.clearProperty(LineReader.PROP_SUPPORT_PARSEDLINE);
            }
        }
    }

    @Override
    public @Nullable String getUser() {
        Object result = session.get("USER");
        return result != null ? result.toString() : null;
    }

    public Session getSession() {
        return session;
    }
}
