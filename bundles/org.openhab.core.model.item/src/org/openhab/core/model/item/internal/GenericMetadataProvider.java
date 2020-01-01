/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
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
 */
@Component(service = { MetadataProvider.class, GenericMetadataProvider.class })
@NonNullByDefault
public class GenericMetadataProvider extends AbstractProvider<Metadata> implements MetadataProvider {

    private final Set<Metadata> metadata = new HashSet<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    /**
     * Adds metadata to this provider
     *
     * @param bindingType
     * @param itemName
     * @param configuration
     */
    public void addMetadata(String bindingType, String itemName, String value,
            @Nullable Map<String, Object> configuration) {
        MetadataKey key = new MetadataKey(bindingType, itemName);
        Metadata md = new Metadata(key, value, configuration);
        try {
            lock.writeLock().lock();
            metadata.add(md);
        } finally {
            lock.writeLock().unlock();
        }
        notifyListenersAboutAddedElement(md);
    }

    /**
     * Removes all meta-data for a given item name
     *
     * @param itemName
     */
    public void removeMetadata(String itemName) {
        Set<Metadata> toBeRemoved;
        try {
            lock.writeLock().lock();
            toBeRemoved = metadata.stream().filter(MetadataPredicates.ofItem(itemName)).collect(toSet());
            metadata.removeAll(toBeRemoved);
        } finally {
            lock.writeLock().unlock();
        }
        for (Metadata m : toBeRemoved) {
            notifyListenersAboutRemovedElement(m);
        }
    }

    @Override
    public Collection<Metadata> getAll() {
        try {
            lock.readLock().lock();
            return Collections.unmodifiableSet(new HashSet<>(metadata));
        } finally {
            lock.readLock().unlock();
        }
    }

}
