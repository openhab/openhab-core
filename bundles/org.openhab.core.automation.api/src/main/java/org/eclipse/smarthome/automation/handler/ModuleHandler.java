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
package org.eclipse.smarthome.automation.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.automation.ModuleHandlerCallback;

/**
 * A common interface for all module Handler interfaces. The Handler interfaces are
 * bridge between RuleManager and external modules used by the RuleManager.
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @see ModuleHandlerFactory
 */
@NonNullByDefault
public interface ModuleHandler {

    /**
     * The method is called by RuleManager to free resources when {@link ModuleHandler} is released.
     */
    public void dispose();

    /**
     * The callback is injected to the handler through this method.
     *
     * @param callback a {@link ModuleHandlerCallback} instance
     */
    void setCallback(ModuleHandlerCallback callback);

}
