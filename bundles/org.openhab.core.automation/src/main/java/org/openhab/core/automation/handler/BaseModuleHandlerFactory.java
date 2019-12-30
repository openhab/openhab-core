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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;

/**
 * This class provides a {@link ModuleHandlerFactory} base implementation. It is used by its subclasses for base
 * implementation of creating and disposing {@link ModuleHandler} instances. They only have to implement
 * {@link #internalCreate(Module, String)} method for creating concrete instances needed for the operation of the
 * {@link Module}s.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Benedikt Niehues - change behavior for unregistering ModuleHandler
 */
@NonNullByDefault
public abstract class BaseModuleHandlerFactory implements ModuleHandlerFactory {

    private final Map<String, ModuleHandler> handlers = new HashMap<>();

    /**
     * Should be overridden by the implementations that extend this base class. Called from DS to deactivate the
     * {@link ModuleHandlerFactory}.
     */
    protected void deactivate() {
        for (ModuleHandler handler : handlers.values()) {
            handler.dispose();
        }
        handlers.clear();
    }

    /**
     * Provides all available {@link ModuleHandler}s created by concrete factory implementation.
     *
     * @return a map with keys calculated by concatenated rule UID and module Id and values representing
     *         {@link ModuleHandler} created for concrete module corresponding to the module Id and belongs to rule with
     *         such UID.
     */
    protected Map<String, ModuleHandler> getHandlers() {
        return Collections.unmodifiableMap(handlers);
    }

    @Override
    @SuppressWarnings("null")
    public @Nullable ModuleHandler getHandler(Module module, String ruleUID) {
        String id = ruleUID + module.getId();
        ModuleHandler handler = handlers.get(id);
        handler = handler == null ? internalCreate(module, ruleUID) : handler;
        if (handler != null) {
            handlers.put(id, handler);
        }
        return handler;
    }

    /**
     * Creates a new {@link ModuleHandler} for a given {@code module} and {@code ruleUID}.
     *
     * @param module the {@link Module} for which a handler should be created.
     * @param ruleUID the identifier of the {@link Rule} that the given module belongs to.
     * @return a {@link ModuleHandler} instance or {@code null} if thins module type is not supported.
     */
    protected abstract @Nullable ModuleHandler internalCreate(Module module, String ruleUID);

    @Override
    public void ungetHandler(Module module, String ruleUID, ModuleHandler handler) {
        if (handlers.remove(ruleUID + module.getId(), handler)) {
            handler.dispose();
        }
    }
}
