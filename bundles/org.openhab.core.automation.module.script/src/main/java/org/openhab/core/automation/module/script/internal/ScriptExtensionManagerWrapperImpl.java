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
package org.openhab.core.automation.module.script.internal;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptExtensionManagerWrapper;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
@NonNullByDefault
public class ScriptExtensionManagerWrapperImpl implements ScriptExtensionManagerWrapper {

    private final ScriptEngineContainer container;
    private final ScriptExtensionManager manager;

    public ScriptExtensionManagerWrapperImpl(ScriptExtensionManager manager, ScriptEngineContainer container) {
        this.manager = manager;
        this.container = container;
    }

    public void addScriptExtensionProvider(ScriptExtensionProvider provider) {
        manager.addExtension(provider);
    }

    public void removeScriptExtensionProvider(ScriptExtensionProvider provider) {
        manager.removeExtension(provider);
    }

    @Override
    public List<String> getTypes() {
        return manager.getTypes();
    }

    @Override
    public String getScriptIdentifier() {
        return container.getIdentifier();
    }

    @Override
    public List<String> getPresets() {
        return manager.getPresets();
    }

    @Override
    public @Nullable Object get(String type) {
        return manager.get(type, container.getIdentifier());
    }

    @Override
    public List<String> getDefaultPresets() {
        return manager.getDefaultPresets();
    }

    @Override
    public Map<String, Object> importPreset(String preset) {
        return manager.importPreset(preset, container.getFactory(), container.getScriptEngine(),
                container.getIdentifier());
    }
}
