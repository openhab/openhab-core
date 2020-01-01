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
package org.openhab.core.io.console.extensions;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.console.Console;

/**
 * A base class that should be used by console command extension for better inclusion.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractConsoleCommandExtension implements ConsoleCommandExtension {

    private final String cmd;
    private final String desc;

    /**
     * Generate a new console command extension.
     *
     * @param cmd The command the extension is used for.
     * @param desc The description what this extension is handling.
     */
    public AbstractConsoleCommandExtension(final String cmd, final String desc) {
        this.cmd = cmd;
        this.desc = desc;
    }

    @Override
    public String getCommand() {
        return cmd;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    /**
     * Build a command usage string.
     *
     * You should always use that function to use a usage string that complies to a standard format.
     *
     * @param description the description of the command
     * @return a usage string that complies to a standard format
     */
    protected String buildCommandUsage(final String description) {
        return String.format("%s - %s", getCommand(), description);
    }

    /**
     * Build a command usage string.
     *
     * You should always use that function to use a usage string that complies to a standard format.
     *
     * @param syntax the syntax format
     * @param description the description of the command
     * @return a usage string that complies to a standard format
     */
    protected String buildCommandUsage(final String syntax, final String description) {
        return String.format("%s %s - %s", getCommand(), syntax, description);
    }

    /**
     * Print the whole usages to the console.
     *
     * @param console the console that should be used for output
     */
    protected void printUsage(Console console) {
        for (final String usage : getUsages()) {
            console.printUsage(usage);
        }
    }

}
