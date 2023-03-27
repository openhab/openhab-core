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

import static org.openhab.core.automation.module.script.profile.ScriptProfileFactory.PROFILE_CONFIG_URI_PREFIX;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.internal.ScriptEngineFactoryHelper;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
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
@Component(immediate = true, service = { ScriptTransformationServiceFactory.class, ConfigDescriptionProvider.class })
@NonNullByDefault
public class ScriptTransformationServiceFactory implements ConfigDescriptionProvider {
    private static final URI CONFIG_DESCRIPTION_TEMPLATE_URI = URI.create(PROFILE_CONFIG_URI_PREFIX + "SCRIPT");

    private final ConfigDescriptionRegistry configDescRegistry;
    private final ComponentFactory<ScriptTransformationService> scriptTransformationFactory;

    private final Map<String, ComponentInstance<ScriptTransformationService>> scriptTransformations = new ConcurrentHashMap<>();

    @Activate
    public ScriptTransformationServiceFactory(@Reference ConfigDescriptionRegistry configDescRegistry,
            @Reference(target = "(component.factory=org.openhab.core.automation.module.script.transformation.factory)") ComponentFactory<ScriptTransformationService> factory) {
        this.configDescRegistry = configDescRegistry;
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

        scriptTransformations.computeIfAbsent(scriptType.get(), type -> {
            ScriptEngine scriptEngine = engineFactory.createScriptEngine(type);
            if (scriptEngine == null) {
                return null;
            }
            String languageName = ScriptEngineFactoryHelper.getLanguageName(scriptEngine.getFactory());
            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put(TransformationService.SERVICE_PROPERTY_NAME, type.toUpperCase());
            properties.put(TransformationService.SERVICE_PROPERTY_LABEL, languageName);
            properties.put(ScriptTransformationService.SCRIPT_TYPE_PROPERTY_NAME, type);
            return scriptTransformationFactory.newInstance(properties);
        });
    }

    public void unsetScriptEngineFactory(ScriptEngineFactory engineFactory) {
        Optional<String> scriptType = ScriptEngineFactoryHelper.getPreferredExtension(engineFactory);
        if (scriptType.isEmpty()) {
            return;
        }

        ComponentInstance<ScriptTransformationService> toBeUnregistered = scriptTransformations
                .remove(scriptType.get());
        if (toBeUnregistered != null) {
            unregisterService(toBeUnregistered);
        }
    }

    private void unregisterService(ComponentInstance<ScriptTransformationService> instance) {
        instance.getInstance().deactivate();
        instance.dispose();
    }

    @Override
    public Collection<ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
        ConfigDescription template = configDescRegistry.getConfigDescription(CONFIG_DESCRIPTION_TEMPLATE_URI, locale);
        if (template == null) {
            return Collections.emptyList();
        }

        return scriptTransformations.keySet().stream()
                .map(type -> ConfigDescriptionBuilder.create(URI.create(PROFILE_CONFIG_URI_PREFIX + type.toUpperCase()))
                        .withParameters(template.getParameters()).withParameterGroups(template.getParameterGroups())
                        .build())
                .toList();
    }

    @Override
    public @Nullable ConfigDescription getConfigDescription(URI uri, @Nullable Locale locale) {
        String uriString = uri.toString();
        if (!uriString.startsWith(PROFILE_CONFIG_URI_PREFIX)) {
            return null;
        }
        String scriptType = uriString.substring(PROFILE_CONFIG_URI_PREFIX.length());
        if (!scriptTransformations.containsKey(scriptType.toLowerCase())) {
            return null;
        }

        ConfigDescription template = configDescRegistry.getConfigDescription(CONFIG_DESCRIPTION_TEMPLATE_URI, locale);
        if (template == null) {
            return null;
        }
        return ConfigDescriptionBuilder.create(uri).withParameters(template.getParameters())
                .withParameterGroups(template.getParameterGroups()).build();
    }
}
