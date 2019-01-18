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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.automation.module.script.ScriptEngineContainer;
import org.eclipse.smarthome.automation.module.script.ScriptExtensionProvider;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
public class ScriptExtensionManagerWrapper {
    private ScriptEngineContainer container;
    private ScriptExtensionManager manager;

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

    public Object get(String type) {
        return manager.get(type, container.getIdentifier());
    }

    public List<String> getDefaultPresets() {
        return manager.getDefaultPresets();
    }

    public void importPreset(String preset) {
        manager.importPreset(preset, container.getFactory(), container.getScriptEngine(), container.getIdentifier());
    }
}
