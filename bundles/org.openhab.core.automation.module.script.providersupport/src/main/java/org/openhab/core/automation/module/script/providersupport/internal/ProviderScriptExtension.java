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
package org.openhab.core.automation.module.script.providersupport.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.openhab.core.automation.module.script.providersupport.shared.ProviderItemRegistryDelegate;
import org.openhab.core.automation.module.script.providersupport.shared.ProviderThingRegistryDelegate;
import org.openhab.core.automation.module.script.providersupport.shared.ScriptedItemProvider;
import org.openhab.core.automation.module.script.providersupport.shared.ScriptedThingProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.thing.ThingRegistry;
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
    private static final String PRESET_NAME = "provider";
    private static final String ITEM_REGISTRY_NAME = "itemRegistry";
    private static final String THING_REGISTRY_NAME = "thingRegistry";

    private final Map<String, Map<String, Object>> objectCache = new ConcurrentHashMap<>();

    private final ItemRegistry itemRegistry;
    private final ScriptedItemProvider itemProvider;
    private final ThingRegistry thingRegistry;
    private final ScriptedThingProvider thingProvider;

    @Activate
    public ProviderScriptExtension(final @Reference ItemRegistry itemRegistry,
            final @Reference ScriptedItemProvider itemProvider, final @Reference ThingRegistry thingRegistry,
            final @Reference ScriptedThingProvider thingProvider) {
        this.itemRegistry = itemRegistry;
        this.itemProvider = itemProvider;
        this.thingRegistry = thingRegistry;
        this.thingProvider = thingProvider;
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
        return Set.of(ITEM_REGISTRY_NAME, THING_REGISTRY_NAME);
    }

    @Override
    public @Nullable Object get(String scriptIdentifier, String type) throws IllegalArgumentException {
        Map<String, Object> objects = Objects
                .requireNonNull(objectCache.computeIfAbsent(scriptIdentifier, k -> new HashMap<>()));

        Object obj = objects.get(type);
        if (obj != null) {
            return obj;
        }

        return switch (type) {
            case ITEM_REGISTRY_NAME -> {
                ProviderItemRegistryDelegate itemRegistryDelegate = new ProviderItemRegistryDelegate(itemRegistry,
                        itemProvider);
                objects.put(ITEM_REGISTRY_NAME, itemRegistryDelegate);
                yield itemRegistryDelegate;
            }
            case THING_REGISTRY_NAME -> {
                ProviderThingRegistryDelegate thingRegistryDelegate = new ProviderThingRegistryDelegate(thingRegistry,
                        thingProvider);
                objects.put(THING_REGISTRY_NAME, thingRegistryDelegate);
                yield thingRegistryDelegate;
            }
            default -> null;
        };
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if (PRESET_NAME.equals(preset)) {
            return Map.of(ITEM_REGISTRY_NAME, Objects.requireNonNull(get(scriptIdentifier, ITEM_REGISTRY_NAME)),
                    THING_REGISTRY_NAME, Objects.requireNonNull(get(scriptIdentifier, THING_REGISTRY_NAME)));
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
            Object thingRegistry = objects.get(THING_REGISTRY_NAME);
            if (thingRegistry != null) {
                ProviderThingRegistryDelegate thingRegistryDelegate = (ProviderThingRegistryDelegate) thingRegistry;
                thingRegistryDelegate.removeAllAddedByScript();
            }
        }
    }
}
