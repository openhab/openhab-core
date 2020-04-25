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
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.Output;
import org.openhab.core.config.core.Configuration;

/**
 * This class is implementation of {@link Condition} modules used in the {@link RuleEngineImpl}s.
 *
 * @author Yordan Mihaylov - Initial contribution
 */
@NonNullByDefault
public class ConditionImpl extends ModuleImpl implements Condition {

    private Map<String, String> inputs = Collections.emptyMap();

    /**
     * Constructor of {@link Condition} module object.
     *
     * @param id id of the module.
     * @param typeUID unique module type id.
     * @param configuration configuration values of the {@link Condition} module.
     * @param label the label
     * @param description description
     * @param inputs set of {@link Input}s used by this module.
     */
    public ConditionImpl(String id, String typeUID, @Nullable Configuration configuration, @Nullable String label,
            @Nullable String description, @Nullable Map<String, String> inputs) {
        super(id, typeUID, configuration, label, description);
        this.inputs = inputs == null ? Collections.emptyMap() : Collections.unmodifiableMap(inputs);
    }

    /**
     * This method is used to get input connections of the Condition. The connections
     * are links between {@link Input}s of the current {@link Module} and {@link Output}s of other
     * {@link Module}s.
     *
     * @return map that contains the inputs of this condition.
     */
    @Override
    public Map<String, String> getInputs() {
        return inputs;
    }
}
