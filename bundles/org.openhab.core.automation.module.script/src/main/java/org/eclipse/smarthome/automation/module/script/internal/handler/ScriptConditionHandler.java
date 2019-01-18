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
package org.eclipse.smarthome.automation.module.script.internal.handler;

import java.util.Map;
import java.util.Optional;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.handler.ConditionHandler;
import org.eclipse.smarthome.automation.module.script.ScriptEngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler can evaluate a condition based on a script.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Merschjohann
 *
 */
public class ScriptConditionHandler extends AbstractScriptModuleHandler<Condition> implements ConditionHandler {

    public final Logger logger = LoggerFactory.getLogger(ScriptConditionHandler.class);

    public static final String SCRIPT_CONDITION = "script.ScriptCondition";

    public ScriptConditionHandler(Condition module, String ruleUID, ScriptEngineManager scriptEngineManager) {
        super(module, ruleUID, scriptEngineManager);
    }

    @Override
    public boolean isSatisfied(final Map<String, Object> context) {
        boolean result = false;
        Optional<ScriptEngine> engine = getScriptEngine();

        if (engine.isPresent()) {
            ScriptEngine scriptEngine = engine.get();
            setExecutionContext(scriptEngine, context);
            try {
                Object returnVal = scriptEngine.eval(script);
                if (returnVal instanceof Boolean) {
                    result = (boolean) returnVal;
                } else {
                    logger.error("Script did not return a boolean value, but '{}'", returnVal.toString());
                }
            } catch (ScriptException e) {
                logger.error("Script execution failed: {}", e.getMessage());
            }
        }

        return result;
    }

}
