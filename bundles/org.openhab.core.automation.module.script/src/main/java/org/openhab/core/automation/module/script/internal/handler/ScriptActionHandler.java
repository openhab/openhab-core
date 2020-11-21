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
package org.openhab.core.automation.module.script.internal.handler;

import java.util.HashMap;
import java.util.Map;

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
 */
@NonNullByDefault
public class ScriptActionHandler extends AbstractScriptModuleHandler<Action> implements ActionHandler {

    public static final String TYPE_ID = "script.ScriptAction";

    private final Logger logger = LoggerFactory.getLogger(ScriptActionHandler.class);

    /**
     * constructs a new ScriptActionHandler
     *
     * @param module
     * @param ruleUid the UID of the rule this handler is used for
     */
    public ScriptActionHandler(Action module, String ruleUID, ScriptEngineManager scriptEngineManager) {
        super(module, ruleUID, scriptEngineManager);
    }

    @Override
    public void dispose() {
    }

    @Override
    public @Nullable Map<String, Object> execute(final Map<String, Object> context) {
        Map<String, Object> resultMap = new HashMap<>();

        getScriptEngine().ifPresent(scriptEngine -> {
            setExecutionContext(scriptEngine, context);
            try {
                Object result = scriptEngine.eval(script);
                resultMap.put("result", result);
            } catch (ScriptException e) {
                logger.error("Script execution of rule with UID '{}' failed: {}", ruleUID, e.getMessage(),
                        logger.isDebugEnabled() ? e : null);
            }
        });

        return resultMap;
    }
}
