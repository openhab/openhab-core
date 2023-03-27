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
package org.openhab.core.automation.module.script;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.transform.Transformation;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationRegistry;
import org.openhab.core.transform.TransformationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ScriptTransformationService} implements a {@link TransformationService} using any available script
 * language
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(factory = "org.openhab.core.automation.module.script.transformation.factory")
public class ScriptTransformationService implements TransformationService, RegistryChangeListener<Transformation> {
    public static final String SCRIPT_TYPE_PROPERTY_NAME = "openhab.transform.script.scriptType";
    public static final String OPENHAB_TRANSFORMATION_SCRIPT = "openhab-transformation-script-";

    private static final Pattern INLINE_SCRIPT_CONFIG_PATTERN = Pattern.compile("\\|(?<inlineScript>.+)");

    private static final Pattern SCRIPT_CONFIG_PATTERN = Pattern.compile("(?<scriptUid>.+?)(\\?(?<params>.*?))?");

    private final Logger logger = LoggerFactory.getLogger(ScriptTransformationService.class);

    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);

    private final String scriptType;

    private final Map<String, ScriptRecord> scriptCache = new ConcurrentHashMap<>();

    private final TransformationRegistry transformationRegistry;
    private final ScriptEngineManager scriptEngineManager;

    @Activate
    public ScriptTransformationService(@Reference TransformationRegistry transformationRegistry,
            @Reference ScriptEngineManager scriptEngineManager, Map<String, Object> config) {
        String scriptType = ConfigParser.valueAs(config.get(SCRIPT_TYPE_PROPERTY_NAME), String.class);
        if (scriptType == null) {
            throw new IllegalStateException(
                    "'" + SCRIPT_TYPE_PROPERTY_NAME + "' must not be null in service configuration");
        }

        this.transformationRegistry = transformationRegistry;
        this.scriptEngineManager = scriptEngineManager;
        this.scriptType = scriptType;
        transformationRegistry.addRegistryChangeListener(this);
    }

    @Deactivate
    public void deactivate() {
        transformationRegistry.removeRegistryChangeListener(this);

        // cleanup script engines
        scriptCache.values().forEach(this::disposeScriptRecord);
    }

    @Override
    public @Nullable String transform(String function, String source) throws TransformationException {
        String scriptUid;
        String inlineScript = null;
        String params = null;

        Matcher configMatcher = INLINE_SCRIPT_CONFIG_PATTERN.matcher(function);
        if (configMatcher.matches()) {
            inlineScript = configMatcher.group("inlineScript");
            // prefix with | to avoid clashing with a real filename
            scriptUid = "|" + Integer.toString(inlineScript.hashCode());
        } else {
            configMatcher = SCRIPT_CONFIG_PATTERN.matcher(function);
            if (!configMatcher.matches()) {
                throw new TransformationException("Invalid syntax for the script transformation: '" + function + "'");
            }
            scriptUid = configMatcher.group("scriptUid");
            params = configMatcher.group("params");
        }

        ScriptRecord scriptRecord = scriptCache.computeIfAbsent(scriptUid, k -> new ScriptRecord());
        scriptRecord.lock.lock();
        try {
            if (scriptRecord.script.isBlank()) {
                if (inlineScript != null) {
                    scriptRecord.script = inlineScript;
                } else {
                    // get script from transformation registry
                    Transformation transformation = transformationRegistry.get(scriptUid);
                    if (transformation != null) {
                        scriptRecord.script = transformation.getConfiguration().getOrDefault(Transformation.FUNCTION,
                                "");
                    }
                }
                if (scriptRecord.script.isBlank()) {
                    throw new TransformationException("Could not get script for UID '" + scriptUid + "'.");
                }
                scriptCache.put(scriptUid, scriptRecord);
            }

            if (!scriptEngineManager.isSupported(scriptType)) {
                // language has been removed, clear container and compiled scripts if found
                if (scriptRecord.scriptEngineContainer != null) {
                    scriptEngineManager.removeEngine(OPENHAB_TRANSFORMATION_SCRIPT + scriptUid);
                }
                clearCache(scriptUid);
                throw new TransformationException(
                        "Script type '" + scriptType + "' is not supported by any available script engine.");
            }

            if (scriptRecord.scriptEngineContainer == null) {
                scriptRecord.scriptEngineContainer = scriptEngineManager.createScriptEngine(scriptType,
                        OPENHAB_TRANSFORMATION_SCRIPT + scriptUid);
            }
            ScriptEngineContainer scriptEngineContainer = scriptRecord.scriptEngineContainer;

            if (scriptEngineContainer == null) {
                throw new TransformationException("Failed to create script engine container for '" + function + "'.");
            }
            try {
                CompiledScript compiledScript = scriptRecord.compiledScript;

                ScriptEngine engine = compiledScript != null ? compiledScript.getEngine()
                        : scriptEngineContainer.getScriptEngine();
                ScriptContext executionContext = engine.getContext();
                executionContext.setAttribute("input", source, ScriptContext.ENGINE_SCOPE);

                if (params != null) {
                    for (String param : params.split("&")) {
                        String[] splitString = param.split("=");
                        if (splitString.length != 2) {
                            logger.warn(
                                    "Parameter '{}' does not consist of two parts for configuration UID {}, skipping.",
                                    param, scriptUid);
                        } else {
                            param = URLDecoder.decode(splitString[0], StandardCharsets.UTF_8);
                            String value = URLDecoder.decode(splitString[1], StandardCharsets.UTF_8);
                            executionContext.setAttribute(param, value, ScriptContext.ENGINE_SCOPE);
                        }
                    }
                }

                // compile the script here _after_ setting context attributes, so that the script engine
                // can bind the attributes as variables during compilation. This primarily affects jruby.
                if (compiledScript == null
                        && scriptEngineContainer.getScriptEngine() instanceof Compilable scriptEngine) {
                    // no compiled script available but compiling is supported
                    compiledScript = scriptEngine.compile(scriptRecord.script);
                    scriptRecord.compiledScript = compiledScript;
                }

                Object result = compiledScript != null ? compiledScript.eval() : engine.eval(scriptRecord.script);
                return result == null ? null : result.toString();
            } catch (ScriptException e) {
                throw new TransformationException("Failed to execute script.", e);
            }
        } finally {
            scriptRecord.lock.unlock();
        }
    }

    @Override
    public void added(Transformation element) {
        clearCache(element.getUID());
    }

    @Override
    public void removed(Transformation element) {
        clearCache(element.getUID());
    }

    @Override
    public void updated(Transformation oldElement, Transformation element) {
        clearCache(element.getUID());
    }

    private void clearCache(String uid) {
        ScriptRecord scriptRecord = scriptCache.remove(uid);
        if (scriptRecord != null) {
            disposeScriptRecord(scriptRecord);
        }
    }

    private void disposeScriptRecord(ScriptRecord scriptRecord) {
        ScriptEngineContainer scriptEngineContainer = scriptRecord.scriptEngineContainer;
        if (scriptEngineContainer != null) {
            disposeScriptEngine(scriptEngineContainer.getScriptEngine());
        }
        CompiledScript compiledScript = scriptRecord.compiledScript;
        if (compiledScript != null) {
            disposeScriptEngine(compiledScript.getEngine());
        }
    }

    private void disposeScriptEngine(ScriptEngine scriptEngine) {
        if (scriptEngine instanceof AutoCloseable closableScriptEngine) {
            // we cannot not use ScheduledExecutorService.execute here as it might execute the task in the calling
            // thread (calling ScriptEngine.close in the same thread may result in a deadlock if the ScriptEngine
            // tries to Thread.join)
            scheduler.schedule(() -> {
                try {
                    closableScriptEngine.close();
                } catch (Exception e) {
                    logger.error("Error while closing script engine", e);
                }
            }, 0, TimeUnit.SECONDS);
        } else {
            logger.trace("ScriptEngine does not support AutoCloseable interface");
        }
    }

    private static class ScriptRecord {
        public String script = "";
        public @Nullable ScriptEngineContainer scriptEngineContainer;
        public @Nullable CompiledScript compiledScript;

        public final Lock lock = new ReentrantLock();
    }
}
