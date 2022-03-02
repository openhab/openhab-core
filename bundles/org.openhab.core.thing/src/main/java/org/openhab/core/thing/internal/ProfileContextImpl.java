/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.thing.internal;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * {@link ProfileContext} implementation.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Jan N. Klug - Add accepted type methods
 */
@NonNullByDefault
public class ProfileContextImpl implements ProfileContext {

    private static final String THREAD_POOL_NAME = "profiles";
    private final Configuration configuration;

    private final List<Class<? extends State>> acceptedDataTypes;
    private final List<Class<? extends Command>> acceptedCommandTypes;

    public ProfileContextImpl(Configuration configuration) {
        this.configuration = configuration;
        this.acceptedDataTypes = List.of();
        this.acceptedCommandTypes = List.of();
    }

    public ProfileContextImpl(Configuration configuration, List<Class<? extends State>> acceptedDataTypes,
            List<Class<? extends Command>> acceptedCommandTypes) {
        this.configuration = configuration;
        this.acceptedDataTypes = acceptedDataTypes;
        this.acceptedCommandTypes = acceptedCommandTypes;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public ScheduledExecutorService getExecutorService() {
        return ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);
    }

    @Override
    public List<Class<? extends State>> getAcceptedDataTypes() {
        return acceptedDataTypes;
    }

    @Override
    public List<Class<? extends Command>> getAcceptedCommandTypes() {
        return acceptedCommandTypes;
    }
}
