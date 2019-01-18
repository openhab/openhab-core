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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * {@link RegistryChangeListener} can be added to {@link Registry} services, to
 * listen for changes.
 *
 * @author Dennis Nobel - Initial contribution
 *
 * @param <E> type of the element in the registry
 */
@NonNullByDefault
public interface RegistryChangeListener<E> {

    /**
     * Notifies the listener that a single element has been added.
     *
     * @param element the element that has been added
     */
    void added(E element);

    /**
     * Notifies the listener that a single element has been removed.
     *
     * @param element the element that has been removed
     */
    void removed(E element);

    /**
     * Notifies the listener that a single element has been updated.
     *
     * @param element the new element
     * @param oldElement the element that has been updated
     */
    void updated(E oldElement, E element);

}
