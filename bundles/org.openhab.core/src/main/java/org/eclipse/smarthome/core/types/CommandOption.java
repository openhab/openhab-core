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

import java.nio.channels.Channel;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Represents a command option for "write only" command {@link Channel}s. CommandOptions will be rendered as
 * push-buttons in the UI and will not represent a state.
 *
 * @author Henning Treu - Initial contribution
 *
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
    private final String label;

    public CommandOption(String command, String label) {
        this.command = command;
        this.label = label;
    }

    public String getCommand() {
        return command;
    }

    public String getLabel() {
        return label;
    }

}
