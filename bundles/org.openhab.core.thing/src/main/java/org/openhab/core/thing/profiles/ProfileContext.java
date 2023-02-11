/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * The profile's context
 *
 * It gives access to related information like the profile's configuration or a scheduler.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Jan N. Klug - Add accepted type methods
 */
@NonNullByDefault
public interface ProfileContext {

    /**
     * Get the profile's configuration object
     *
     * @return the configuration
     */
    Configuration getConfiguration();

    /**
     * Get a scheduler to be used within profiles (if needed at all)
     *
     * @return the scheduler
     */
    ScheduledExecutorService getExecutorService();

    /**
     * Get the list of accepted data types for state updates to the linked item
     *
     * This is an optional method and will return an empty list if not implemented.
     *
     * @return A list of all accepted data types
     */
    default List<Class<? extends State>> getAcceptedDataTypes() {
        return List.of();
    }

    /**
     * Get the list of accepted command types for commands send to the linked item
     *
     * This is an optional method and will return an empty list if not implemented.
     *
     * @return A list of all accepted command types
     */
    default List<Class<? extends Command>> getAcceptedCommandTypes() {
        return List.of();
    }

    /**
     * Get the list of accepted command types for commands sent to the handler
     *
     * This is an optional method and will return an empty list if not implemented.
     *
     * @return A list of all accepted command types
     */
    default List<Class<? extends Command>> getHandlerAcceptedCommandTypes() {
        return List.of();
    }
}
