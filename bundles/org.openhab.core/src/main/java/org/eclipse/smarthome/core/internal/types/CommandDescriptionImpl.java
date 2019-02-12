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
package org.eclipse.smarthome.core.internal.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.types.CommandDescription;
import org.eclipse.smarthome.core.types.CommandOption;

/**
 * The {@link CommandDescriptionImpl} groups state command properties.
 *
 * @author Henning Treu - initial contribution
 *
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
}
