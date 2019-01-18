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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.script.ScriptEngine;

import org.eclipse.smarthome.automation.module.script.ScriptEngineFactory;
import org.eclipse.smarthome.automation.module.script.ScriptExtensionProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This manager allows a script import extension providers
 *
 * @author Simon Merschjohann
 *
 */
@Component(service = ScriptExtensionManager.class)
public class ScriptExtensionManager {
    private Set<ScriptExtensionProvider> scriptExtensionProviders = new CopyOnWriteArraySet<ScriptExtensionProvider>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addScriptExtensionProvider(ScriptExtensionProvider provider) {
        scriptExtensionProviders.add(provider);
    }

    public void removeScriptExtensionProvider(ScriptExtensionProvider provider) {
        scriptExtensionProviders.remove(provider);
    }

    public void addExtension(ScriptExtensionProvider provider) {
        scriptExtensionProviders.add(provider);
    }

    public void removeExtension(ScriptExtensionProvider provider) {
        scriptExtensionProviders.remove(provider);
    }

    public List<String> getTypes() {
        ArrayList<String> types = new ArrayList<>();

        for (ScriptExtensionProvider provider : scriptExtensionProviders) {
            types.addAll(provider.getTypes());
        }

        return types;
    }

    public List<String> getPresets() {
        ArrayList<String> presets = new ArrayList<>();

        for (ScriptExtensionProvider provider : scriptExtensionProviders) {
            presets.addAll(provider.getPresets());
        }

        return presets;
    }

    public Object get(String type, String scriptIdentifier) {
        for (ScriptExtensionProvider provider : scriptExtensionProviders) {
            if (provider.getTypes().contains(type)) {
                return provider.get(scriptIdentifier, type);
            }
        }

        return null;
    }

    public List<String> getDefaultPresets() {
        ArrayList<String> defaultPresets = new ArrayList<>();

        for (ScriptExtensionProvider provider : scriptExtensionProviders) {
            defaultPresets.addAll(provider.getDefaultPresets());
        }

        return defaultPresets;
    }

    public void importDefaultPresets(ScriptEngineFactory engineProvider, ScriptEngine scriptEngine,
            String scriptIdentifier) {
        for (String preset : getDefaultPresets()) {
            importPreset(preset, engineProvider, scriptEngine, scriptIdentifier);
        }
    }

    public void importPreset(String preset, ScriptEngineFactory engineProvider, ScriptEngine scriptEngine,
            String scriptIdentifier) {
        for (ScriptExtensionProvider provider : scriptExtensionProviders) {
            if (provider.getPresets().contains(preset)) {
                Map<String, Object> scopeValues = provider.importPreset(scriptIdentifier, preset);

                engineProvider.scopeValues(scriptEngine, scopeValues);
            }
        }
    }

    public void dispose(String scriptIdentifier) {
        for (ScriptExtensionProvider provider : scriptExtensionProviders) {
            provider.unload(scriptIdentifier);
        }
    }

}
