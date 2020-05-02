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
package org.openhab.core.automation.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.ModuleHandlerCallback;
import org.openhab.core.config.core.Configuration;

/**
 * This is a base class that can be used by any ModuleHandler implementation
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class BaseModuleHandler<T extends Module> implements ModuleHandler {

    protected T module;

    protected @Nullable ModuleHandlerCallback callback;

    public BaseModuleHandler(T module) {
        this.module = module;
    }

    @Override
    public void setCallback(ModuleHandlerCallback callback) {
        this.callback = callback;
    }

    @Override
    public void dispose() {
        this.callback = null;
    }

    /**
     * Returns the configuration of the module.
     *
     * @return configuration of the module
     */
    protected Configuration getConfig() {
        return module.getConfiguration();
    }

    /**
     * Returns the configuration of the module and transforms it to the given class.
     *
     * @param configurationClass configuration class
     * @return configuration of module in form of the given class
     */
    protected <C> C getConfigAs(Class<C> configurationClass) {
        return getConfig().as(configurationClass);
    }
}
