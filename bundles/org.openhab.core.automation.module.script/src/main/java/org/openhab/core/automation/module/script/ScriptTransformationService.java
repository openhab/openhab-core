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

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.transform.TransformationConfiguration;
import org.openhab.core.transform.TransformationConfigurationRegistry;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

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
    }

    @Override
    public @Nullable String transform(String function, String source) throws TransformationException {
        int splitPoint = function.indexOf(":");
        if (splitPoint < 1) {
            throw new TransformationException("Script Type must be prepended to transformation UID.");
        }
        String scriptType = function.substring(0, splitPoint);
        String scriptUid = function.substring(splitPoint + 1);

        String script = scriptCache.get(scriptUid);
        if (script == null) {
            TransformationConfiguration transformationConfiguration = transformationConfigurationRegistry
                    .get(scriptUid);
            if (transformationConfiguration != null) {
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
        if (scriptEngineContainer != null) {
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
                executionContext.setAttribute("inputString", source, ScriptContext.ENGINE_SCOPE);

                Object result = compiledScript != null ? compiledScript.eval() : engine.eval(script);
                return result == null ? null : result.toString();
            } catch (ScriptException e) {
                throw new TransformationException("Failed to execute script.", e);
            }
        }

        return null;
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
        compiledScripts.remove(uid);
        scriptEngineContainers.remove(uid);
        scriptCache.remove(uid);
    }
}
