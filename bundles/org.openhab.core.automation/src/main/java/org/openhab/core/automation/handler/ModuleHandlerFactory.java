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

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;

/**
 * This interface represents a factory for {@link ModuleHandler} instances. It is used for creating
 * and disposing the {@link TriggerHandler}s, {@link ConditionHandler}s and {@link ActionHandler}s
 * needed for the operation of the {@link Module}s included in {@link Rule}s.
 * <p>
 * {@link ModuleHandlerFactory} implementations must be registered as services in the OSGi framework.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Benedikt Niehues - change behavior for unregistering ModuleHandler
 */
@NonNullByDefault
public interface ModuleHandlerFactory {

    /**
     * Returns the UIDs of the module types currently supported by this factory.
     * A {@link ModuleHandlerFactory} instance can add new types to this list, but should not remove. If a
     * module type is no longer supported, the {@link ModuleHandlerFactory} service must be unregistered, and
     * then registered again with the new list.
     * <p>
     * If two or more {@link ModuleHandlerFactory}s support the same module type, the Rule Engine will choose
     * one of them randomly. Once a factory is chosen, it will be used to create instances of this module
     * type until its service is unregistered.
     *
     * @return collection of module type UIDs supported by this factory.
     */
    public Collection<String> getTypes();

    /**
     * Creates a {@link ModuleHandler} instance needed for the operation of the {@link Module}s
     * included in {@link Rule}s.
     *
     * @param module the {@link Module} for which a {@link ModuleHandler} instance must be created.
     * @param ruleUID the identifier of the {@link Rule} that the given module belongs to.
     * @return a new {@link ModuleHandler} instance, or {@code null} if the type of the
     *         {@code module} parameter is not supported by this factory.
     */
    public @Nullable ModuleHandler getHandler(Module module, String ruleUID);

    /**
     * Releases the {@link ModuleHandler} instance when it is not needed anymore
     * for handling the specified {@code module} in the {@link Rule} with the specified {@code ruleUID}.
     * If no other {@link Rule}s and {@link Module}s use this {@code handler} instance, it should be disposed.
     *
     * @param module the {@link Module} for which the {@code handler} was created.
     * @param ruleUID the identifier of the {@link Rule} that the given module belongs to.
     * @param handler the {@link ModuleHandler} instance that is no longer needed.
     */
    public void ungetHandler(Module module, String ruleUID, ModuleHandler handler);
}
