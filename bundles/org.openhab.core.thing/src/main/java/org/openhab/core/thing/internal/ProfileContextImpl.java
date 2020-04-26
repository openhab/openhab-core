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
package org.openhab.core.thing.internal;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.profiles.ProfileContext;

/**
 * {@link ProfileContext} implementation.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class ProfileContextImpl implements ProfileContext {

    private static final String THREAD_POOL_NAME = "profiles";
    private final Configuration configuration;

    public ProfileContextImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public ScheduledExecutorService getExecutorService() {
        return ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME);
    }
}
