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
package org.openhab.core.model.item.internal;

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.emf.codegen.ecore.templates.edit.ItemProvider;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataPredicates;
import org.openhab.core.items.MetadataProvider;
import org.osgi.service.component.annotations.Component;

/**
 * This class serves as a provider for all metadata that is found within item files.
 * It is filled with content by the {@link GenericItemProvider}, which cannot itself implement the
 * {@link MetadataProvider} interface as it already implements {@link ItemProvider}, which would lead to duplicate
 * methods.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - Store metadata per model + do not notify the registry for isolated models
 */
@Component(service = { MetadataProvider.class, GenericMetadataProvider.class })
@NonNullByDefault
public class GenericMetadataProvider extends AbstractProvider<Metadata> implements MetadataProvider {

    private final Map<String, Set<Metadata>> metadata = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Set<String> isolatedModels = new HashSet<>();

    /**
     * Adds metadata to this provider
     *
     * @param modelName the model name
     * @param isolated whether the model is an isolated model
     * @param bindingType
     * @param itemName
     * @param configuration
     */
    public void addMetadata(String modelName, boolean isolated, String bindingType, String itemName, String value,
            @Nullable Map<String, Object> configuration) {
        MetadataKey key = new MetadataKey(bindingType, itemName);
        Metadata md = new Metadata(key, value, configuration);
        try {
            lock.writeLock().lock();
            Set<Metadata> mdSet = Objects.requireNonNull(metadata.computeIfAbsent(modelName, k -> new HashSet<>()));
            mdSet.add(md);
            if (isolated) {
                isolatedModels.add(modelName);
            }
        } finally {
            lock.writeLock().unlock();
        }
        if (!isolated) {
            notifyListenersAboutAddedElement(md);
        }
    }

    /**
     * Removes all meta-data for a given namespace
     *
     * @param namespace the namespace
     */
    public void removeMetadataByNamespace(String namespace) {
        Map<String, Set<Metadata>> toBeNotified;
        try {
            lock.writeLock().lock();
            toBeNotified = new HashMap<>();
            for (Map.Entry<String, Set<Metadata>> entry : metadata.entrySet()) {
                String modelName = entry.getKey();
                boolean notify = !isolatedModels.contains(modelName);
                Set<Metadata> mdSet = entry.getValue();
                Set<Metadata> toBeRemoved = mdSet.stream().filter(MetadataPredicates.hasNamespace(namespace))
                        .collect(toSet());
                mdSet.removeAll(toBeRemoved);
                if (mdSet.isEmpty()) {
                    metadata.remove(modelName);
                    isolatedModels.remove(modelName);
                }
                if (notify && !toBeRemoved.isEmpty()) {
                    toBeNotified.put(modelName, toBeRemoved);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        toBeNotified.values().forEach((set) -> {
            set.forEach(this::notifyListenersAboutRemovedElement);
        });
    }

    /**
     * Removes all meta-data for a given item
     *
     * @param modelName the model name
     * @param itemName the item name
     */
    public void removeMetadataByItemName(String modelName, String itemName) {
        Set<Metadata> toBeNotified;
        try {
            lock.writeLock().lock();
            toBeNotified = new HashSet<>();
            boolean notify = !isolatedModels.contains(modelName);
            Set<Metadata> mdSet = metadata.getOrDefault(modelName, new HashSet<>());
            Set<Metadata> toBeRemoved = mdSet.stream().filter(MetadataPredicates.ofItem(itemName)).collect(toSet());
            mdSet.removeAll(toBeRemoved);
            if (mdSet.isEmpty()) {
                metadata.remove(modelName);
                isolatedModels.remove(modelName);
            }
            if (notify && !toBeRemoved.isEmpty()) {
                toBeNotified.addAll(toBeRemoved);
            }
        } finally {
            lock.writeLock().unlock();
        }
        toBeNotified.forEach(this::notifyListenersAboutRemovedElement);
    }

    @Override
    public Collection<Metadata> getAll() {
        try {
            lock.readLock().lock();
            // Ignore isolated models
            Set<Metadata> set = metadata.keySet().stream().filter(name -> !isolatedModels.contains(name))
                    .map(name -> metadata.getOrDefault(name, Set.of())).flatMap(s -> s.stream()).collect(toSet());
            return Set.copyOf(set);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<Metadata> getAllFromModel(String modelName) {
        try {
            lock.readLock().lock();
            Set<Metadata> set = metadata.getOrDefault(modelName, Set.of());
            return Set.copyOf(set);
        } finally {
            lock.readLock().unlock();
        }
    }
}
