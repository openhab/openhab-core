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
package org.openhab.core.automation.module.script.internal.factory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.handler.BaseModuleHandlerFactory;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.handler.ModuleHandlerFactory;
import org.openhab.core.automation.module.script.ScriptDependencyTracker;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.automation.module.script.internal.handler.ScriptActionHandler;
import org.openhab.core.automation.module.script.internal.handler.ScriptConditionHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This HandlerFactory creates ModuleHandlers for scripts.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component(service = { ModuleHandlerFactory.class, ScriptDependencyTracker.Listener.class })
public class ScriptModuleHandlerFactory extends BaseModuleHandlerFactory implements ScriptDependencyTracker.Listener {

    private final Logger logger = LoggerFactory.getLogger(ScriptModuleHandlerFactory.class);

    private static final Collection<String> TYPES = List.of(ScriptActionHandler.TYPE_ID,
            ScriptConditionHandler.TYPE_ID);
    private @NonNullByDefault({}) ScriptEngineManager scriptEngineManager;

    private Map<String, ScriptActionHandler> trackedHandlers = new ConcurrentHashMap<>();

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Override
    protected @Nullable ModuleHandler internalCreate(Module module, String ruleUID) {
        logger.trace("create {} -> {}", module.getId(), module.getTypeUID());
        String moduleTypeUID = module.getTypeUID();
        if (ScriptConditionHandler.TYPE_ID.equals(moduleTypeUID) && module instanceof Condition condition) {
            return new ScriptConditionHandler(condition, ruleUID, scriptEngineManager);
        } else if (ScriptActionHandler.TYPE_ID.equals(moduleTypeUID) && module instanceof Action action) {
            ScriptActionHandler handler = new ScriptActionHandler(action, ruleUID, scriptEngineManager,
                    this::onHandlerRemoval);
            trackedHandlers.put(handler.getEngineIdentifier(), handler);
            return handler;
        } else {
            logger.error("The ModuleHandler is not supported: {}", moduleTypeUID);
            return null;
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    public void setScriptEngineManager(ScriptEngineManager scriptEngineManager) {
        this.scriptEngineManager = scriptEngineManager;
    }

    public void unsetScriptEngineManager(ScriptEngineManager scriptEngineManager) {
        this.scriptEngineManager = null;
    }

    private void onHandlerRemoval(ScriptActionHandler handler) {
        trackedHandlers.values().remove(handler);
    }

    @Override
    public void onDependencyChange(String engineIdentifier) {
        ScriptActionHandler handler = trackedHandlers.get(engineIdentifier);
        if (handler != null) {
            logger.debug("Resetting script engine for script {}", engineIdentifier);
            handler.resetScriptEngine();
        }
    }
}
