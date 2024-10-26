/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.ConditionHandler;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler can evaluate a condition based on a script.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Merschjohann - Initial contribution
 * @author Florian Hotze - Add support for script pre-compilation, Synchronize script context access if the ScriptEngine
 *         implements locking
 */
@NonNullByDefault
public class ScriptConditionHandler extends AbstractScriptModuleHandler<Condition> implements ConditionHandler {

    public static final String TYPE_ID = "script.ScriptCondition";

    private final Logger logger = LoggerFactory.getLogger(ScriptConditionHandler.class);

    public ScriptConditionHandler(Condition module, String ruleUID, ScriptEngineManager scriptEngineManager) {
        super(module, ruleUID, scriptEngineManager);
    }

    @Override
    public void compile() throws ScriptException {
        super.compileScript();
    }

    @Override
    public boolean isSatisfied(final Map<String, Object> context) {
        boolean result = false;

        if (script.isEmpty()) {
            return true;
        }

        Optional<ScriptEngine> engine = getScriptEngine();

        if (engine.isPresent()) {
            ScriptEngine scriptEngine = engine.get();
            try {
                if (scriptEngine instanceof Lock lock && !lock.tryLock(1, TimeUnit.MINUTES)) {
                    logger.error(
                            "Failed to acquire lock within one minute for script module '{}' of rule with UID '{}'",
                            module.getId(), ruleUID);
                    return result;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                setExecutionContext(scriptEngine, context);
                Object returnVal = eval(scriptEngine, script);
                if (returnVal instanceof Boolean boolean1) {
                    result = boolean1;
                } else {
                    logger.error("Script of rule with UID '{}' did not return a boolean value, but '{}'", ruleUID,
                            returnVal);
                }
                resetExecutionContext(scriptEngine, context);
            } finally { // Make sure that Lock is unlocked regardless of an exception being thrown or not to avoid
                        // deadlocks
                if (scriptEngine instanceof Lock lock) {
                    lock.unlock();
                }
            }
        }

        return result;
    }
}
