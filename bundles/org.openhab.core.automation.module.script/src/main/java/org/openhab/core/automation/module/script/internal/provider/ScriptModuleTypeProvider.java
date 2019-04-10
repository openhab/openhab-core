/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
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
@Component
public class ScriptModuleTypeProvider implements ModuleTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<ParameterOption> parameterOptions = new CopyOnWriteArrayList<>();

    @SuppressWarnings("unchecked")
    @Override
    public ModuleType getModuleType(String UID, Locale locale) {
        if (ScriptActionHandler.TYPE_ID.equals(UID)) {
            return getScriptActionType(locale);
        } else if (ScriptConditionHandler.TYPE_ID.equals(UID)) {
            return getScriptConditionType(locale);
        } else {
            return null;
        }
    }

    private ModuleType getScriptActionType(Locale locale) {
        if (parameterOptions.isEmpty()) {
            return null;
        } else {
            List<Output> outputs = new ArrayList<Output>();

            Output result = new Output("result", "java.lang.Object", "result", "the script result", null, null, null);
            outputs.add(result);
            return new ActionType(ScriptActionHandler.TYPE_ID, getConfigDescriptions(locale), "execute a given script",
                    "Allows the execution of a user-defined script.", null, Visibility.VISIBLE, null, outputs);
        }
    }

    private ModuleType getScriptConditionType(Locale locale) {
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
    private List<ConfigDescriptionParameter> getConfigDescriptions(Locale locale) {
        final ConfigDescriptionParameter scriptType = ConfigDescriptionParameterBuilder.create("type", Type.TEXT)
                .withRequired(true).withReadOnly(true).withMultiple(false).withLabel("Script Type")
                .withDescription("the scripting language used").withOptions(parameterOptions).withLimitToOptions(true)
                .build();
        final ConfigDescriptionParameter script = ConfigDescriptionParameterBuilder.create("script", Type.TEXT)
                .withRequired(true).withReadOnly(false).withMultiple(false).withLabel("Script").withContext("script")
                .withDescription("the script to execute").build();
        return Stream.of(scriptType, script).collect(Collectors.toList());
    }

    @Override
    public Collection<ModuleType> getModuleTypes(Locale locale) {
        return Stream.of(getScriptActionType(locale), getScriptConditionType(locale)).collect(Collectors.toList());
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
     * As {@link ScriptEngineFactory}s are added/changed, this method will create the {@link ParameterOption}s
     * that are available when selecting a script type in a ScriptActionType or ScriptConditionType.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void setScriptEngineFactory(ScriptEngineFactory engineFactory) {
        parameterOptions.clear();
        for (javax.script.ScriptEngineFactory f : ScriptEngineFactory.engineManager.getEngineFactories()) {
            String languageName = String.format("%s (%s)", StringUtils.capitalize(f.getLanguageName()),
                    f.getLanguageVersion());
            List<String> mimeTypes = new ArrayList<>();

            mimeTypes.addAll(f.getMimeTypes());
            String preferredMimeType = mimeTypes.get(0);
            mimeTypes.removeIf(mimeType -> !mimeType.contains("application") || mimeType.contains("x-"));
            if (mimeTypes.size() > 0) {
                preferredMimeType = mimeTypes.get(0);
            }
            parameterOptions.add(new ParameterOption(preferredMimeType, languageName));
        }
        logger.trace("ParameterOptions: {}", parameterOptions);
    }

    public void unsetScriptEngineFactory(ScriptEngineFactory scriptEngineFactory) {
        parameterOptions.clear();
    }

}
