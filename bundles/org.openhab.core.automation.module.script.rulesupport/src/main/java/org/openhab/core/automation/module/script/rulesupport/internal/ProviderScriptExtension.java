/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptExtensionProvider;
import org.openhab.core.automation.module.script.rulesupport.shared.ScriptedItemProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link ProviderScriptExtension} provides types to support providing openHAB entities like items through scripts.
 * It handles the registration and un-registration of the {@link org.openhab.core.common.registry.Provider} services.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class ProviderScriptExtension implements ScriptExtensionProvider {
    static final String PRESET_NAME = "provider";
    static final String ITEM_PROVIDER_NAME = "itemProvider";

    private final Map<String, ItemProviderContainer> itemProviders = new ConcurrentHashMap<>();
    private final BundleContext bundleContext;

    @Activate
    public ProviderScriptExtension(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
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
        return Set.of(ITEM_PROVIDER_NAME);
    }

    @Override
    public @Nullable Object get(String scriptIdentifier, String type) throws IllegalArgumentException {
        if (ITEM_PROVIDER_NAME.equals(type)) {
            return itemProviders.computeIfAbsent(scriptIdentifier, key -> {
                ScriptedItemProvider itemProvider = new ScriptedItemProvider();
                ServiceRegistration<ScriptedItemProvider> registration = bundleContext
                        .registerService(ScriptedItemProvider.class, itemProvider, null);
                return new ItemProviderContainer(itemProvider, registration);
            }).itemProvider();
        }

        return null;
    }

    @Override
    public Map<String, Object> importPreset(String scriptIdentifier, String preset) {
        if (PRESET_NAME.equals(preset)) {
            return Map.of(ITEM_PROVIDER_NAME, Objects.requireNonNull(get(scriptIdentifier, ITEM_PROVIDER_NAME)));
        }

        return Map.of();
    }

    @Override
    public void unload(String scriptIdentifier) {
        ItemProviderContainer itemProviderContainer = itemProviders.remove(scriptIdentifier);
        if (itemProviderContainer != null) {
            itemProviderContainer.registration().unregister();
        }
    }

    private record ItemProviderContainer(ScriptedItemProvider itemProvider,
            ServiceRegistration<ScriptedItemProvider> registration) {
    }
}
