/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.openhab.core.automation.module.script.rulesupport.shared.ProviderItemRegistryDelegate;
import org.openhab.core.automation.module.script.rulesupport.shared.ScriptedItemProvider;
import org.openhab.core.items.ItemRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ProviderScriptExtension} extends scripts to provide openHAB entities like items.
 * It handles the lifecycle of these entities, ensuring that they are removed when the script is unloaded.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class ProviderScriptExtension implements ScriptExtensionProvider {
    static final String PRESET_NAME = "provider";
    static final String ITEM_REGISTRY_NAME = "itemRegistry";

    private final Map<String, Map<String, Object>> objectCache = new ConcurrentHashMap<>();

    private final ItemRegistry itemRegistry;
    private final ScriptedItemProvider itemProvider;

    @Activate
    public ProviderScriptExtension(final @Reference ItemRegistry itemRegistry,
            final @Reference ScriptedItemProvider itemProvider) {
        this.itemRegistry = itemRegistry;
        this.itemProvider = itemProvider;
    }

    @Override
    public Collection<String> getDefaultPresets() {
        return Set.of();
    }

    @Override
    public Collection<String> getPresets() {
        return Set.of(PRESET_NAME);
    }

    @Override
    public Collection<String> getTypes() {
        return Set.of(ITEM_REGISTRY_NAME);
    }

    @Override
    public @Nullable Object get(String scriptIdentifier, String type) throws IllegalArgumentException {
        Map<String, Object> objects = Objects
                .requireNonNull(objectCache.computeIfAbsent(scriptIdentifier, k -> new HashMap<>()));

        Object obj = objects.get(type);
        if (obj != null) {
            return obj;
        }

        if (ITEM_REGISTRY_NAME.equals(type)) {
            ProviderItemRegistryDelegate itemRegistryDelegate = new ProviderItemRegistryDelegate(itemRegistry,
                    itemProvider);
            objects.put(ITEM_REGISTRY_NAME, itemRegistryDelegate);
            return itemRegistryDelegate;
        }

        return obj;
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if (PRESET_NAME.equals(preset)) {
            return Map.of(ITEM_REGISTRY_NAME, Objects.requireNonNull(get(scriptIdentifier, ITEM_REGISTRY_NAME)));
        }

        return Map.of();
    }

    @Override
    public void unload(String scriptIdentifier) {
        Map<String, Object> objects = objectCache.remove(scriptIdentifier);
        if (objects != null) {
            Object itemRegistry = objects.get(ITEM_REGISTRY_NAME);
            if (itemRegistry != null) {
                ProviderItemRegistryDelegate itemRegistryDelegate = (ProviderItemRegistryDelegate) itemRegistry;
                itemRegistryDelegate.removeAllAddedByScript();
            }
        }
    }
}
