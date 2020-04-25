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

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link Provider} provides elements of a determined type and the subinterfaces
 * are registered as OSGi services. Providers are tracked by {@link Registry} services, which collect all elements from
 * different providers of the same
 * type.
 *
 * @author Dennis Nobel - Initial contribution
 *
 * @param <E> type of the provided elements
 */
@NonNullByDefault
public interface Provider<@NonNull E> {

    /**
     * Adds a {@link ProviderChangeListener} which must be notified if there are
     * changes concerning the elements provided by the {@link Provider}.
     *
     * @param listener the listener to be added
     */
    void addProviderChangeListener(ProviderChangeListener<E> listener);

    /**
     * Returns a collection of all elements.
     *
     * @return collection of all elements
     */
    Collection<E> getAll();

    /**
     * Removes a {@link ProviderChangeListener}.
     *
     * @param listener the listener to be removed.
     */
    void removeProviderChangeListener(ProviderChangeListener<E> listener);
}
