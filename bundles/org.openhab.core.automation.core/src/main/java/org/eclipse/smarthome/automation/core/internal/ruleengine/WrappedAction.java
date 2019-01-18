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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.core.internal.Connection;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.type.Input;
import org.eclipse.smarthome.automation.type.Output;

/**
 * This class holds the information that is necessary for the rule engine.
 *
 * @author Markus Rathgeb - Initial Contribution and API
 */
@NonNullByDefault
public class WrappedAction extends WrappedModule<Action, ActionHandler> {

    private Set<Connection> connections = Collections.emptySet();
    private Map<String, String> inputs = Collections.emptyMap();

    public WrappedAction(final Action action) {
        super(action);
        inputs = action.getInputs();
    }

    /**
     * This method sets the connections for this module.
     *
     * @param connections the set of connections for this action
     */
    public void setConnections(@Nullable Set<Connection> connections) {
        this.connections = connections == null ? Collections.emptySet() : connections;
    }

    public Set<Connection> getConnections() {
        return connections;
    }

    /**
     * This method is used to get input connections of the Action. The connections
     * are links between {@link Input}s of the this {@link Module} and {@link Output}s
     * of other {@link Module}s.
     *
     * @return map that contains the inputs of this action.
     */
    public Map<String, String> getInputs() {
        return inputs;
    }

    /**
     * This method is used to connect {@link Input}s of the action to {@link Output}s of other {@link Module}s.
     *
     * @param inputs map that contains the inputs for this action.
     */
    public void setInputs(@Nullable Map<String, String> inputs) {
        this.inputs = inputs == null ? Collections.emptyMap() : Collections.unmodifiableMap(inputs);
    }

}
