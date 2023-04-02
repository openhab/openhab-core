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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.module.script.internal.ScriptEngineFactoryHelper;
import org.openhab.core.transform.TransformationService;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link ScriptTransformationServiceFactory} registers a {@link ScriptTransformationService}
 * for each newly added script engine.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@Component(immediate = true, service = { ScriptTransformationServiceFactory.class })
@NonNullByDefault
public class ScriptTransformationServiceFactory {

    private final ComponentFactory<ScriptTransformationService> scriptTransformationFactory;

    private final Map<ScriptEngineFactory, ComponentInstance<ScriptTransformationService>> scriptTransformations = new ConcurrentHashMap<>();

    @Activate
    public ScriptTransformationServiceFactory(
            @Reference(target = "(component.factory=org.openhab.core.automation.module.script.transformation.factory)") ComponentFactory<ScriptTransformationService> factory) {
        this.scriptTransformationFactory = factory;
    }

    @Deactivate
    public void deactivate() {
        scriptTransformations.values().forEach(this::unregisterService);
        scriptTransformations.clear();
    }

    /**
     * As {@link ScriptEngineFactory}s are added/removed, this method will cache all available script types
     * and registers a transformation service for the script engine.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void setScriptEngineFactory(ScriptEngineFactory engineFactory) {
        Optional<String> scriptType = ScriptEngineFactoryHelper.getPreferredExtension(engineFactory);
        if (scriptType.isEmpty()) {
            return;
        }

        scriptTransformations.computeIfAbsent(engineFactory, factory -> {
            ScriptEngine scriptEngine = engineFactory.createScriptEngine(scriptType.get());
            if (scriptEngine == null) {
                return null;
            }
            String languageName = ScriptEngineFactoryHelper.getLanguageName(scriptEngine.getFactory());
            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put(TransformationService.SERVICE_PROPERTY_NAME, scriptType.get().toUpperCase());
            properties.put(TransformationService.SERVICE_PROPERTY_LABEL, "SCRIPT " + languageName);
            properties.put(ScriptTransformationService.SCRIPT_TYPE_PROPERTY_NAME, scriptType.get());
            return scriptTransformationFactory.newInstance(properties);
        });
    }

    public void unsetScriptEngineFactory(ScriptEngineFactory engineFactory) {
        ComponentInstance<ScriptTransformationService> toBeUnregistered = scriptTransformations.remove(engineFactory);
        if (toBeUnregistered != null) {
            unregisterService(toBeUnregistered);
        }
    }

    private void unregisterService(ComponentInstance<ScriptTransformationService> instance) {
        instance.getInstance().deactivate();
        instance.dispose();
    }
}
