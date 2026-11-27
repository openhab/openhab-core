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
package org.openhab.core.persistence;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.State;

/**
 * A persistence service which can be used to store data from openHAB.
 * This must not necessarily be a local database, a persistence service
 * can also be cloud-based or a simply data-export facility (e.g.
 * for sending data to an IoT (Internet of Things) service.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Mark Herwege - Make default strategy to be only a configuration suggestion
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
     * <p>
     * Stores the historic item value. This allows the item, time and value to be specified.
     *
     * <p>
     * Adding data with the same time as an existing record should update the current record value rather than adding a
     * new record.
     *
     * <p>
     * Implementors SHOULD NOT rely on the default. It is provided solely for compatibility with existing
     * implementations which do not provide a store method which allows date and state to be specified.
     *
     * <p>
     * Implementors should keep in mind that all registered {@link PersistenceService}s are called synchronously. Hence
     * long running operations should be processed asynchronously. E.g. <code>store</code> adds things to a queue which
     * is processed by some asynchronous workers (Quartz Job, Thread, etc.).
     *
     * @param item the data to be stored
     * @param date the date of the record
     * @param state the state to be recorded
     */
    default void store(Item item, ZonedDateTime date, State state) {
        store(item);
    }

    /**
     * <p>
     * Stores the historic item value under a specified alias. This allows the item, time and value to be specified.
     *
     * <p>
     * Adding data with the same time as an existing record should update the current record value rather than adding a
     * new record.
     *
     * <p>
     * Implementors SHOULD NOT rely on the default. It is provided solely for compatibility with existing
     * implementations which do not provide a store method which allows date and state to be specified.
     *
     * <p>
     * Implementors should keep in mind that all registered {@link PersistenceService}s are called synchronously. Hence
     * long running operations should be processed asynchronously. E.g. <code>store</code> adds things to a queue which
     * is processed by some asynchronous workers (Quartz Job, Thread, etc.).
     *
     * @param item the data to be stored
     * @param date the date of the record
     * @param state the state to be recorded
     */
    default void store(Item item, ZonedDateTime date, State state, @Nullable String alias) {
        store(item, alias);
    }

    /**
     * Stores the current value of the given item.
     *
     * This method has been deprecated and {@link #store(Item, ZonedDateTime, State, String)} MUST be used instead.
     *
     * <p>
     * Implementors should keep in mind that all registered {@link PersistenceService}s are called synchronously. Hence
     * long running operations should be processed asynchronously. E.g. <code>store</code> adds things to a queue which
     * is processed by some asynchronous workers (Quartz Job, Thread, etc.).
     *
     * @param item the item which state should be persisted.
     */
    @Deprecated
    default void store(Item item) {
        store(item, ZonedDateTime.now(), item.getState(), null);
    }

    /**
     * <p>
     * Stores the current value of the given item under a specified alias.
     *
     * This method has been deprecated and {@link #store(Item, ZonedDateTime, State, String)} MUST be used instead.
     *
     * <p>
     * Implementors should keep in mind that all registered {@link PersistenceService}s are called synchronously. Hence
     * long running operations should be processed asynchronously. E.g. <code>store</code> adds things to a queue which
     * is processed by some asynchronous workers (Quartz Job, Thread, etc.).
     *
     * @param item the item which state should be persisted.
     * @param alias the alias under which the item should be persisted.
     */
    @Deprecated
    default void store(Item item, @Nullable String alias) {
        store(item, ZonedDateTime.now(), item.getState(), alias);
    }

    /**
     * Provides default persistence strategies that are used for all items if no user defined configuration is found.
     *
     * This method has been deprecated and {@link #getSuggestedStrategies()} should be used instead. These
     * persistence strategies are no longer applied automatically.
     *
     * @return The suggested persistence strategies
     */
    @Deprecated
    default List<PersistenceStrategy> getDefaultStrategies() {
        return List.of();
    }

    /**
     * Provides suggested persistence strategies that can be used in the UI as a suggestion for configuration.
     *
     * @return The suggested persistence strategies
     */
    default List<PersistenceStrategy> getSuggestedStrategies() {
        return getDefaultStrategies();
    }
}
