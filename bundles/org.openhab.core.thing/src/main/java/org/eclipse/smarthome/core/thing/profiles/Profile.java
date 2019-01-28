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
import org.eclipse.smarthome.core.types.State;

/**
 * Common ancestor of all profile types.
 *
 * Profiles define the communication flow between the framework and bindings, i.e. how (and if) certain events and
 * commands are forwarded from the framework to the thing handler and vice versa.
 * <p>
 * Profiles are allowed to maintain some transient state internally, i.e. the same instance of a profile will be used
 * per link for all communication so that the temporal dimension can be taken in account.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@NonNullByDefault
public interface Profile {

    /**
     * Get the {@link ProfileTypeUID} of this profile.
     *
     * @return the UID of the profile type
     */
    ProfileTypeUID getProfileTypeUID();

    /**
     * Will be called if an item has changed its state and this information should be forwarded to the binding.
     *
     * @param state the new state
     */
    void onStateUpdateFromItem(State state);

}
