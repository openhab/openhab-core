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
package org.openhab.core.automation.module.script.providersupport.shared;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.providersupport.internal.ProviderRegistry;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataPredicates;
import org.openhab.core.items.MetadataRegistry;

/**
 * The {@link ProviderMetadataRegistryDelegate} is wrapping a {@link MetadataRegistry} to provide a comfortable way to
 * provide items from scripts without worrying about the need to remove metadata again when the script is unloaded.
 * Nonetheless, using the {@link #addPermanent(Metadata)} method it is still possible to add metadata permanently.
 * <p>
 * Use a new instance of this class for each {@link javax.script.ScriptEngine}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ProviderMetadataRegistryDelegate implements MetadataRegistry, ProviderRegistry {
    private final MetadataRegistry metadataRegistry;

    private final Set<MetadataKey> metadataKeys = new HashSet<>();

    private final ScriptedMetadataProvider metadataProvider;

    public ProviderMetadataRegistryDelegate(MetadataRegistry metadataRegistry,
            ScriptedMetadataProvider metadataProvider) {
        this.metadataRegistry = metadataRegistry;
        this.metadataProvider = metadataProvider;
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<Metadata> listener) {
        metadataRegistry.addRegistryChangeListener(listener);
    }

    @Override
    public Collection<Metadata> getAll() {
        return metadataRegistry.getAll();
    }

    @Override
    public Stream<Metadata> stream() {
        return metadataRegistry.stream();
    }

    @Override
    public @Nullable Metadata get(MetadataKey key) {
        return metadataRegistry.get(key);
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<Metadata> listener) {
        metadataRegistry.removeRegistryChangeListener(listener);
    }

    @Override
    public Metadata add(Metadata element) {
        MetadataKey key = element.getUID();
        // Check for metadata already existing here because the metadata might exist in a different provider, so we need
        // to check the registry and not only the provider itself
        if (get(key) != null) {
            throw new IllegalArgumentException(
                    "Cannot add metadata, because metadata with same name (" + key + ") already exists.");
        }

        metadataProvider.add(element);
        metadataKeys.add(key);

        return element;
    }

    /**
     * Adds metadata permanently to the registry.
     * This metadata will be kept in the registry even if the script is unloaded.
     * 
     * @param element the metadata to be added (must not be null)
     * @return the added metadata
     */
    public Metadata addPermanent(Metadata element) {
        return metadataRegistry.add(element);
    }

    @Override
    public @Nullable Metadata update(Metadata element) {
        if (metadataKeys.contains(element.getUID())) {
            return metadataProvider.update(element);
        }
        return metadataRegistry.update(element);
    }

    @Override
    public @Nullable Metadata remove(MetadataKey key) {
        if (metadataKeys.contains(key)) {
            return metadataProvider.remove(key);
        }
        return metadataRegistry.remove(key);
    }

    @Override
    public boolean isInternalNamespace(String namespace) {
        return metadataRegistry.isInternalNamespace(namespace);
    }

    @Override
    public Collection<String> getAllNamespaces(String itemname) {
        return metadataRegistry.getAllNamespaces(itemname);
    }

    @Override
    public void removeItemMetadata(String itemname) {
        if (metadataProvider.getAll().stream().anyMatch(MetadataPredicates.ofItem(itemname))) {
            metadataProvider.removeItemMetadata(itemname);
            return;
        }
        metadataRegistry.removeItemMetadata(itemname);
    }

    @Override
    public void removeAllAddedByScript() {
        for (MetadataKey key : metadataKeys) {
            metadataProvider.remove(key);
        }
        metadataKeys.clear();
    }
}
