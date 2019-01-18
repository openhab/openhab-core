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
package org.eclipse.smarthome.automation.module.script.rulesupport.internal.delegates;

import org.eclipse.smarthome.automation.ModuleHandlerCallback;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandlerCallback;

/**
 * The {@link SimpleTriggerHandlerDelegate} allows to define triggers in a script language in different ways.
 *
 * @author Simon Merschjohann
 */
public class SimpleTriggerHandlerDelegate extends BaseModuleHandler<Trigger> implements TriggerHandler {
    private final org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler triggerHandler;

    public SimpleTriggerHandlerDelegate(Trigger module,
            org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler triggerHandler) {
        super(module);
        this.triggerHandler = triggerHandler;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void setCallback(ModuleHandlerCallback callback) {
        triggerHandler.setRuleEngineCallback(this.module,
                new SimpleTriggerHandlerCallbackDelegate(this.module, (TriggerHandlerCallback) callback));
    }
}
