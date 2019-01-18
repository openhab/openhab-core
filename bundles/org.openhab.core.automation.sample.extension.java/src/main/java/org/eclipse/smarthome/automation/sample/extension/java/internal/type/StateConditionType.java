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
package org.eclipse.smarthome.automation.sample.extension.java.internal.type;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.automation.Visibility;
import org.eclipse.smarthome.automation.type.ConditionType;
import org.eclipse.smarthome.automation.type.Input;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;

/**
 * The purpose of this class is to illustrate how to create {@link ConditionType}
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class StateConditionType extends ConditionType {

    public static final String UID = "StateCondition";

    public static final String CONFIG_STATE = "state";
    public static final String INPUT_CURRENT_STATE = "currentState";

    public static StateConditionType initialize() {
        final ConfigDescriptionParameter state = ConfigDescriptionParameterBuilder.create(CONFIG_STATE, Type.TEXT)
                .withRequired(true).withReadOnly(true).withMultiple(false).withLabel("State")
                .withDescription("State of the unit").build();

        final List<ConfigDescriptionParameter> config = new ArrayList<ConfigDescriptionParameter>();
        config.add(state);

        Input leftOperand = new Input(INPUT_CURRENT_STATE, String.class.getName(), "Current State",
                "Current state of the unit", null, true, null, null);
        List<Input> input = new ArrayList<Input>();
        input.add(leftOperand);
        return new StateConditionType(config, input);
    }

    public StateConditionType(List<ConfigDescriptionParameter> config, List<Input> input) {
        super(UID, config, "State Condition Template", "Template for creation of a State Condition.", null,
                Visibility.VISIBLE, input);
    }
}
