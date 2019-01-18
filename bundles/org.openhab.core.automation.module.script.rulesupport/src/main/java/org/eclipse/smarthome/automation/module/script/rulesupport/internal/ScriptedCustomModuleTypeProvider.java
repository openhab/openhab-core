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
package org.eclipse.smarthome.automation.module.script.rulesupport.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link ScriptedCustomModuleTypeProvider} is used in combination with the
 * {@link ScriptedCustomModuleHandlerFactory} to allow scripts to define custom types in the RuleManager. These
 * registered types can then be used publicly from any Rule-Editor.
 *
 * @author Simon Merschjohann - initial contribution
 *
 */
@Component(immediate = true, service = { ScriptedCustomModuleTypeProvider.class, ModuleTypeProvider.class })
public class ScriptedCustomModuleTypeProvider implements ModuleTypeProvider {
    private final HashMap<String, ModuleType> modulesTypes = new HashMap<>();

    private final HashSet<ProviderChangeListener<ModuleType>> listeners = new HashSet<>();

    @Override
    public Collection<ModuleType> getAll() {
        return modulesTypes.values();
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        this.listeners.remove(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> T getModuleType(String UID, Locale locale) {
        ModuleType handler = modulesTypes.get(UID);

        return (T) handler;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> Collection<T> getModuleTypes(Locale locale) {
        return (Collection<T>) modulesTypes.values();
    }

    public void addModuleType(ModuleType moduleType) {
        modulesTypes.put(moduleType.getUID(), moduleType);

        for (ProviderChangeListener<ModuleType> listener : listeners) {
            listener.added(this, moduleType);
        }
    }

    public void removeModuleType(ModuleType moduleType) {
        removeModuleType(moduleType.getUID());
    }

    public void removeModuleType(String moduleTypeUID) {
        ModuleType element = modulesTypes.remove(moduleTypeUID);

        for (ProviderChangeListener<ModuleType> listener : listeners) {
            listener.removed(this, element);
        }
    }

    public void updateModuleHandler(String uid) {
        ModuleType modType = modulesTypes.get(uid);

        if (modType != null) {
            for (ProviderChangeListener<ModuleType> listener : listeners) {
                listener.updated(this, null, modType);
            }
        }
    }
}
