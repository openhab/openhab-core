/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
    protected void setExecutionContext(ScriptEngine engine, Map<String, Object> context) {
        setExecutionContext(engine, context, null);
    }

    /**
     * Adds the passed context variables of the rule engine to the context scope of the ScriptEngine, this should be
     * updated each time the module is executed
     *
     * Additionally the global shared variables of the global context are injected into
     *
     * @param engine the script engine that is used
     * @param context the variables and types to put into the execution context
     * @param globalContext the global share variables to put into the execution context
     */
    protected void setExecutionContext(ScriptEngine engine, Map<String, Object> context,
            @Nullable Map<String, Object> globalContext) {
        // Prepare the script context
        final Map<String, Object> scriptContext = createScriptContext(context);

        // Prepare the execution context
        ScriptContext executionContext = engine.getContext();
        prepareExecutionContext(executionContext, scriptContext, globalContext);
    }

    /**
     * Creates a copy of the given input context and adds the ruleUID to the resulting script context
     *
     * @param context The input context of the script module
     * @return Cloned context including the ruleUID
     */
    private Map<String, Object> createScriptContext(final Map<String, Object> context) {
        // Adding additional attribute mappings should not be visible in the referenced context.
        // We therefore clone the incoming context before altering its content
        final Map<String, Object> scriptContext = cloneContext(context);

        // We add the ruleUID to the script context
        scriptContext.put("ruleUID", this.ruleUID);

        return scriptContext;
    }

    /**
     * Prepares the rules engines execution context for the scripts next execution
     *
     * @param executionContext The execution context of the script engine
     * @param scriptContext The script local variables to be added to the execution context
     * @param globalScriptContext The global shared variables to be added to the execution context
     */
    private void prepareExecutionContext(ScriptContext executionContext, Map<String, Object> scriptContext,
            @Nullable Map<String, Object> globalScriptContext) {
        // We add the script context under the key "ctx" to the script execution context using the local scope
        // Note: We don't use "context" here as it doesn't work on all JVM versions!
        executionContext.setAttribute("ctx", scriptContext, ScriptContext.ENGINE_SCOPE);

        // We add the ruleUID under a second mapping using the local scope
        executionContext.setAttribute("ruleUID", scriptContext.get("ruleUID"), ScriptContext.ENGINE_SCOPE);

        // We create second mappings for all context entries using their keys as attribute names stripped by the prefix
        addContextEntriesAsAttributes(executionContext, scriptContext);

        if (globalScriptContext != null) {
            // Finally we add the global script context under the global scope if present
            executionContext.setAttribute("ctx.global", globalScriptContext, ScriptContext.GLOBAL_SCOPE);
        } else {
            // Otherwise we add an empty context in the local scope
            executionContext.setAttribute("ctx.global", new ConcurrentHashMap<>(), ScriptContext.ENGINE_SCOPE);
        }
    }

    /**
     * Adds all entries from the script context additionally as single entries to the execution context. If the
     * attribute name is prefixed by a dot notation, the prefix is removed before adding.
     *
     * @param executionContext The script excution context
     * @param scriptContext The script context entries to be added to the execution context as single attributes
     */
    private void addContextEntriesAsAttributes(ScriptContext executionContext, Map<String, Object> scriptContext) {
        // add the context entries without their prefix to the scope
        for (Entry<String, Object> entry : scriptContext.entrySet()) {
            final String key = removePrefixIfPresent(entry.getKey());
            final Object value = entry.getValue();

            executionContext.setAttribute(key, value, ScriptContext.ENGINE_SCOPE);
        }
    }

    private String removePrefixIfPresent(final String key) {
        final int dotPosition = key.indexOf('.');
        return dotPosition != -1 ? key.substring(dotPosition + 1) : key;
    }

    private Map<String, Object> cloneContext(Map<String, Object> context) {
        return new HashMap<>(context);
    }
}
