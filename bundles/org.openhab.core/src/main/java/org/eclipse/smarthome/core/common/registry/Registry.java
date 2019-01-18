/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.common.registry;

import java.util.Collection;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Registry} interface represents a registry for elements of the type
 * E. The concrete sub interfaces are registered as OSGi services.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Victor Toni - provide elements as {@link Stream}
 * @author Kai Kreuzer - added null annotations
 *
 * @param <E> type of the elements in the registry
 */
public interface Registry<E extends Identifiable<K>, K> {

    /**
     * Adds a {@link RegistryChangeListener} to the registry.
     *
     * @param listener registry change listener
     */
    void addRegistryChangeListener(@NonNull RegistryChangeListener<E> listener);

    /**
     * Returns a collection of all elements in the registry.
     *
     * @return collection of all elements in the registry
     */
    @NonNull
    Collection<@NonNull E> getAll();

    /**
     * Returns a stream of all elements in the registry.
     *
     * @return stream of all elements in the registry
     */
    Stream<E> stream();

    /**
     * This method retrieves a single element from the registry.
     *
     * @param key key of the element
     * @return element or null if no element was found
     */
    public @Nullable E get(K key);

    /**
     * Removes a {@link RegistryChangeListener} from the registry.
     *
     * @param listener registry change listener
     */
    void removeRegistryChangeListener(@NonNull RegistryChangeListener<E> listener);

    /**
     * Adds the given element to the according {@link ManagedProvider}.
     *
     * @param element element to be added (must not be null)
     * @return the added element or newly created object of the same type
     * @throws IllegalStateException if no ManagedProvider is available
     */
    public @NonNull E add(@NonNull E element);

    /**
     * Updates the given element at the according {@link ManagedProvider}.
     *
     * @param element element to be updated (must not be null)
     * @return returns the old element or null if no element with the same key
     *         exists
     * @throws IllegalStateException if no ManagedProvider is available
     */
    public @Nullable E update(@NonNull E element);

    /**
     * Removes the given element from the according {@link ManagedProvider}.
     *
     * @param key key of the element (must not be null)
     * @return element that was removed, or null if no element with the given
     *         key exists
     * @throws IllegalStateException if no ManagedProvider is available
     */
    public @Nullable E remove(@NonNull K key);
}
