/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.ui.internal.components;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentProvider;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.openhab.core.ui.components.UIProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Implementation for a {@link UIComponentRegistryFactory} using a set of {@link UIComponentProvider}.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Łukasz Dywicki - Removed explicit dependency on storage providers.
 * @author Jonathan Gilbert - Made providers' collections immutable.
 */
@NonNullByDefault
@Component(service = UIComponentRegistryFactory.class, immediate = true)
public class UIComponentRegistryFactoryImpl implements UIComponentRegistryFactory {
    Map<String, UIComponentRegistryImpl> registries = new ConcurrentHashMap<>();
    Map<String, Set<UIProvider>> providers = new ConcurrentHashMap<>();

    @Override
    public UIComponentRegistryImpl getRegistry(String namespace) {
        UIComponentRegistryImpl registry = registries.get(namespace);
        if (registry == null) {
            Set<UIProvider> namespaceProviders = this.providers.get(namespace);
            ManagedProvider<RootUIComponent, String> managedProvider = null;
            if (namespaceProviders != null) {
                for (UIProvider provider : namespaceProviders) {
                    if (provider instanceof ManagedProvider) {
                        managedProvider = (ManagedProvider<RootUIComponent, String>) provider;
                        break;
                    }
                }
            }
            registry = new UIComponentRegistryImpl(namespace, managedProvider, namespaceProviders);
            registries.put(namespace, registry);
        }
        return registry;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addProvider(UIProvider provider) {
        UIComponentRegistryImpl registry = registries.get(provider.getNamespace());
        if (registry != null) {
            registry.addProvider(provider);
        }
        registerProvider(provider);
    }

    void removeProvider(UIProvider provider) {
        UIComponentRegistryImpl registry = registries.get(provider.getNamespace());
        if (registry != null) {
            registry.removeProvider(provider);
        }
        deregisterProvider(provider);
    }

    private void registerProvider(UIProvider provider) {
        Set<UIProvider> existing = providers.get(provider.getNamespace());

        if (existing == null) {
            existing = Collections.emptySet();
        }

        Set<UIProvider> updated = new HashSet<>(existing);
        updated.add(provider);
        providers.put(provider.getNamespace(), Set.copyOf(updated));
    }

    private void deregisterProvider(UIProvider provider) {
        Set<UIProvider> existing = providers.get(provider.getNamespace());

        if (existing != null && !existing.isEmpty()) {
            Set<UIProvider> updated = new HashSet<>(existing);
            updated.remove(provider);
            providers.put(provider.getNamespace(), Set.copyOf(updated));
        }
    }
}
