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
package org.eclipse.smarthome.automation.core.internal.ruleengine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.handler.ModuleHandler;

/**
 * This class holds the information that is necessary for the rule engine.
 *
 * @author Markus Rathgeb - Initial Contribution and API
 */
@NonNullByDefault
public class WrappedModule<M extends Module, H extends ModuleHandler> {

    private final M module;
    private @Nullable H handler;

    protected WrappedModule(final M module) {
        this.module = module;
    }

    public M unwrap() {
        return module;
    }

    /**
     * This method gets handler which is responsible for handling of this module.
     *
     * @return handler of the module or null.
     */
    public @Nullable H getModuleHandler() {
        return handler;
    }

    /**
     * This method sets handler of the module.
     *
     * @param handler the new handler
     */
    public void setModuleHandler(final @Nullable H handler) {
        this.handler = handler;
    }
}
