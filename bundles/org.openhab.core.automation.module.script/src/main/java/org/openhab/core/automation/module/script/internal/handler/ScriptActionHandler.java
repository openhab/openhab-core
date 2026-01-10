/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.internal.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler can execute script actions.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Merschjohann - Initial contribution
 * @author Florian Hotze - Add support for script pre-compilation, Synchronize script context access if the ScriptEngine
 *         implements locking
 */
@NonNullByDefault
public class ScriptActionHandler extends AbstractScriptModuleHandler<Action> implements ActionHandler {

    public static final String TYPE_ID = "script.ScriptAction";

    private final Logger logger = LoggerFactory.getLogger(ScriptActionHandler.class);
    private final Consumer<ScriptActionHandler> onRemoval;

    /**
     * constructs a new ScriptActionHandler
     *
     * @param module the module
     * @param ruleUID the UID of the rule this handler is used for
     * @param onRemoval called on removal of this script
     */
    public ScriptActionHandler(Action module, String ruleUID, ScriptEngineManager scriptEngineManager,
            Consumer<ScriptActionHandler> onRemoval) {
        super(module, ruleUID, scriptEngineManager);

        this.onRemoval = onRemoval;
    }

    @Override
    public void dispose() {
        onRemoval.accept(this);

        super.dispose();
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public void compile() throws ScriptException {
        super.compileScript();
    }

    @Override
    public @Nullable Map<String, @Nullable Object> execute(final Map<String, Object> context) {
        Map<String, @Nullable Object> resultMap = new HashMap<>();

        if (script.isEmpty()) {
            return resultMap;
        }

        getScriptEngine().ifPresent(scriptEngine -> {
            try {
                if (scriptEngine instanceof Lock lock && !lock.tryLock(1, TimeUnit.MINUTES)) {
                    logger.error(
                            "Failed to acquire lock within one minute for script module '{}' of rule with UID '{}'",
                            module.getId(), ruleUID);
                    return;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                setExecutionContext(scriptEngine, context);
                Object result = eval(scriptEngine);
                resultMap.put("result", result);
                resetExecutionContext(scriptEngine, context);
            } finally { // Make sure that Lock is unlocked regardless of an exception being thrown or not to avoid
                        // deadlocks
                if (scriptEngine instanceof Lock lock) {
                    lock.unlock();
                }
            }
        });

        return resultMap;
    }
}
