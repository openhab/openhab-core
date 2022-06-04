/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.openhab.core.transform.TransformationConfiguration;
import org.openhab.core.transform.TransformationConfigurationRegistry;
import org.openhab.core.transform.TransformationException;
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
@Component(service = TransformationService.class, property = { "openhab.transform=SCRIPT" })
@NonNullByDefault
public class ScriptTransformationService
        implements TransformationService, RegistryChangeListener<TransformationConfiguration> {
    public static final String OPENHAB_TRANSFORMATION_SCRIPT = "openhab-transformation-script-";
    public static final String SUPPORTED_CONFIGURATION_TYPE = "script";

    private static final Pattern SCRIPT_CONFIG_PATTERN = Pattern
            .compile("(?<scriptType>.*?):(?<scriptUid>.*?)(\\?(?<params>.*?))?");

    private final Logger logger = LoggerFactory.getLogger(ScriptTransformationService.class);

    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);

    private final Map<String, ScriptEngineContainer> scriptEngineContainers = new HashMap<>();
    private final Map<String, CompiledScript> compiledScripts = new HashMap<>();
    private final Map<String, String> scriptCache = new HashMap<>();

    private final TransformationConfigurationRegistry transformationConfigurationRegistry;
    private final ScriptEngineManager scriptEngineManager;

    @Activate
    public ScriptTransformationService(
            @Reference TransformationConfigurationRegistry transformationConfigurationRegistry,
            @Reference ScriptEngineManager scriptEngineManager) {
        this.transformationConfigurationRegistry = transformationConfigurationRegistry;
        this.scriptEngineManager = scriptEngineManager;
        transformationConfigurationRegistry.addRegistryChangeListener(this);
    }

    @Deactivate
    public void deactivate() {
        transformationConfigurationRegistry.removeRegistryChangeListener(this);

        // cleanup script engines
        scriptEngineContainers.values().stream().map(ScriptEngineContainer::getScriptEngine)
                .forEach(this::disposeScriptEngine);
        compiledScripts.values().stream().map(CompiledScript::getEngine).forEach(this::disposeScriptEngine);
    }

    @Override
    public @Nullable String transform(String function, String source) throws TransformationException {
        Matcher configMatcher = SCRIPT_CONFIG_PATTERN.matcher(function);
        if (!configMatcher.matches()) {
            throw new TransformationException("Script Type must be prepended to transformation UID.");
        }
        String scriptType = configMatcher.group("scriptType");
        String scriptUid = configMatcher.group("scriptUid");

        String script = scriptCache.get(scriptUid);
        if (script == null) {
            TransformationConfiguration transformationConfiguration = transformationConfigurationRegistry
                    .get(scriptUid);
            if (transformationConfiguration != null) {
                if (!SUPPORTED_CONFIGURATION_TYPE.equals(transformationConfiguration.getType())) {
                    throw new TransformationException("Configuration does not have correct type 'script' but '"
                            + transformationConfiguration.getType() + "'.");
                }
                script = transformationConfiguration.getContent();
            }
            if (script == null) {
                throw new TransformationException("Could not get script for UID '" + scriptUid + "'.");
            }
            scriptCache.put(scriptUid, script);
        }

        if (!scriptEngineManager.isSupported(scriptType)) {
            // language has been removed, clear container and compiled scripts if found
            if (scriptEngineContainers.containsKey(scriptUid)) {
                scriptEngineManager.removeEngine(OPENHAB_TRANSFORMATION_SCRIPT + scriptUid);
            }
            clearCache(scriptUid);
            throw new TransformationException(
                    "Script type '" + scriptType + "' is not supported by any available script engine.");
        }

        ScriptEngineContainer scriptEngineContainer = scriptEngineContainers.computeIfAbsent(scriptUid,
                k -> scriptEngineManager.createScriptEngine(scriptType, OPENHAB_TRANSFORMATION_SCRIPT + k));

        if (scriptEngineContainer == null) {
            throw new TransformationException("Failed to create script engine container for '" + function + "'.");
        }
        try {
            CompiledScript compiledScript = this.compiledScripts.get(scriptUid);

            if (compiledScript == null && scriptEngineContainer.getScriptEngine() instanceof Compilable) {
                // no compiled script available but compiling is supported
                compiledScript = ((Compilable) scriptEngineContainer.getScriptEngine()).compile(script);
                this.compiledScripts.put(scriptUid, compiledScript);
            }

            ScriptEngine engine = compiledScript != null ? compiledScript.getEngine()
                    : scriptEngineContainer.getScriptEngine();
            ScriptContext executionContext = engine.getContext();
            executionContext.setAttribute("input", source, ScriptContext.ENGINE_SCOPE);

            String params = configMatcher.group("params");
            if (params != null) {
                for (String param : params.split("&")) {
                    String[] splitString = param.split("=");
                    if (splitString.length != 2) {
                        logger.warn("Parameter '{}' does not consist of two parts for configuration UID {}, skipping.",
                                param, scriptUid);
                    } else {
                        executionContext.setAttribute(splitString[0], splitString[1], ScriptContext.ENGINE_SCOPE);
                    }
                }
            }

            Object result = compiledScript != null ? compiledScript.eval() : engine.eval(script);
            return result == null ? null : result.toString();
        } catch (ScriptException e) {
            throw new TransformationException("Failed to execute script.", e);
        }
    }

    @Override
    public void added(TransformationConfiguration element) {
        clearCache(element.getUID());
    }

    @Override
    public void removed(TransformationConfiguration element) {
        clearCache(element.getUID());
    }

    @Override
    public void updated(TransformationConfiguration oldElement, TransformationConfiguration element) {
        clearCache(element.getUID());
    }

    private void clearCache(String uid) {
        CompiledScript compiledScript = compiledScripts.remove(uid);
        if (compiledScript != null) {
            disposeScriptEngine(compiledScript.getEngine());
        }
        ScriptEngineContainer container = scriptEngineContainers.remove(uid);
        if (container != null) {
            disposeScriptEngine(container.getScriptEngine());
        }
        scriptCache.remove(uid);
    }

    private void disposeScriptEngine(ScriptEngine scriptEngine) {
        if (scriptEngine instanceof AutoCloseable) {
            // we cannot not use ScheduledExecutorService.execute here as it might execute the task in the calling
            // thread (calling ScriptEngine.close in the same thread may result in a deadlock if the ScriptEngine
            // tries to Thread.join)
            scheduler.schedule(() -> {
                AutoCloseable closeable = (AutoCloseable) scriptEngine;
                try {
                    closeable.close();
                } catch (Exception e) {
                    logger.error("Error while closing script engine", e);
                }
            }, 0, TimeUnit.SECONDS);
        } else {
            logger.trace("ScriptEngine does not support AutoCloseable interface");
        }
    }
}
