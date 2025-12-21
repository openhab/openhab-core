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
package org.openhab.core.persistence.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.persistence.registry.ManagedPersistenceServiceConfigurationProvider;
import org.openhab.core.persistence.registry.PersistenceServiceConfiguration;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationProvider;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistry;
import org.openhab.core.persistence.registry.PersistenceServiceConfigurationRegistryChangeListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PersistenceServiceConfigurationRegistryImpl} implements the
 * {@link PersistenceServiceConfigurationRegistry}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = PersistenceServiceConfigurationRegistry.class)
public class PersistenceServiceConfigurationRegistryImpl
        extends AbstractRegistry<PersistenceServiceConfiguration, String, PersistenceServiceConfigurationProvider>
        implements PersistenceServiceConfigurationRegistry {
    private final Logger logger = LoggerFactory.getLogger(PersistenceServiceConfigurationRegistryImpl.class);
    private final Map<String, Provider<PersistenceServiceConfiguration>> serviceToProvider = new HashMap<>();
    private final Map<String, Set<Provider<PersistenceServiceConfiguration>>> serviceToAllProviders = new HashMap<>();
    private final Set<PersistenceServiceConfigurationRegistryChangeListener> registryChangeListeners = new CopyOnWriteArraySet<>();

    public PersistenceServiceConfigurationRegistryImpl() {
        super(PersistenceServiceConfigurationProvider.class);
    }

    @Override
    public void added(Provider<PersistenceServiceConfiguration> provider, PersistenceServiceConfiguration element) {
        String elementUID = element.getUID();
        Set<Provider<PersistenceServiceConfiguration>> providers = serviceToAllProviders.getOrDefault(elementUID,
                new HashSet<>());
        providers.add(provider);
        serviceToAllProviders.put(elementUID, providers);
        Provider<PersistenceServiceConfiguration> existingProvider = serviceToProvider.get(elementUID);
        if (existingProvider != null) {
            String newProvider = provider instanceof ManagedPersistenceServiceConfigurationProvider ? "managed" : "DSL";
            String oldProvider = existingProvider instanceof ManagedPersistenceServiceConfigurationProvider ? "managed"
                    : "DSL";
            logger.warn(
                    "Tried to add strategy container with serviceId '{}' from {} provider, but it was already added before from {} provider.",
                    elementUID, newProvider, oldProvider);
        } else {
            serviceToProvider.put(elementUID, provider);
            super.added(provider, element);
        }
    }

    @Override
    public void removed(Provider<PersistenceServiceConfiguration> provider, PersistenceServiceConfiguration element) {
        String elementUID = element.getUID();
        Set<Provider<PersistenceServiceConfiguration>> providers = serviceToAllProviders.get(elementUID);
        if (providers != null) {
            providers.remove(provider);
        }
        if (!provider.equals(serviceToProvider.getOrDefault(elementUID, provider))) {
            logger.warn("Tried to remove strategy container with serviceId '{}', but it was added by another provider.",
                    element.getUID());
        } else {
            super.removed(provider, element);
            if (providers != null && !providers.isEmpty()) {
                Provider<PersistenceServiceConfiguration> alternateProvider = providers.stream().findAny().get();
                serviceToProvider.put(elementUID, alternateProvider);
                super.added(alternateProvider, element);
            }
        }
    }

    @Override
    public void updated(Provider<PersistenceServiceConfiguration> provider, PersistenceServiceConfiguration oldelement,
            PersistenceServiceConfiguration element) {
        if (!provider.equals(serviceToProvider.getOrDefault(element.getUID(), provider))) {
            logger.warn("Tried to update strategy container with serviceId '{}', but it was added by another provider.",
                    element.getUID());
        } else {
            super.updated(provider, oldelement, element);
        }
    }

    @Override
    protected void notifyListenersAboutAddedElement(PersistenceServiceConfiguration element) {
        registryChangeListeners.forEach(listener -> listener.added(element));
        super.notifyListenersAboutAddedElement(element);
    }

    @Override
    protected void notifyListenersAboutRemovedElement(PersistenceServiceConfiguration element) {
        registryChangeListeners.forEach(listener -> listener.removed(element));
        super.notifyListenersAboutRemovedElement(element);
    }

    @Override
    protected void notifyListenersAboutUpdatedElement(PersistenceServiceConfiguration oldElement,
            PersistenceServiceConfiguration element) {
        registryChangeListeners.forEach(listener -> listener.updated(oldElement, element));
    }

    @Override
    public void addRegistryChangeListener(PersistenceServiceConfigurationRegistryChangeListener listener) {
        registryChangeListeners.add(listener);
    }

    @Override
    public void removeRegistryChangeListener(PersistenceServiceConfigurationRegistryChangeListener listener) {
        registryChangeListeners.remove(listener);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedPersistenceServiceConfigurationProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedPersistenceServiceConfigurationProvider provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    public List<String> getPersistenceServiceConfigurationRegistryConflicts() {
        return serviceToAllProviders.entrySet().stream().filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey()).toList();
    }
}
