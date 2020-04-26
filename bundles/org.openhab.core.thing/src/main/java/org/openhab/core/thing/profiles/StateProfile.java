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
 * A {@link StateProfile} defined the communication for channels of STATE kind.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface StateProfile extends Profile {

    /**
     * Will be called if a command should be forwarded to the binding.
     *
     * @param command
     */
    void onCommandFromItem(Command command);

    /**
     * If a binding issued a command to a channel, this method will be called for each linked item.
     *
     * @param command
     */
    void onCommandFromHandler(Command command);

    /**
     * If the binding indicated a state update on a channel, then this method will be called for each linked item.
     *
     * @param state
     */
    void onStateUpdateFromHandler(State state);
}
