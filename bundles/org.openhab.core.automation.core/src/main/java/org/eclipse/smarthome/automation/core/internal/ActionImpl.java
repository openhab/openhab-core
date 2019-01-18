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
package org.eclipse.smarthome.automation.core.internal;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.type.Input;
import org.eclipse.smarthome.automation.type.Output;
import org.eclipse.smarthome.config.core.Configuration;

/**
 * This class is implementation of {@link Action} modules used in the {@link RuleEngineImpl}s.
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Ana Dimova - Initial Contribution
 * @author Vasil Ilchev - Initial Contribution
 */
@NonNullByDefault
public class ActionImpl extends ModuleImpl implements Action {

    private Map<String, String> inputs = Collections.emptyMap();

    /**
     * Constructor of Action object.
     *
     * @param UID action unique id.
     * @param typeUID module type unique id.
     * @param configuration map of configuration values.
     * @param label the label
     * @param description description
     * @param inputs set of connections to other modules (triggers and other actions).
     */
    public ActionImpl(String UID, String typeUID, @Nullable Configuration configuration, @Nullable String label,
            @Nullable String description, @Nullable Map<String, String> inputs) {
        super(UID, typeUID, configuration, label, description);
        this.inputs = inputs == null ? Collections.emptyMap() : Collections.unmodifiableMap(inputs);
    }

    /**
     * This method is used to get input connections of the Action. The connections
     * are links between {@link Input}s of the this {@link Module} and {@link Output}s
     * of other {@link Module}s.
     *
     * @return map that contains the inputs of this action.
     */
    @Override
    public Map<String, String> getInputs() {
        return inputs;
    }
}
