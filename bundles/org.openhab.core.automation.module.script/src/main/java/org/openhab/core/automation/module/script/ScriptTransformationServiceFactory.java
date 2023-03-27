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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.internal.ScriptEngineFactoryHelper;
import org.openhab.core.automation.module.script.profile.ScriptProfile;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionProvider;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.transform.TransformationRegistry;
import org.openhab.core.transform.TransformationService;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
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
@Component(immediate = true, service = { ConfigDescriptionProvider.class, ConfigOptionProvider.class })
@NonNullByDefault
public class ScriptTransformationServiceFactory implements ConfigDescriptionProvider, ConfigOptionProvider {
    private final Logger logger = LoggerFactory.getLogger(ScriptTransformationServiceFactory.class);

    public static final String PROFILE_CONFIG_URI_PREFIX = "profile:transform:";
    private static final URI CONFIG_DESCRIPTION_TEMPLATE_URI = URI.create(PROFILE_CONFIG_URI_PREFIX + "SCRIPT");

    private final ConfigDescriptionRegistry configDescRegistry;
    private final ComponentFactory<ScriptTransformationService> scriptTransformationFactory;
    private final TransformationRegistry transformationRegistry;

    private final Map<String, TransformationRecord> scriptTransformations = new ConcurrentHashMap<>();

    @Activate
    public ScriptTransformationServiceFactory(@Reference ConfigDescriptionRegistry configDescRegistry,
            @Reference TransformationRegistry transformationRegistry,
            @Reference(target = "(component.factory=org.openhab.core.automation.module.script.transformation.factory)") ComponentFactory<ScriptTransformationService> factory) {
        this.configDescRegistry = configDescRegistry;
        this.scriptTransformationFactory = factory;
        this.transformationRegistry = transformationRegistry;
    }

    @Deactivate
    public void deactivate() {
        scriptTransformations.values().stream().map(tr -> tr.instance()).forEach(this::unregisterService);
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

            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put(TransformationService.SERVICE_PROPERTY_NAME, type.toUpperCase());
            ComponentInstance<ScriptTransformationService> instance = scriptTransformationFactory
                    .newInstance(properties);

            return new TransformationRecord(instance, getLanguageName(type, engineFactory));
        });
    }

    public void unsetScriptEngineFactory(ScriptEngineFactory engineFactory) {
        Optional<String> scriptType = ScriptEngineFactoryHelper.getPreferredExtension(engineFactory);
        if (scriptType.isPresent()) {
            Optional.ofNullable(scriptTransformations.remove(scriptType.get()))
                    .ifPresent(tr -> unregisterService(tr.instance()));
        }
    }

    public Stream<String> getScriptTypes() {
        return scriptTransformations.keySet().stream();
    }

    private String getLanguageName(final String scriptType, ScriptEngineFactory engineFactory) {
        var scriptEngine = engineFactory.createScriptEngine(scriptType);
        String languageName = ScriptEngineFactoryHelper.getLanguageName(scriptEngine.getFactory());
        return languageName;
    }

    @Nullable
    public String getLanguageName(final String scriptType) {
        TransformationRecord tr = scriptTransformations.get(scriptType);
        if (tr != null) {
            return tr.languageName();
        }
        return null;
    }

    private void unregisterService(ComponentInstance<ScriptTransformationService> instance) {
        instance.getInstance().deactivate();
        instance.dispose();
    }

    @Nullable
    public ScriptTransformationService getTransformationService(String scriptType) {
        TransformationRecord rec = scriptTransformations.get(scriptType);
        if (rec == null) {
            return null;
        }
        return rec.instance().getInstance();
    }

    @Override
    public Collection<ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
        ConfigDescription template = configDescRegistry.getConfigDescription(CONFIG_DESCRIPTION_TEMPLATE_URI, locale);
        if (template == null) {
            return Collections.emptyList();
        }

        return getScriptTypes().map(type -> ConfigDescriptionBuilder
                .create(URI.create(PROFILE_CONFIG_URI_PREFIX + type.toUpperCase()))
                .withParameters(template.getParameters()).withParameterGroups(template.getParameterGroups()).build())
                .toList();
    }

    /**
     * Provides a {@link ConfigDescription} for the given URI.
     *
     * @param uri uri of the config description
     * @param locale locale
     * @return config description or null if no config description could be found
     */
    @Override
    public @Nullable ConfigDescription getConfigDescription(URI uri, @Nullable Locale locale) {
        String uriString = uri.toString();
        if (!uriString.startsWith(PROFILE_CONFIG_URI_PREFIX) || !getProfileConfigURIs().contains(uriString)) {
            return null;
        }

        ConfigDescription template = configDescRegistry.getConfigDescription(CONFIG_DESCRIPTION_TEMPLATE_URI, locale);
        if (template == null) {
            return null;
        }
        return ConfigDescriptionBuilder.create(uri).withParameters(template.getParameters())
                .withParameterGroups(template.getParameterGroups()).build();
    }

    private Set<String> getProfileConfigURIs() {
        return getScriptTypes().map(type -> PROFILE_CONFIG_URI_PREFIX + type.toUpperCase()).collect(Collectors.toSet());
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        String uriString = uri.toString();
        if (!uriString.startsWith(PROFILE_CONFIG_URI_PREFIX) || !getProfileConfigURIs().contains(uriString)) {
            return null;
        }

        String[] uri_parts = uri.toString().split(":");
        if (uri_parts.length == 0) {
            return null;
        }

        String scriptType = uri_parts[uri_parts.length - 1].toLowerCase();

        if (ScriptProfile.CONFIG_TO_HANDLER_SCRIPT.equals(param) || ScriptProfile.CONFIG_TO_ITEM_SCRIPT.equals(param)) {
            var scriptTypes = scriptType.equals("dsl") ? List.of(scriptType, "rules") : List.of(scriptType);
            return transformationRegistry.getTransformations(scriptTypes).stream()
                    .map(c -> new ParameterOption(c.getUID(), c.getLabel())).collect(Collectors.toList());
        }
        return null;
    }

    private record TransformationRecord(ComponentInstance<ScriptTransformationService> instance, String languageName) {
    }
}
