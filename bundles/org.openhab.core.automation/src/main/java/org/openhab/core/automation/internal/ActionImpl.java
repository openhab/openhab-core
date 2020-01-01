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
package org.openhab.core.automation.internal;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.Output;
import org.openhab.core.config.core.Configuration;

/**
 * This class is implementation of {@link Action} modules used in the {@link RuleEngineImpl}s.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Initial contribution
 * @author Vasil Ilchev - Initial contribution
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
