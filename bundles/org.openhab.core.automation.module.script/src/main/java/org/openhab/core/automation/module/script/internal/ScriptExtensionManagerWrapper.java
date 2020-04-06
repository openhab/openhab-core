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

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
public class ScriptExtensionManagerWrapper {

    private final ScriptEngineContainer container;
    private final ScriptExtensionManager manager;

    public ScriptExtensionManagerWrapper(ScriptExtensionManager manager, ScriptEngineContainer container) {
        this.manager = manager;
        this.container = container;
    }

    public void addScriptExtensionProvider(ScriptExtensionProvider provider) {
        manager.addExtension(provider);
    }

    public void removeScriptExtensionProvider(ScriptExtensionProvider provider) {
        manager.removeExtension(provider);
    }

    public List<String> getTypes() {
        return manager.getTypes();
    }

    public List<String> getPresets() {
        return manager.getPresets();
    }

    public @Nullable Object get(String type) {
        return manager.get(type, container.getIdentifier());
    }

    public List<String> getDefaultPresets() {
        return manager.getDefaultPresets();
    }

    /**
     * Imports a collection of named host objects/classes into a script engine instance. Sets of objects are provided
     * under their object name, and categorized by preset name. This method will import all named objects for a specific
     * preset name.
     *
     * @implNote This call both returns the imported objects, and requests that the {@link ScriptEngineFactory} import
     *           them. The mechanism of how they are imported by the ScriptEngineFactory, or whether they are imported
     *           at all (aside from eing returned by this call) is dependent of the implementation of the
     *           ScriptEngineFactory.
     *
     * @apiNote Objects may appear in multiple named presets.
     * @see ScriptExtensionManager
     *
     * @param preset the name of the preset to import
     * @return a map of host object names to objects
     */
    public Map<String, Object> importPreset(String preset) {
        return manager.importPreset(preset, container.getFactory(), container.getScriptEngine(),
                container.getIdentifier());
    }
}
