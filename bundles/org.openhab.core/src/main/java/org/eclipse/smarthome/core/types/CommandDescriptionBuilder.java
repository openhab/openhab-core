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
package org.eclipse.smarthome.core.types;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.core.internal.types.CommandDescriptionImpl;

/**
 * Used to build instances of {@link CommandDescription}.
 *
 * @author Henning Treu - Initial contribution
 *
 */
public class CommandDescriptionBuilder {

    private final List<CommandOption> commandOptions = new ArrayList<>();

    private CommandDescriptionBuilder() {
        // prevent public instantiation
    }

    public static CommandDescriptionBuilder create() {
        return new CommandDescriptionBuilder();
    }

    public void withCommandOption(CommandOption commandOption) {
        this.commandOptions.add(commandOption);
    }

    public CommandDescription build() {
        CommandDescriptionImpl commandDescription = new CommandDescriptionImpl();
        commandOptions.forEach(co -> commandDescription.addCommandOption(co));

        return commandDescription;
    }

}
