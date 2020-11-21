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
package org.openhab.core.thing.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * Gives access to the framework features for continuing the communication flow.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface ProfileCallback {

    /**
     * Forward the given command to the respective thing handler.
     *
     * @param command
     */
    void handleCommand(Command command);

    /**
     * Send a command to the framework.
     *
     * @param command
     */
    void sendCommand(Command command);

    /**
     * Send a state update to the framework.
     *
     * @param state
     */
    void sendUpdate(State state);
}
