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
public class TemperatureConditionType extends ConditionType {

    public static final String UID = "TemperatureCondition";

    public static final String OPERATOR_HEATING = "heating";
    public static final String OPERATOR_COOLING = "cooling";
    public static final String CONFIG_OPERATOR = "operator";
    public static final String CONFIG_TEMPERATURE = "temperature";
    public static final String INPUT_CURRENT_TEMPERATURE = "currentTemperature";

    public static TemperatureConditionType initialize() {
        final ConfigDescriptionParameter temperature = ConfigDescriptionParameterBuilder
                .create(CONFIG_TEMPERATURE, Type.INTEGER).withRequired(true).withReadOnly(true).withMultiple(false)
                .withLabel("Temperature").withDescription("Targeted room temperature").build();
        final ConfigDescriptionParameter operator = ConfigDescriptionParameterBuilder.create(CONFIG_OPERATOR, Type.TEXT)
                .withRequired(true).withReadOnly(true).withMultiple(false).withLabel("Mode")
                .withDescription("Heating/Cooling mode").build();

        final List<ConfigDescriptionParameter> config = new ArrayList<ConfigDescriptionParameter>();
        config.add(temperature);
        config.add(operator);

        Input currentTemperature = new Input(INPUT_CURRENT_TEMPERATURE, Integer.class.getName(), "Current Temperature",
                "Current room temperature", null, true, null, null);
        List<Input> input = new ArrayList<Input>();
        input.add(currentTemperature);
        return new TemperatureConditionType(config, input);
    }

    public TemperatureConditionType(List<ConfigDescriptionParameter> config, List<Input> input) {
        super(UID, config, "Temperature Condition Template", "Template for creation of a Temperature Condition.", null,
                Visibility.VISIBLE, input);
    }
}
