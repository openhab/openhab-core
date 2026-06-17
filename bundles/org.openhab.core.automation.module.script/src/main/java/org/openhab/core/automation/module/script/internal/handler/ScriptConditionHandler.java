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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.ConditionHandler;
import org.openhab.core.automation.module.script.LockableScriptEngine;
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
    public String getTypeId() {
        return TYPE_ID;
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

        ScriptEngine scriptEngine = getScriptEngine();
        Lock lock = null;
        long timeout = 0L;
        if (scriptEngine instanceof LockableScriptEngine lockable) {
            lock = lockable.getLock();
            timeout = lockable.getLockAcquisitionTimeoutMs();
        }

        if (scriptEngine != null) {
            try {
                if (lock != null && !lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                    if (timeout < 2000L) {
                        logger.error(
                                "Failed to acquire lock within {} milliseconds for script module '{}' of rule with UID '{}'",
                                timeout, module.getId(), ruleUID);
                    } else {
                        logger.error(
                                "Failed to acquire lock within {} seconds for script module '{}' of rule with UID '{}'",
                                TimeUnit.MILLISECONDS.toSeconds(timeout), module.getId(), ruleUID);
                    }
                    return result;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            try {
                setExecutionContext(scriptEngine, context);
                Object returnVal = eval(scriptEngine);
                if (returnVal instanceof Boolean boolean1) {
                    result = boolean1;
                } else {
                    logger.error("Script of rule with UID '{}' did not return a boolean value, but '{}'", ruleUID,
                            returnVal);
                }
                resetExecutionContext(scriptEngine, context);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }

        return result;
    }
}
