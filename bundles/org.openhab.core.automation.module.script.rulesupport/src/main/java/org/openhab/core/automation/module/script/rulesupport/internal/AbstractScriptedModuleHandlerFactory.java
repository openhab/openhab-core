/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.handler.BaseModuleHandlerFactory;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.module.script.rulesupport.internal.delegates.SimpleActionHandlerDelegate;
import org.openhab.core.automation.module.script.rulesupport.internal.delegates.SimpleConditionHandlerDelegate;
import org.openhab.core.automation.module.script.rulesupport.internal.delegates.SimpleTriggerHandlerDelegate;
import org.openhab.core.automation.module.script.rulesupport.shared.ScriptedHandler;
import org.openhab.core.automation.module.script.rulesupport.shared.factories.ScriptedActionHandlerFactory;
import org.openhab.core.automation.module.script.rulesupport.shared.factories.ScriptedConditionHandlerFactory;
import org.openhab.core.automation.module.script.rulesupport.shared.factories.ScriptedTriggerHandlerFactory;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleActionHandler;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleConditionHandler;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractScriptedModuleHandlerFactory} wrappes the ScriptedHandler based on the underlying type.
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractScriptedModuleHandlerFactory extends BaseModuleHandlerFactory {
    Logger logger = LoggerFactory.getLogger(AbstractScriptedModuleHandlerFactory.class);

    protected @Nullable ModuleHandler getModuleHandler(Module module, @Nullable ScriptedHandler scriptedHandler) {
        ModuleHandler moduleHandler = null;

        if (scriptedHandler != null) {
            if (scriptedHandler instanceof SimpleActionHandler handler) {
                moduleHandler = new SimpleActionHandlerDelegate((Action) module, handler);
            } else if (scriptedHandler instanceof SimpleConditionHandler handler) {
                moduleHandler = new SimpleConditionHandlerDelegate((Condition) module, handler);
            } else if (scriptedHandler instanceof SimpleTriggerHandler handler) {
                moduleHandler = new SimpleTriggerHandlerDelegate((Trigger) module, handler);
            } else if (scriptedHandler instanceof ScriptedActionHandlerFactory factory) {
                moduleHandler = factory.get((Action) module);
            } else if (scriptedHandler instanceof ScriptedTriggerHandlerFactory factory) {
                moduleHandler = factory.get((Trigger) module);
            } else if (scriptedHandler instanceof ScriptedConditionHandlerFactory factory) {
                moduleHandler = factory.get((Condition) module);
            } else {
                logger.error("Not supported moduleHandler: {}", module.getTypeUID());
            }
        }

        return moduleHandler;
    }
}
