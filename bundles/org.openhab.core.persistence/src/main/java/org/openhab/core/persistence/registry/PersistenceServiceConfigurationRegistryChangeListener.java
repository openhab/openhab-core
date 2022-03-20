/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.persistence.registry;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link PersistenceServiceConfigurationRegistryChangeListener} is an interface that can be implemented by services
 * that need to listen to the {@link PersistenceServiceConfigurationRegistry} when more than one registry with different
 * types is used.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface PersistenceServiceConfigurationRegistryChangeListener {
    /**
     * Notifies the listener that a single element has been added.
     *
     * @param element the element that has been added
     */
    void added(PersistenceServiceConfiguration element);

    /**
     * Notifies the listener that a single element has been removed.
     *
     * @param element the element that has been removed
     */
    void removed(PersistenceServiceConfiguration element);

    /**
     * Notifies the listener that a single element has been updated.
     *
     * @param element the new element
     * @param oldElement the element that has been updated
     */
    void updated(PersistenceServiceConfiguration oldElement, PersistenceServiceConfiguration element);
}
