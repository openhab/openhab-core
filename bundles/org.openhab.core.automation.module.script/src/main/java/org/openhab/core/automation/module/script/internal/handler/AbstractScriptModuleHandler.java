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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.handler.BaseModuleHandler;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an abstract class that can be used when implementing any module handler that handles scripts.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Simon Merschjohann - Initial contribution
 *
 * @param <T> the type of module the concrete handler can handle
 */
@NonNullByDefault
public abstract class AbstractScriptModuleHandler<T extends Module> extends BaseModuleHandler<T> {

    private final Logger logger = LoggerFactory.getLogger(AbstractScriptModuleHandler.class);

    /** Constant defining the configuration parameter of modules that specifies the mime type of a script */
    public static final String SCRIPT_TYPE = "type";

    /** Constant defining the configuration parameter of modules that specifies the script itself */
    public static final String SCRIPT = "script";

    protected final ScriptEngineManager scriptEngineManager;

    private final String engineIdentifier;

    private Optional<ScriptEngine> scriptEngine = Optional.empty();
    private final String type;
    protected final String script;

    protected final String ruleUID;

    public AbstractScriptModuleHandler(T module, String ruleUID, ScriptEngineManager scriptEngineManager) {
        super(module);
        this.scriptEngineManager = scriptEngineManager;
        this.ruleUID = ruleUID;
        this.engineIdentifier = UUID.randomUUID().toString();

        this.type = getValidConfigParameter(SCRIPT_TYPE, module.getConfiguration(), module.getId());
        this.script = getValidConfigParameter(SCRIPT, module.getConfiguration(), module.getId());
    }

    private static String getValidConfigParameter(String parameter, Configuration config, String moduleId) {
        Object value = config.get(parameter);
        if (value != null && value instanceof String && !((String) value).trim().isEmpty()) {
            return (String) value;
        } else {
            throw new IllegalStateException(String.format(
                    "Config parameter '%s' is missing in the configuration of module '%s'.", parameter, moduleId));
        }
    }

    @Override
    public void dispose() {
        scriptEngineManager.removeEngine(engineIdentifier);
    }

    protected Optional<ScriptEngine> getScriptEngine() {
        return scriptEngine.isPresent() ? scriptEngine : createScriptEngine();
    }

    private Optional<ScriptEngine> createScriptEngine() {
        ScriptEngineContainer container = scriptEngineManager.createScriptEngine(type, engineIdentifier);

        if (container != null) {
            scriptEngine = Optional.ofNullable(container.getScriptEngine());
            return scriptEngine;
        } else {
            logger.debug("No engine available for script type '{}' in action '{}'.", type, module.getId());
            return Optional.empty();
        }
    }

    /**
     * Adds the passed context variables of the rule engine to the context scope of the ScriptEngine, this should be
     * updated each time the module is executed
     *
     * @param engine the script engine that is used
     * @param context the variables and types to put into the execution context
     */
    protected void setExecutionContext(ScriptEngine engine, Map<String, ?> context) {
        ScriptContext executionContext = engine.getContext();

        // Add the rule's UID to the context and make it available as "ctx".
        // Note: We don't use "context" here as it doesn't work on all JVM versions!
        final Map<String, Object> contextNew = new HashMap<>(context);
        contextNew.put("ruleUID", this.ruleUID);
        executionContext.setAttribute("ctx", contextNew, ScriptContext.ENGINE_SCOPE);

        // Add the rule's UID to the global namespace.
        executionContext.setAttribute("ruleUID", this.ruleUID, ScriptContext.ENGINE_SCOPE);

        // add the single context entries without their prefix to the scope
        for (Entry<String, ?> entry : context.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            int dotIndex = key.indexOf('.');
            if (dotIndex != -1) {
                key = key.substring(dotIndex + 1);
            }
            executionContext.setAttribute(key, value, ScriptContext.ENGINE_SCOPE);
        }
    }
}
