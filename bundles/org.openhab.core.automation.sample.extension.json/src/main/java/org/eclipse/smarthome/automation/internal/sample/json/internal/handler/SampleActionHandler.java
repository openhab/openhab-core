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
package org.eclipse.smarthome.automation.internal.sample.json.internal.handler;

import java.util.Map;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;

/**
 * Action Handler sample implementation
 *
 * @author Vasil Ilchev - Initial Contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 */
public class SampleActionHandler extends BaseModuleHandler<Action> implements ActionHandler {

    /**
     * Constructs SampleActionHandler
     *
     * @param module
     * @param actionType
     */
    public SampleActionHandler(Action module) {
        super(module);
    }

    @Override
    public void dispose() {
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        Object message = getMessage(inputs);
        if (message == null) {
            message = "";
        }
        return null;
    }

    private Object getMessage(Map<String, ?> inputs) {
        return inputs != null ? inputs.get("message") : null;
    }
}
