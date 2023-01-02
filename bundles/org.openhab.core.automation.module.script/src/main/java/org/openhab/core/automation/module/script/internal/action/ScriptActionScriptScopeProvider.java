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
package org.openhab.core.automation.module.script.internal.action;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.openhab.core.automation.module.script.action.ScriptExecution;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This is a scope provider for script actions that were available in openHAB 1 DSL rules
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class ScriptActionScriptScopeProvider implements ScriptExtensionProvider {

    private static final String PRESET_ACTIONS = "ScriptAction";

    private final Map<String, Object> elements;

    @Activate
    public ScriptActionScriptScopeProvider(final @Reference ScriptExecution scriptExecution) {
        elements = Map.of("scriptExecution", scriptExecution);
    }

    @Override
    public Collection<String> getDefaultPresets() {
        return Set.of();
    }

    @Override
    public Collection<String> getPresets() {
        return Set.of(PRESET_ACTIONS);
    }

    @Override
    public Collection<String> getTypes() {
        return elements.keySet();
    }

    @Override
    public @Nullable Object get(String scriptIdentifier, String type) {
        return elements.get(type);
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if (PRESET_ACTIONS.equals(preset)) {
            return elements;
        }
        return Map.of();
    }

    @Override
    public void unload(String scriptIdentifier) {
        // nothing todo
    }
}
