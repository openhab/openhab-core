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
package org.openhab.core.types;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a command option for "write only" command channels. CommandOptions will be rendered as push-buttons in the
 * UI and will not represent a state.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class CommandOption {

    /**
     * The command which will be send to the Channel
     */
    private final String command;

    /**
     * The name of the command which will be displayed in the UI.
     */
    private @Nullable String label;

    /**
     * Creates a {@link CommandOption} object.
     *
     * @param command the command of the item
     * @param label label
     */
    public CommandOption(String command, @Nullable String label) {
        this.command = command;
        this.label = label;
    }

    /**
     * Returns the command.
     *
     * @return command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns the label.
     *
     * @return label
     */
    public @Nullable String getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + command.hashCode();
        result = prime * result + (label != null ? label.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CommandOption other = (CommandOption) obj;
        return command.equals(other.command) && (label != null ? label.equals(other.label) : other.label == null);
    }

    @Override
    public String toString() {
        return "CommandOption [command=" + command + ", label=" + label + "]";
    }
}
