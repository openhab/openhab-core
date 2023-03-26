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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.internal.ScriptEngineFactoryHelper;
import org.openhab.core.transform.TransformationRegistry;
import org.openhab.core.transform.TransformationService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ScriptTransformationServiceFactory} registers a {@link ScriptTransformationService}
 * for each newly added script engine.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@Component(immediate = true, service = ScriptTransformationServiceFactory.class)
@NonNullByDefault
public class ScriptTransformationServiceFactory {
    private final Logger logger = LoggerFactory.getLogger(ScriptTransformationServiceFactory.class);

    private final TransformationRegistry transformationRegistry;
    private final ScriptEngineManager scriptEngineManager;
    private final BundleContext bundleContext;

    private final Map<String, ServiceRegistration> scriptTransformations = new ConcurrentHashMap<>();

    @Activate
    public ScriptTransformationServiceFactory(@Reference TransformationRegistry transformationRegistry,
            @Reference ScriptEngineManager scriptEngineManager, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.transformationRegistry = transformationRegistry;
        this.scriptEngineManager = scriptEngineManager;
    }

    @Deactivate
    public void deactivate() {
        scriptTransformations.values().forEach(this::unregisterService);
    }

    /**
     * As {@link ScriptEngineFactory}s are added/removed, this method will cache all available script types
     * and registers a transformation service for the script engine.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void setScriptEngineFactory(ScriptEngineFactory engineFactory) {
        Optional<String> scriptType = ScriptEngineFactoryHelper.getPreferredExtension(engineFactory);
        if (!scriptType.isPresent()) {
            return;
        }

        scriptTransformations.computeIfAbsent(scriptType.get(), type -> {
            ScriptTransformationService scriptTransformation = new ScriptTransformationService(type,
                    transformationRegistry, scriptEngineManager);

            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put(TransformationService.SERVICE_PROPERTY_NAME, type.toUpperCase());
            return bundleContext.registerService(TransformationService.class, scriptTransformation, properties);
        });
    }

    public void unsetScriptEngineFactory(ScriptEngineFactory engineFactory) {
        Optional<String> scriptType = ScriptEngineFactoryHelper.getPreferredExtension(engineFactory);
        if (scriptType.isPresent()) {
            Optional.ofNullable(scriptTransformations.remove(scriptType.get())).ifPresent(this::unregisterService);
        }
    }

    public Set<String> getScriptTypes() {
        return scriptTransformations.keySet();
    }

    @Nullable
    public ScriptTransformationService getTransformationService(final String scriptType) {
        ServiceRegistration reg = scriptTransformations.get(scriptType);
        if (reg != null) {
            return (ScriptTransformationService) bundleContext.getService(reg.getReference());
        }
        return null;
    }

    private void unregisterService(ServiceRegistration reg) {
        ScriptTransformationService scriptTransformation = (ScriptTransformationService) bundleContext
                .getService(reg.getReference());
        scriptTransformation.deactivate();
        reg.unregister();
    }
}
