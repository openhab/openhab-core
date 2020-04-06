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
package org.openhab.core.internal.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandOption;

/**
 * The {@link CommandDescriptionImpl} groups state command properties.
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
public class CommandDescriptionImpl implements CommandDescription {

    private final List<CommandOption> commandOptions;

    public CommandDescriptionImpl() {
        commandOptions = new ArrayList<>();
    }

    /**
     * Adds a {@link CommandOption} to this {@link CommandDescriptionImpl}.
     *
     * @param commandOption a commandOption to be added to this {@link CommandDescriptionImpl}.
     */
    public void addCommandOption(CommandOption commandOption) {
        commandOptions.add(commandOption);
    }

    @Override
    public List<CommandOption> getCommandOptions() {
        return Collections.unmodifiableList(commandOptions);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + commandOptions.hashCode();
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
        CommandDescriptionImpl other = (CommandDescriptionImpl) obj;
        return commandOptions.equals(other.commandOptions);
    }

    @Override
    public String toString() {
        return "CommandDescription [commandOptions=" + commandOptions + "]";
    }
}
