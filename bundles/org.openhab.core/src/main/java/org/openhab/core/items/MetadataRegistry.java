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
package org.openhab.core.items;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.common.registry.Registry;

/**
 * The MetadataRegistry is the central place, where additional information about items is kept.
 *
 * Metadata can be supplied by {@link MetadataProvider}s, which can provision them from any source
 * they like and also dynamically remove or add data.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface MetadataRegistry extends Registry<Metadata, MetadataKey> {

    String INTERNAL_NAMESPACE_PREFIX = "_";

    /**
     * Determines whether the given namespace is internal.
     *
     * @param namespace the metadata namespace to check
     * @return {@code true} if the given namespace is internal, {@code false} otherwise
     */
    boolean isInternalNamespace(String namespace);

    /**
     * Provides all namespaces of a particular item
     *
     * @param itemname the name of the item for which the namespaces should be searched.
     */
    Collection<String> getAllNamespaces(String itemname);

    /**
     * Remove all metadata of a given item
     *
     * @param itemname the name of the item for which the metadata is to be removed.
     */
    void removeItemMetadata(String itemname);

    /**
     * Add element to metadata
     *
     * @param element the element to add (must not be null)
     * @return the added element or newly created object of the same type
     *
     * @throws UnsupportedOperationException if the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}
     * @throws IllegalStateException if no ManagedProvider is available
     */
    @Override
    Metadata add(Metadata element);

    /**
     * Update element in metadata
     *
     * @param element the element to update (must not be null)
     * @return returns the old element or null if no element with the same key
     *         exists
     *
     * @throws UnsupportedOperationException if the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}
     * @throws IllegalStateException if no ManagedProvider is available
     */
    @Override
    @Nullable
    Metadata update(Metadata element);

    /**
     * Remove element from metadata
     *
     * @param key the key of the element to remove (must not be null)
     * @return the removed element, or null if no element with the given key exists
     *
     * @throws UnsupportedOperationException if the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}
     * @throws IllegalStateException if no ManagedProvider is available
     */
    @Override
    @Nullable
    Metadata remove(MetadataKey key);
}
