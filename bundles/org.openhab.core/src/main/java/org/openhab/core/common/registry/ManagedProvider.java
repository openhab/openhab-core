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
package org.openhab.core.common.registry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link ManagedProvider} is a specific {@link Provider} that enables to
 * add, remove and update elements at runtime.
 *
 * @author Dennis Nobel - Initial contribution
 *
 * @param <E>
 *            type of the element
 * @param <K>
 *            type of the element key
 */
@NonNullByDefault
public interface ManagedProvider<@NonNull E extends Identifiable<K>, @NonNull K> extends Provider<E> {

    /**
     * Adds an element.
     *
     * @param element element to be added
     */
    void add(E element);

    /**
     * Removes an element and returns the removed element.
     *
     * @param key key of the element that should be removed
     * @return element that was removed, or null if no element with the given
     *         key exists
     */
    @Nullable
    E remove(K key);

    /**
     * Updates an element.
     *
     * @param element element to be updated
     * @return returns the old element or null if no element with the same key
     *         exists
     */
    @Nullable
    E update(E element);

    /**
     * Returns an element for the given key or null if no element for the given
     * key exists.
     *
     * @param key key
     * @return returns element or null, if no element for the given key exists
     */
    @Nullable
    E get(K key);
}
