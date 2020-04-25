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

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.Configuration;

/**
 * The profile's context
 *
 * It gives access to related information like the profile's configuration or a scheduler.
 *
 * @author Simon Kaufmann - Initial contribution
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
}
