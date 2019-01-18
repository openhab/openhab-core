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
package org.eclipse.smarthome.automation.module.script.internal.factory;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
import org.eclipse.smarthome.automation.module.script.ScriptEngineManager;
import org.eclipse.smarthome.automation.module.script.internal.handler.ScriptActionHandler;
import org.eclipse.smarthome.automation.module.script.internal.handler.ScriptConditionHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This HandlerFactory creates ModuleHandlers for scripts.
 *
 * @author Kai Kreuzer
 *
 */
@NonNullByDefault
@Component(service = ModuleHandlerFactory.class)
public class ScriptModuleHandlerFactory extends BaseModuleHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(ScriptModuleHandlerFactory.class);

    private @NonNullByDefault({}) ScriptEngineManager scriptEngineManager;

    private static final Collection<String> TYPES = Arrays
            .asList(new String[] { ScriptActionHandler.SCRIPT_ACTION_ID, ScriptConditionHandler.SCRIPT_CONDITION });

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    public void setScriptEngineManager(ScriptEngineManager scriptEngineManager) {
        this.scriptEngineManager = scriptEngineManager;
    }

    public void unsetScriptEngineManager(ScriptEngineManager scriptEngineManager) {
        this.scriptEngineManager = null;
    }

    @Override
    protected @Nullable ModuleHandler internalCreate(Module module, String ruleUID) {
        logger.trace("create {} -> {}", module.getId(), module.getTypeUID());
        String moduleTypeUID = module.getTypeUID();
        if (moduleTypeUID != null) {
            if (ScriptConditionHandler.SCRIPT_CONDITION.equals(moduleTypeUID) && module instanceof Condition) {
                ScriptConditionHandler handler = new ScriptConditionHandler((Condition) module, ruleUID,
                        scriptEngineManager);
                return handler;
            } else if (ScriptActionHandler.SCRIPT_ACTION_ID.equals(moduleTypeUID) && module instanceof Action) {
                ScriptActionHandler handler = new ScriptActionHandler((Action) module, ruleUID, scriptEngineManager);
                return handler;
            } else {
                logger.error("The ModuleHandler is not supported: {}", moduleTypeUID);
            }
        } else {
            logger.error("ModuleType is not registered: {}", moduleTypeUID);
        }
        return null;
    }

}
