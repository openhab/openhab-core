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
package org.openhab.core.automation.module.script.rulesupport.shared.simple;

import java.util.Map;

import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.module.script.rulesupport.shared.ScriptedHandler;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
public abstract class SimpleTriggerHandler implements ScriptedHandler {
    private SimpleTriggerHandlerCallback ruleCallback;

    public void init(Trigger module) {
    }

    public void setRuleEngineCallback(Trigger module, SimpleTriggerHandlerCallback ruleCallback) {
        this.ruleCallback = ruleCallback;
    }

    protected void trigger(Map<String, ?> context) {
        this.ruleCallback.triggered(context);
    }
}
