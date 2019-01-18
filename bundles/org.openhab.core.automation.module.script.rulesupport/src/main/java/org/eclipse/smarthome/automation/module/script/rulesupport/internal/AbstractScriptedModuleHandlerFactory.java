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
package org.eclipse.smarthome.automation.module.script.rulesupport.internal;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.delegates.SimpleActionHandlerDelegate;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.delegates.SimpleConditionHandlerDelegate;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.delegates.SimpleTriggerHandlerDelegate;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.ScriptedHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.factories.ScriptedActionHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.factories.ScriptedConditionHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.factories.ScriptedTriggerHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleActionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleConditionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractScriptedModuleHandlerFactory} wrappes the ScriptedHandler based on the underlying type.
 *
 * @author Simon Merschjohann
 *
 */
public abstract class AbstractScriptedModuleHandlerFactory extends BaseModuleHandlerFactory {
    Logger logger = LoggerFactory.getLogger(AbstractScriptedModuleHandlerFactory.class);

    protected ModuleHandler getModuleHandler(Module module, ScriptedHandler scriptedHandler) {
        ModuleHandler moduleHandler = null;

        if (scriptedHandler != null) {
            if (scriptedHandler instanceof SimpleActionHandler) {
                moduleHandler = new SimpleActionHandlerDelegate((Action) module, (SimpleActionHandler) scriptedHandler);
            } else if (scriptedHandler instanceof SimpleConditionHandler) {
                moduleHandler = new SimpleConditionHandlerDelegate((Condition) module,
                        (SimpleConditionHandler) scriptedHandler);
            } else if (scriptedHandler instanceof SimpleTriggerHandler) {
                moduleHandler = new SimpleTriggerHandlerDelegate((Trigger) module,
                        (SimpleTriggerHandler) scriptedHandler);
            } else if (scriptedHandler instanceof ScriptedActionHandlerFactory) {
                moduleHandler = ((ScriptedActionHandlerFactory) scriptedHandler).get((Action) module);
            } else if (scriptedHandler instanceof ScriptedTriggerHandlerFactory) {
                moduleHandler = ((ScriptedTriggerHandlerFactory) scriptedHandler).get((Trigger) module);
            } else if (scriptedHandler instanceof ScriptedConditionHandlerFactory) {
                moduleHandler = ((ScriptedConditionHandlerFactory) scriptedHandler).get((Condition) module);
            } else {
                logger.error("Not supported moduleHandler: {}", module.getTypeUID());
            }
        }

        return moduleHandler;
    }
}
