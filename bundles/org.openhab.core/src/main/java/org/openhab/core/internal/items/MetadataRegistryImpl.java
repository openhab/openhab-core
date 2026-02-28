/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.internal.items;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ManagedMetadataProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.service.ReadyService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This is the main implementing class of the {@link MetadataRegistry} interface. It
 * keeps track of all declared metadata of all metadata providers.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Mark Herwege - semantics namespace not in managed provider
 */
@Component(immediate = true, service = MetadataRegistry.class)
@NonNullByDefault
public class MetadataRegistryImpl extends AbstractRegistry<Metadata, MetadataKey, MetadataProvider>
        implements MetadataRegistry {

    private Map<String, MetadataProvider> reservedNamespaces = new ConcurrentHashMap<>();

    @Activate
    public MetadataRegistryImpl(final @Reference ReadyService readyService) {
        super(MetadataProvider.class);
        super.setReadyService(readyService);
    }

    @Override
    @Activate
    protected void activate(BundleContext context) {
        super.activate(context);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public boolean isInternalNamespace(String namespace) {
        return namespace.startsWith(INTERNAL_NAMESPACE_PREFIX);
    }

    /**
     * Provides all namespaces of a particular item
     *
     * @param itemname the name of the item for which the namespaces should be searched.
     */
    @Override
    public Collection<String> getAllNamespaces(String itemname) {
        return stream().map(Metadata::getUID).filter(key -> key.getItemName().equals(itemname))
                .map(MetadataKey::getNamespace).collect(Collectors.toSet());
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    @Override
    protected void setEventPublisher(EventPublisher eventPublisher) {
        super.setEventPublisher(eventPublisher);
    }

    @Override
    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        super.unsetEventPublisher(eventPublisher);
    }

    @Override
    @Reference
    protected void setReadyService(ReadyService readyService) {
        super.setReadyService(readyService);
    }

    @Override
    protected void unsetReadyService(ReadyService readyService) {
        super.unsetReadyService(readyService);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedMetadataProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedMetadataProvider managedProvider) {
        super.unsetManagedProvider(managedProvider);
    }

    @Override
    public void removeItemMetadata(String itemName) {
        // remove our metadata for that item
        getManagedProvider()
                .ifPresent(managedProvider -> ((ManagedMetadataProvider) managedProvider).removeItemMetadata(itemName));
    }

    @Override
    public Metadata add(Metadata element) {
        String namespace = element.getUID().getNamespace();
        if (reservedNamespaces.get(namespace) == null || reservedNamespaces.get(namespace) instanceof ManagedProvider) {
            return super.add(element);
        }
        throw new IllegalStateException("Cannot add metadata to '" + namespace + "' namespace");
    }

    @Override
    public @Nullable Metadata update(Metadata element) {
        String namespace = element.getUID().getNamespace();
        if (reservedNamespaces.get(namespace) == null || reservedNamespaces.get(namespace) instanceof ManagedProvider) {
            return super.update(element);
        }
        throw new IllegalStateException("Cannot update metadata in '" + namespace + "' namespace");
    }

    @Override
    public @Nullable Metadata remove(MetadataKey key) {
        String namespace = key.getNamespace();
        if (reservedNamespaces.get(namespace) == null || reservedNamespaces.get(namespace) instanceof ManagedProvider) {
            return super.remove(key);
        }
        throw new IllegalStateException("Cannot remove metadata from '" + namespace + "' namespace");
    }

    @Override
    protected void addProvider(Provider<Metadata> provider) {
        if (provider instanceof MetadataProvider metadataProvider) {
            metadataProvider.getReservedNamespaces().stream()
                    .forEach(namespace -> reservedNamespaces.putIfAbsent(namespace, metadataProvider));
        }
        super.addProvider(provider);
    }

    @Override
    protected void removeProvider(Provider<Metadata> provider) {
        if (provider instanceof MetadataProvider metadataProvider) {
            metadataProvider.getReservedNamespaces().stream()
                    .forEach(namespace -> reservedNamespaces.remove(namespace, metadataProvider));
        }
        super.removeProvider(provider);
    }
}
