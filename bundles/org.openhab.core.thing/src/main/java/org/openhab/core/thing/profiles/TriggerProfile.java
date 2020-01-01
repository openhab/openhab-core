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

/**
 * A {@link TriggerProfile} specifies the communication between the framework and the handler for trigger channels.
 *
 * Although trigger channels by their nature do not have a state, it becomes possible to link such trigger channels to
 * items using such a profile.
 * <p>
 * The main purpose of a {@link TriggerProfile} is to listen to triggered events and use them to calculate a meaningful
 * state.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public interface TriggerProfile extends Profile {

    /**
     * Will be called whenever the binding intends to issue a trigger event.
     *
     * @param event the event payload
     */
    void onTriggerFromHandler(String event);

}
