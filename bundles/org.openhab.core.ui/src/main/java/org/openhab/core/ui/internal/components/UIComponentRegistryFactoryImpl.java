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
package org.openhab.core.ui.internal.components;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.ui.components.UIComponentProvider;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for a {@link UIComponentRegistryFactory} using a set of {@link ManagedUIComponentProvider}.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Łukasz Dywicki - Removed explicit dependency on storage providers.
 * @author Jonathan Gilbert - Made providers' collections immutable.
 */
@NonNullByDefault
@Component(service = UIComponentRegistryFactory.class, immediate = true)
public class UIComponentRegistryFactoryImpl implements UIComponentRegistryFactory {
    private final Logger logger = LoggerFactory.getLogger(UIComponentRegistryFactoryImpl.class);

    private final ComponentFactory<ManagedUIComponentProvider> providerFactory;
    private final BundleContext bc;

    Map<String, UIComponentRegistryImpl> registries = new ConcurrentHashMap<>();
    Set<ComponentInstance<ManagedUIComponentProvider>> createdProviders = new CopyOnWriteArraySet<>();
    Map<String, Set<UIComponentProvider>> providers = new ConcurrentHashMap<>();

    @Activate
    public UIComponentRegistryFactoryImpl(
            @Reference(target = "(component.factory=org.openhab.core.ui.component.provider.factory)") ComponentFactory<ManagedUIComponentProvider> factory,
            BundleContext bc) {
        this.providerFactory = factory;
        this.bc = bc;
    }

    @Override
    public UIComponentRegistryImpl getRegistry(String namespace) {
        UIComponentRegistryImpl registry = registries.get(namespace);
        if (registry == null) {
            if (!managedProviderAvailable(namespace)) {
                logger.debug("Creating managed provider for '{}'", namespace);
                Dictionary<String, Object> properties = new Hashtable<>();
                properties.put(UIComponentProvider.CONFIG_NAMESPACE, namespace);
                ComponentInstance<ManagedUIComponentProvider> instance = this.providerFactory.newInstance(properties);
                createdProviders.add(instance);
            }
            Set<UIComponentProvider> namespaceProviders = this.providers.get(namespace);
            registry = new UIComponentRegistryImpl(namespace, namespaceProviders);
            registries.put(namespace, registry);
        }
        return registry;
    }

    @Deactivate
    public void deactivate() {
        createdProviders.forEach(ComponentInstance::dispose);
    }

    private boolean managedProviderAvailable(String namespace) {
        try {
            return bc.getServiceReferences(UIComponentProvider.class, null).stream().map(bc::getService)
                    .anyMatch(s -> namespace.equals(s.getNamespace()) && s instanceof ManagedProvider<?, ?>);
        } catch (InvalidSyntaxException e) {
            return false;
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addProvider(UIComponentProvider provider) {
        UIComponentRegistryImpl registry = registries.get(provider.getNamespace());
        if (registry != null) {
            registry.addProvider(provider);
        }
        registerProvider(provider);
    }

    void removeProvider(UIComponentProvider provider) {
        UIComponentRegistryImpl registry = registries.get(provider.getNamespace());
        if (registry != null) {
            registry.removeProvider(provider);
        }
        unregisterProvider(provider);
    }

    private void registerProvider(UIComponentProvider provider) {
        Set<UIComponentProvider> existing = providers.get(provider.getNamespace());

        if (existing == null) {
            existing = Collections.emptySet();
        }

        Set<UIComponentProvider> updated = new HashSet<>(existing);
        updated.add(provider);
        providers.put(provider.getNamespace(), Set.copyOf(updated));
    }

    private void unregisterProvider(UIComponentProvider provider) {
        Set<UIComponentProvider> existing = providers.get(provider.getNamespace());

        if (existing != null && !existing.isEmpty()) {
            Set<UIComponentProvider> updated = new HashSet<>(existing);
            updated.remove(provider);
            providers.put(provider.getNamespace(), Set.copyOf(updated));
        }
    }
}
