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
package org.openhab.core.automation.module.script.internal;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 * @author Scott Rushworth - replaced GenericScriptEngineFactory with a service and cleaned up logging
 */
@NonNullByDefault
@Component(service = ScriptEngineManager.class)
public class ScriptEngineManagerImpl implements ScriptEngineManager {

    private final Logger logger = LoggerFactory.getLogger(ScriptEngineManagerImpl.class);
    private final Map<String, ScriptEngineContainer> loadedScriptEngineInstances = new HashMap<>();
    private final Map<String, ScriptEngineFactory> customSupport = new HashMap<>();
    private final Map<String, ScriptEngineFactory> genericSupport = new HashMap<>();
    private final ScriptExtensionManager scriptExtensionManager;

    @Activate
    public ScriptEngineManagerImpl(final @Reference ScriptExtensionManager scriptExtensionManager) {
        this.scriptExtensionManager = scriptExtensionManager;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addScriptEngineFactory(ScriptEngineFactory engineFactory) {
        List<String> scriptTypes = engineFactory.getScriptTypes();
        logger.trace("{}.getScriptTypes(): {}", engineFactory.getClass().getSimpleName(), scriptTypes);
        for (String scriptType : scriptTypes) {
            if (isCustomFactory(engineFactory)) {
                this.customSupport.put(scriptType, engineFactory);
            } else {
                this.genericSupport.put(scriptType, engineFactory);
            }
        }
        if (logger.isDebugEnabled()) {
            if (!scriptTypes.isEmpty()) {
                ScriptEngine scriptEngine = engineFactory.createScriptEngine(scriptTypes.get(0));
                if (scriptEngine != null) {
                    javax.script.ScriptEngineFactory factory = scriptEngine.getFactory();
                    logger.debug(
                            "Initialized a {} ScriptEngineFactory for {} ({}): supports {} ({}) with file extensions {}, names {}, and mimetypes {}",
                            (isCustomFactory(engineFactory)) ? "custom" : "generic", factory.getEngineName(),
                            factory.getEngineVersion(), factory.getLanguageName(), factory.getLanguageVersion(),
                            factory.getExtensions(), factory.getNames(), factory.getMimeTypes());
                } else {
                    logger.trace("addScriptEngineFactory: engine was null");
                }
            } else {
                logger.trace("addScriptEngineFactory: scriptTypes was empty");
            }
        }
    }

    public void removeScriptEngineFactory(ScriptEngineFactory engineFactory) {
        List<String> scriptTypes = engineFactory.getScriptTypes();
        logger.trace("{}.getScriptTypes(): {}", engineFactory.getClass().getSimpleName(), scriptTypes);
        for (String scriptType : scriptTypes) {
            if (isCustomFactory(engineFactory)) {
                this.customSupport.remove(scriptType, engineFactory);
            } else {
                this.genericSupport.remove(scriptType, engineFactory);
            }
        }
        logger.debug("Removed {}", engineFactory.getClass().getSimpleName());
    }

    /**
     * This method is used to determine if a given {@link ScriptEngineFactory} is generic or customized.
     *
     * @param engineFactory {@link ScriptEngineFactory}
     * @return true, if the {@link ScriptEngineFactory} is custom, otherwise false
     */
    private boolean isCustomFactory(ScriptEngineFactory engineFactory) {
        return !(engineFactory instanceof GenericScriptEngineFactory);
    }

    @Override
    public @Nullable ScriptEngineContainer createScriptEngine(String scriptType, String engineIdentifier) {
        ScriptEngineContainer result = null;
        ScriptEngineFactory engineFactory = findEngineFactory(scriptType);
        if (engineFactory == null) {
            logger.error("ScriptEngine for language '{}' could not be found for identifier: {}", scriptType,
                    engineIdentifier);
        } else {
            try {
                ScriptEngine engine = engineFactory.createScriptEngine(scriptType);
                if (engine != null) {
                    Map<String, Object> scriptExManager = new HashMap<>();
                    result = new ScriptEngineContainer(engine, engineFactory, engineIdentifier);
                    ScriptExtensionManagerWrapper wrapper = new ScriptExtensionManagerWrapper(scriptExtensionManager,
                            result);
                    scriptExManager.put("scriptExtension", wrapper);
                    scriptExManager.put("se", wrapper);
                    engineFactory.scopeValues(engine, scriptExManager);
                    scriptExtensionManager.importDefaultPresets(engineFactory, engine, engineIdentifier);
                    loadedScriptEngineInstances.put(engineIdentifier, result);
                    logger.debug("Added ScriptEngine for language '{}' with identifier: {}", scriptType,
                            engineIdentifier);
                } else {
                    logger.error("ScriptEngine for language '{}' could not be created for identifier: {}", scriptType,
                            engineIdentifier);
                }
            } catch (Exception ex) {
                logger.error("Error while creating ScriptEngine", ex);
                removeScriptExtensions(engineIdentifier);
            }
        }
        return result;
    }

    @Override
    public void loadScript(String engineIdentifier, InputStreamReader scriptData) {
        ScriptEngineContainer container = loadedScriptEngineInstances.get(engineIdentifier);
        if (container == null) {
            logger.error("Could not load script, as no ScriptEngine has been created");
        } else {
            ScriptEngine engine = container.getScriptEngine();
            try {
                engine.eval(scriptData);
                if (engine instanceof Invocable) {
                    Invocable inv = (Invocable) engine;
                    try {
                        inv.invokeFunction("scriptLoaded", engineIdentifier);
                    } catch (NoSuchMethodException e) {
                        logger.trace("scriptLoaded() is not defined in the script: {}", engineIdentifier);
                    }
                } else {
                    logger.trace("ScriptEngine does not support Invocable interface");
                }
            } catch (Exception ex) {
                logger.error("Error during evaluation of script '{}': {}", engineIdentifier, ex.getMessage());
            }
        }
    }

    @Override
    public void removeEngine(String engineIdentifier) {
        ScriptEngineContainer container = loadedScriptEngineInstances.get(engineIdentifier);
        if (container != null) {
            if (container.getScriptEngine() instanceof Invocable) {
                Invocable inv = (Invocable) container.getScriptEngine();
                try {
                    inv.invokeFunction("scriptUnloaded");
                } catch (NoSuchMethodException e) {
                    logger.trace("scriptUnloaded() is not defined in the script");
                } catch (ScriptException ex) {
                    logger.error("Error while executing script", ex);
                }
            } else {
                logger.trace("ScriptEngine does not support Invocable interface");
            }
            removeScriptExtensions(engineIdentifier);
        }
    }

    private void removeScriptExtensions(String pathIdentifier) {
        try {
            scriptExtensionManager.dispose(pathIdentifier);
        } catch (Exception ex) {
            logger.error("Error removing ScriptEngine", ex);
        }
    }

    /**
     * This method will find and return a {@link ScriptEngineFactory} capable of executing a script of the given type,
     * if one exists. Custom ScriptEngineFactories are preferred over generic.
     *
     * @param scriptType a file extension (script) or MimeType (ScriptAction or ScriptCondition)
     * @return {@link ScriptEngineFactory} or null
     */
    private @Nullable ScriptEngineFactory findEngineFactory(String scriptType) {
        ScriptEngineFactory customFactory = customSupport.get(scriptType);
        if (customFactory != null) {
            return customFactory;
        }
        ScriptEngineFactory genericFactory = genericSupport.get(scriptType);
        if (genericFactory != null) {
            return genericFactory;
        }
        return null;
    }

    @Override
    public boolean isSupported(String scriptType) {
        return findEngineFactory(scriptType) != null;
    }
}
