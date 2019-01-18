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
package org.eclipse.smarthome.automation.module.script.internal;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.module.script.ScriptEngineContainer;
import org.eclipse.smarthome.automation.module.script.ScriptEngineFactory;
import org.eclipse.smarthome.automation.module.script.ScriptEngineManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ScriptManager allows to load and unloading of script files using a script engines script type
 *
 * @author Simon Merschjohann
 *
 */
@NonNullByDefault
@Component(service = ScriptEngineManager.class)
public class ScriptEngineManagerImpl implements ScriptEngineManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Set<ScriptEngineFactory> scriptEngineFactories = new HashSet<>();
    private HashMap<String, @Nullable ScriptEngineContainer> loadedScriptEngineInstances = new HashMap<>();
    private HashMap<String, @Nullable ScriptEngineFactory> supportedLanguages = new HashMap<>();
    private GenericScriptEngineFactory genericScriptEngineFactory = new GenericScriptEngineFactory();

    private @NonNullByDefault({}) ScriptExtensionManager scriptExtensionManager;

    @Reference
    public void setScriptExtensionManager(ScriptExtensionManager scriptExtensionManager) {
        this.scriptExtensionManager = scriptExtensionManager;
    }

    public void unsetScriptExtensionManager(ScriptExtensionManager scriptExtensionManager) {
        this.scriptExtensionManager = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addScriptEngineFactory(ScriptEngineFactory provider) {
        this.scriptEngineFactories.add(provider);

        for (String language : provider.getLanguages()) {
            this.supportedLanguages.put(language, provider);
        }
    }

    public void removeScriptEngineFactory(ScriptEngineFactory provider) {
        this.scriptEngineFactories.remove(provider);

        for (String language : provider.getLanguages()) {
            this.supportedLanguages.remove(language, provider);
        }
    }

    @Override
    public boolean isSupported(String fileExtension) {
        return findEngineFactory(fileExtension) != null;
    }

    @Override
    public @Nullable ScriptEngineContainer createScriptEngine(String fileExtension, String scriptIdentifier) {
        ScriptEngineContainer result = null;
        ScriptEngineFactory engineProvider = findEngineFactory(fileExtension);

        if (engineProvider == null) {
            logger.error("loadScript(): scriptengine for language '{}' could not be found for identifier: {}",
                    fileExtension, scriptIdentifier);
        } else {
            try {
                ScriptEngine engine = engineProvider.createScriptEngine(fileExtension);
                HashMap<String, Object> scriptExManager = new HashMap<>();
                result = new ScriptEngineContainer(engine, engineProvider, scriptIdentifier);
                ScriptExtensionManagerWrapper wrapper = new ScriptExtensionManagerWrapper(scriptExtensionManager,
                        result);
                scriptExManager.put("scriptExtension", wrapper);
                scriptExManager.put("se", wrapper);
                engineProvider.scopeValues(engine, scriptExManager);
                scriptExtensionManager.importDefaultPresets(engineProvider, engine, scriptIdentifier);

                loadedScriptEngineInstances.put(scriptIdentifier, result);
            } catch (Exception ex) {
                logger.error("Error while creating ScriptEngine", ex);
                removeScriptExtensions(scriptIdentifier);
            }
        }

        return result;
    }

    @Override
    public void loadScript(String scriptIdentifier, InputStreamReader scriptData) {
        ScriptEngineContainer container = loadedScriptEngineInstances.get(scriptIdentifier);

        if (container == null) {
            logger.error("could not load script as no engine is created");
        } else {
            ScriptEngine engine = container.getScriptEngine();
            try {
                engine.eval(scriptData);

                if (engine instanceof Invocable) {
                    Invocable inv = (Invocable) engine;
                    try {
                        inv.invokeFunction("scriptLoaded", scriptIdentifier);
                    } catch (NoSuchMethodException e) {
                        logger.trace("scriptLoaded() not defined in script: {}", scriptIdentifier);
                    }
                } else {
                    logger.trace("engine does not support Invocable interface");
                }
            } catch (Exception ex) {
                logger.error("Error during evaluation of script '{}': {}", scriptIdentifier, ex.getMessage());
            }
        }
    }

    @Override
    public void removeEngine(String scriptIdentifier) {
        ScriptEngineContainer container = loadedScriptEngineInstances.get(scriptIdentifier);

        if (container != null) {
            if (container.getScriptEngine() instanceof Invocable) {
                Invocable inv = (Invocable) container.getScriptEngine();
                try {
                    inv.invokeFunction("scriptUnloaded");
                } catch (NoSuchMethodException e) {
                    logger.trace("scriptUnloaded() not defined in script");
                } catch (ScriptException e) {
                    logger.error("Error while executing script", e);
                }
            } else {
                logger.trace("engine does not support Invocable interface");
            }

            removeScriptExtensions(scriptIdentifier);
        }
    }

    private void removeScriptExtensions(String pathIdentifier) {
        try {
            scriptExtensionManager.dispose(pathIdentifier);
        } catch (Exception ex) {
            logger.error("error removing engine", ex);
        }
    }

    private @Nullable ScriptEngineFactory findEngineFactory(String fileExtension) {
        ScriptEngineFactory engineProvider = supportedLanguages.get(fileExtension);

        if (engineProvider != null) {
            return engineProvider;
        }

        for (ScriptEngineFactory provider : supportedLanguages.values()) {
            if (provider != null && provider.isSupported(fileExtension)) {
                return provider;
            }
        }

        if (genericScriptEngineFactory.isSupported(fileExtension)) {
            return genericScriptEngineFactory;
        }

        return null;
    }

}
