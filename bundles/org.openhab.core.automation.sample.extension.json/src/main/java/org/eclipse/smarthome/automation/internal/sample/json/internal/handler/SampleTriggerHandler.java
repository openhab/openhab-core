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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseTriggerModuleHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandlerCallback;

/**
 * Trigger Handler sample implementation
 *
 * @author Vasil Ilchev - Initial Contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 */
public class SampleTriggerHandler extends BaseTriggerModuleHandler {
    private static final String OUTPUT_REFERENCE = "triggerOutput";
    private final String ruleUID;

    public SampleTriggerHandler(Trigger module, String ruleUID) {
        super(module);
        this.ruleUID = ruleUID;
    }

    public void trigger(String triggerParam) {
        Map<String, Object> outputs = new HashMap<String, Object>();
        outputs.put(OUTPUT_REFERENCE, triggerParam);
        ((TriggerHandlerCallback) callback).triggered(module, outputs);
    }

    String getTriggerID() {
        return module.getId();
    }

    public String getRuleUID() {
        return ruleUID;
    }

}
