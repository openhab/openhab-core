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
import org.eclipse.smarthome.automation.type.Output;
import org.eclipse.smarthome.automation.type.TriggerType;

/**
 * The purpose of this class is to illustrate how to create {@link TriggerType}
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class LightsTriggerType extends TriggerType {

    public static final String UID = "LightsTrigger";

    public static LightsTriggerType initialize() {
        Output state = new Output(StateConditionType.INPUT_CURRENT_STATE, String.class.getName(), "State",
                "Indicates the state of Lights", null, null, null);
        List<Output> output = new ArrayList<Output>();
        output.add(state);
        return new LightsTriggerType(output);
    }

    public LightsTriggerType(List<Output> output) {
        super(UID, null, "Lights State Trigger", "Template for creation of an Lights State Rule Trigger.", null,
                Visibility.VISIBLE, output);
    }
}
