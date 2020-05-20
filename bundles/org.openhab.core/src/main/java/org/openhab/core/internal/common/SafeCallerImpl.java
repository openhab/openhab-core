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
package org.openhab.core.internal.common;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.common.SafeCallerBuilder;
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 * Implementation of the {@link SafeCaller} API.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "org.openhab.safecaller", immediate = true)
public class SafeCallerImpl implements SafeCaller {

    private static final String SAFE_CALL_POOL_NAME = "safeCall";

    private final ScheduledExecutorService watcher;
    private final SafeCallManagerImpl manager;

    @Activate
    public SafeCallerImpl(@Nullable Map<String, Object> properties) {
        watcher = Executors.newSingleThreadScheduledExecutor();
        manager = new SafeCallManagerImpl(watcher, getScheduler(), false);
        modified(properties);
    }

    @Modified
    public void modified(@Nullable Map<String, Object> properties) {
        if (properties != null) {
            String enabled = (String) properties.get("singleThread");
            manager.setEnforceSingleThreadPerIdentifier("true".equalsIgnoreCase(enabled));
        }
    }

    @Deactivate
    public void deactivate() {
        watcher.shutdownNow();
    }

    @Override
    public <T> SafeCallerBuilder<T> create(T target, Class<T> interfaceType) {
        return new SafeCallerBuilderImpl<T>(target, new Class<?>[] { interfaceType }, manager);
    }

    protected ExecutorService getScheduler() {
        return ThreadPoolManager.getPool(SAFE_CALL_POOL_NAME);
    }
}
