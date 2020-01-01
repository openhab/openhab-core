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
package org.openhab.core.automation.module.script.rulesupport.internal.delegates;

import org.openhab.core.automation.ModuleHandlerCallback;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseTriggerModuleHandler;
import org.openhab.core.automation.handler.TriggerHandlerCallback;

/**
 * The {@link SimpleTriggerHandlerDelegate} allows to define triggers in a script language in different ways.
 *
 * @author Simon Merschjohann - Initial contribution
 */
public class SimpleTriggerHandlerDelegate extends BaseTriggerModuleHandler {

    private final org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler triggerHandler;

    public SimpleTriggerHandlerDelegate(Trigger module,
            org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler triggerHandler) {
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
