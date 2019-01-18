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
package org.eclipse.smarthome.core.persistence;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.Item;

/**
 * A persistence service which can be used to store data from Eclipse SmartHome.
 * This must not necessarily be a local database, a persistence service
 * can also be cloud-based or a simply data-export facility (e.g.
 * for sending data to an IoT (Internet of Things) service.
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@NonNullByDefault
public interface PersistenceService {

    /**
     * Returns the id of this {@link PersistenceService}.
     * This id is used to uniquely identify the {@link PersistenceService}.
     *
     * @return the id to uniquely identify the {@link PersistenceService}.
     */
    String getId();

    /**
     * Returns the label of this {@link PersistenceService}.
     * This label provides a user friendly name for the {@link PersistenceService}.
     *
     * @param locale the language to return the label in, or null for the default language
     * @return the label of the {@link PersistenceService}.
     */
    String getLabel(@Nullable Locale locale);

    /**
     * Stores the current value of the given item.
     * <p>
     * Implementors should keep in mind that all registered {@link PersistenceService}s are called synchronously. Hence
     * long running operations should be processed asynchronously. E.g. <code>store</code> adds things to a queue which
     * is processed by some asynchronous workers (Quartz Job, Thread, etc.).
     *
     * @param item the item which state should be persisted.
     */
    void store(Item item);

    /**
     * <p>
     * Stores the current value of the given item under a specified alias.
     *
     * <p>
     * Implementors should keep in mind that all registered {@link PersistenceService}s are called synchronously. Hence
     * long running operations should be processed asynchronously. E.g. <code>store</code> adds things to a queue which
     * is processed by some asynchronous workers (Quartz Job, Thread, etc.).
     *
     * @param item the item which state should be persisted.
     * @param alias the alias under which the item should be persisted.
     */
    void store(Item item, String alias);
}
