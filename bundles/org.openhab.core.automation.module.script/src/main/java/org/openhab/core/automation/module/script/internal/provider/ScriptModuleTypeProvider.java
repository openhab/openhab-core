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
package org.openhab.core.automation.module.script.internal.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.openhab.core.automation.module.script.internal.handler.AbstractScriptModuleHandler;
import org.openhab.core.automation.module.script.internal.handler.ScriptActionHandler;
import org.openhab.core.automation.module.script.internal.handler.ScriptConditionHandler;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.automation.type.Output;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.ParameterOption;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class dynamically provides ScriptActionType and ScriptConditionType {@link ModuleType}s. This class is necessary
 * because there is no other way to provide dynamic {@link ParameterOption}s for {@link ModuleType}s.
 *
 * @author Scott Rushworth - Initial contribution
 */
@NonNullByDefault
@Component
public class ScriptModuleTypeProvider implements ModuleTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(ScriptModuleTypeProvider.class);
    private final Map<String, String> parameterOptions = new TreeMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable ModuleType getModuleType(String UID, @Nullable Locale locale) {
        if (ScriptActionHandler.TYPE_ID.equals(UID)) {
            return getScriptActionType(locale);
        } else if (ScriptConditionHandler.TYPE_ID.equals(UID)) {
            return getScriptConditionType(locale);
        } else {
            return null;
        }
    }

    private @Nullable ModuleType getScriptActionType(@Nullable Locale locale) {
        if (parameterOptions.isEmpty()) {
            return null;
        } else {
            List<Output> outputs = new ArrayList<>();
            Output result = new Output("result", "java.lang.Object", "result", "the script result", null, null, null);
            outputs.add(result);
            return new ActionType(ScriptActionHandler.TYPE_ID, getConfigDescriptions(locale), "execute a given script",
                    "Allows the execution of a user-defined script.", null, Visibility.VISIBLE, null, outputs);
        }
    }

    private @Nullable ModuleType getScriptConditionType(@Nullable Locale locale) {
        if (parameterOptions.isEmpty()) {
            return null;
        } else {
            return new ConditionType(ScriptConditionHandler.TYPE_ID, getConfigDescriptions(locale),
                    "a given script evaluates to true", "Allows the definition of a condition through a script.", null,
                    Visibility.VISIBLE, null);
        }
    }

    /**
     * This method creates the {@link ConfigurationDescriptionParameter}s used by the generated ScriptActionType and
     * ScriptConditionType. {@link AbstractScriptModuleHandler} requires that the names of these be 'type' and 'script'.
     *
     * @return a list of {#link ConfigurationDescriptionParameter}s
     */
    private List<ConfigDescriptionParameter> getConfigDescriptions(@Nullable Locale locale) {
        List<ParameterOption> parameterOptionsList = new ArrayList<>();
        for (Map.Entry<String, String> entry : parameterOptions.entrySet()) {
            parameterOptionsList.add(new ParameterOption(entry.getKey(), entry.getValue()));
        }
        final ConfigDescriptionParameter scriptType = ConfigDescriptionParameterBuilder.create("type", Type.TEXT)
                .withRequired(true).withReadOnly(true).withMultiple(false).withLabel("Script Type")
                .withDescription("the scripting language used").withOptions(parameterOptionsList)
                .withLimitToOptions(true).build();
        final ConfigDescriptionParameter script = ConfigDescriptionParameterBuilder.create("script", Type.TEXT)
                .withRequired(true).withReadOnly(false).withMultiple(false).withLabel("Script").withContext("script")
                .withDescription("the script to execute").build();
        return List.of(scriptType, script);
    }

    @Override
    public Collection<ModuleType> getModuleTypes(@Nullable Locale locale) {
        return Stream
                .of(Optional.ofNullable(getScriptActionType(locale)),
                        Optional.ofNullable(getScriptConditionType(locale)))
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Collection<ModuleType> getAll() {
        return getModuleTypes(null);
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        // does nothing because this provider does not change
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        // does nothing because this provider does not change
    }

    /**
     * As {@link ScriptEngineFactory}s are added/removed, this method will create the {@link ParameterOption}s
     * that are available when selecting a script type in a ScriptActionType or ScriptConditionType.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void setScriptEngineFactory(ScriptEngineFactory engineFactory) {
        List<String> scriptTypes = engineFactory.getScriptTypes();
        if (!scriptTypes.isEmpty()) {
            ScriptEngine scriptEngine = engineFactory.createScriptEngine(scriptTypes.get(0));
            if (scriptEngine != null) {
                javax.script.ScriptEngineFactory factory = scriptEngine.getFactory();
                parameterOptions.put(getPreferredMimeType(factory), getLanguageName(factory));
                logger.trace("ParameterOptions: {}", parameterOptions);
            } else {
                logger.trace("setScriptEngineFactory: engine was null");
            }
        } else {
            logger.trace("addScriptEngineFactory: scriptTypes was empty");
        }
    }

    public void unsetScriptEngineFactory(ScriptEngineFactory engineFactory) {
        List<String> scriptTypes = engineFactory.getScriptTypes();
        if (!scriptTypes.isEmpty()) {
            ScriptEngine scriptEngine = engineFactory.createScriptEngine(scriptTypes.get(0));
            if (scriptEngine != null) {
                javax.script.ScriptEngineFactory factory = scriptEngine.getFactory();
                parameterOptions.remove(getPreferredMimeType(factory));
                logger.trace("ParameterOptions: {}", parameterOptions);
            } else {
                logger.trace("unsetScriptEngineFactory: engine was null");
            }
        } else {
            logger.trace("unsetScriptEngineFactory: scriptTypes was empty");
        }
    }

    private String getPreferredMimeType(javax.script.ScriptEngineFactory factory) {
        List<String> mimeTypes = new ArrayList<>(factory.getMimeTypes());
        mimeTypes.removeIf(mimeType -> !mimeType.contains("application") || mimeType.contains("x-"));
        return mimeTypes.isEmpty() ? factory.getMimeTypes().get(0) : mimeTypes.get(0);
    }

    private String getLanguageName(javax.script.ScriptEngineFactory factory) {
        return String.format("%s (%s)",
                factory.getLanguageName().substring(0, 1).toUpperCase() + factory.getLanguageName().substring(1),
                factory.getLanguageVersion());
    }
}
