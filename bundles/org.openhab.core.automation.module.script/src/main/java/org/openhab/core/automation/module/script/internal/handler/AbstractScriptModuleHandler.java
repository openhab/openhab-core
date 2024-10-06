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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

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
 * @author Florian Hotze - Add support for script pre-compilation
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
    private Optional<CompiledScript> compiledScript = Optional.empty();
    private final String type;
    protected final String script;

    protected final String ruleUID;

    protected AbstractScriptModuleHandler(T module, String ruleUID, ScriptEngineManager scriptEngineManager) {
        super(module);
        this.scriptEngineManager = scriptEngineManager;
        this.ruleUID = ruleUID;
        this.engineIdentifier = UUID.randomUUID().toString();

        this.type = getValidConfigParameter(SCRIPT_TYPE, module.getConfiguration(), module.getId(), false);
        this.script = getValidConfigParameter(SCRIPT, module.getConfiguration(), module.getId(), true);
    }

    private static String getValidConfigParameter(String parameter, Configuration config, String moduleId,
            boolean emptyAllowed) {
        Object value = config.get(parameter);
        if (value instanceof String string && (emptyAllowed || !string.trim().isEmpty())) {
            return string;
        } else {
            throw new IllegalStateException(String.format(
                    "Config parameter '%s' is missing in the configuration of module '%s'.", parameter, moduleId));
        }
    }

    /**
     * Creates the {@link ScriptEngine} and compiles the script if the {@link ScriptEngine} implements
     * {@link Compilable}.
     */
    protected void compileScript() throws ScriptException {
        if (compiledScript.isPresent()) {
            return;
        }
        if (!scriptEngineManager.isSupported(this.type)) {
            logger.debug(
                    "ScriptEngine for language '{}' could not be found, skipping compilation of script for identifier: {}",
                    type, engineIdentifier);
            return;
        }
        Optional<ScriptEngine> engine = getScriptEngine();
        if (engine.isPresent()) {
            ScriptEngine scriptEngine = engine.get();
            if (scriptEngine instanceof Compilable) {
                logger.debug("Pre-compiling script of rule with UID '{}'", ruleUID);
                compiledScript = Optional.ofNullable(((Compilable) scriptEngine).compile(script));
            }
        }
    }

    @Override
    public void dispose() {
        scriptEngineManager.removeEngine(engineIdentifier);
    }

    /**
     * Reset the script engine to force a script reload
     *
     */
    public synchronized void resetScriptEngine() {
        scriptEngineManager.removeEngine(engineIdentifier);
        scriptEngine = Optional.empty();
    }

    /**
     * Gets the script engine identifier for this module
     *
     * @return the engine identifier string
     */
    public String getEngineIdentifier() {
        return engineIdentifier;
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
     * Adds the passed context variables of the rule engine to the context scope of the ScriptEngine
     * this should be done each time the module is executed to prevent leaking context to later executions
     *
     * @param engine the script engine that is used
     * @param context the variables and types to remove from the execution context
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

    /**
     * Removes passed context variables of the rule engine from the context scope of the ScriptEngine, this should be
     * updated each time the module is executed
     *
     * @param engine the script engine that is used
     * @param context the variables and types to put into the execution context
     */
    protected void resetExecutionContext(ScriptEngine engine, Map<String, ?> context) {
        ScriptContext executionContext = engine.getContext();

        for (Entry<String, ?> entry : context.entrySet()) {
            String key = entry.getKey();
            int dotIndex = key.indexOf('.');
            if (dotIndex != -1) {
                key = key.substring(dotIndex + 1);
            }
            executionContext.removeAttribute(key, ScriptContext.ENGINE_SCOPE);
        }
    }

    /**
     * Evaluates the passed script with the ScriptEngine.
     *
     * @param engine the script engine that is used
     * @param script the script to evaluate
     * @return the value returned from the execution of the script
     */
    protected @Nullable Object eval(ScriptEngine engine, String script) {
        try {
            if (compiledScript.isPresent()) {
                if (engine instanceof Lock lock && !lock.tryLock(1, TimeUnit.MINUTES)) {
                    logger.error("Failed to acquire lock within one minute for script module of rule with UID '{}'",
                            ruleUID);
                    return null;
                }
                logger.debug("Executing pre-compiled script of rule with UID '{}'", ruleUID);
                try {
                    return compiledScript.get().eval(engine.getContext());
                } finally { // Make sure that Lock is unlocked regardless of an exception being thrown or not to avoid
                            // deadlocks
                    if (engine instanceof Lock lock) {
                        lock.unlock();
                    }
                }
            }
            logger.debug("Executing script of rule with UID '{}'", ruleUID);
            return engine.eval(script);
        } catch (ScriptException e) {
            logger.error("Script execution of rule with UID '{}' failed: {}", ruleUID, e.getMessage(),
                    logger.isDebugEnabled() ? e : null);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
