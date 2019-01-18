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
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;

/**
 * The purpose of this class is to illustrate how to create {@link ActionType}
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class WelcomeHomeActionType extends ActionType {

    public static final String UID = "WelcomeHomeAction";

    public static final String CONFIG_DEVICE = "device";
    public static final String CONFIG_RESULT = "result";

    public static WelcomeHomeActionType initialize() {
        final ConfigDescriptionParameter device = ConfigDescriptionParameterBuilder.create(CONFIG_DEVICE, Type.TEXT)
                .withRequired(true).withReadOnly(true).withMultiple(false).withLabel("Device")
                .withDescription("Device description").build();
        final ConfigDescriptionParameter result = ConfigDescriptionParameterBuilder.create(CONFIG_RESULT, Type.TEXT)
                .withRequired(true).withReadOnly(true).withMultiple(false).withLabel("Result")
                .withDescription("Result description").build();
        List<ConfigDescriptionParameter> config = new ArrayList<ConfigDescriptionParameter>();
        config.add(device);
        config.add(result);
        return new WelcomeHomeActionType(config);
    }

    public WelcomeHomeActionType(List<ConfigDescriptionParameter> config) {
        super(UID, config, "Welcome Home Action Template", "Template for creation of a Welcome Home Action.", null,
                Visibility.VISIBLE, null, null);
    }
}
