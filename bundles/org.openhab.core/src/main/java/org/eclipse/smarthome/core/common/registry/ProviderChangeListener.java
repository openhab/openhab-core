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

/**
 * {@link ProviderChangeListener} can be added to {@link Provider} services, to
 * listen for changes. The {@link AbstractRegistry} implements a {@link ProviderChangeListener} and subscribes itself to
 * every added {@link Provider}.
 *
 * @author Dennis Nobel - Initial contribution
 *
 * @param <E>
 *            type of the element from the provider
 */
public interface ProviderChangeListener<E> {

    /**
     * Notifies the listener that a single element has been added.
     *
     * @param provider the provider that provides the element
     * @param element the element that has been added
     */
    void added(Provider<E> provider, E element);

    /**
     * Notifies the listener that a single element has been removed.
     *
     * @param provider the provider that provides the element
     * @param element the element that has been removed
     */
    void removed(Provider<E> provider, E element);

    /**
     * Notifies the listener that a single element has been updated.
     *
     * @param provider the provider that provides the element
     * @param element the element that has been updated
     */
    void updated(Provider<E> provider, E oldelement, E element);

}
