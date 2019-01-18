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

import java.util.Map;

import org.eclipse.smarthome.automation.ModuleHandlerCallback;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.type.Output;

/**
 * This is a callback interface to RuleManager which is used by the {@link TriggerHandler} to notify the RuleManager
 * about firing of the {@link Trigger}. These calls from {@link Trigger}s must be stored in a queue
 * and applied to the RuleAngine in order of their appearance. Each {@link Rule} has to create its own instance of
 * {@link TriggerHandlerCallback}.
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Kai Kreuzer - made it a sub-interface of ModuleHandlerCallback
 */
public interface TriggerHandlerCallback extends ModuleHandlerCallback {

    /**
     * This method is used by the {@link TriggerHandler} to notify the RuleManager when
     * the liked {@link Trigger} instance was fired.
     *
     * @param trigger instance of trigger which was fired. When one TriggerHandler
     *                serve more then one {@link Trigger} instances, this parameter
     *                defines which trigger was fired.
     * @param context is a {@link Map} of output values of the triggered {@link Trigger}. Each entry of the map
     *                contains:
     *                <ul>
     *                <li><code>key</code> - the id of the {@link Output} ,
     *                <li><code>value</code> - represents output value of the {@link Trigger}'s {@link Output}
     *                </ul>
     */
    public void triggered(Trigger trigger, Map<String, ?> context);

}
