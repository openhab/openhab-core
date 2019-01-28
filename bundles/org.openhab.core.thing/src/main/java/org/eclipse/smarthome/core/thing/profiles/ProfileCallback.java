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
package org.eclipse.smarthome.core.thing.profiles;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;

/**
 * Gives access to the framework features for continuing the communication flow.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
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
     * Forward the given state update to the respective thing handler.
     *
     * @param state
     */
    void handleUpdate(State state);

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
