/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A queryable persistence service which can be used to store and retrieve
 * data from openHAB. This is most likely some kind of database system.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Added getItems method
 */
@NonNullByDefault
public interface QueryablePersistenceService extends PersistenceService {

    /**
     * Queries the {@link PersistenceService} for historic data with a given {@link FilterCriteria}.
     * If the persistence service implementing this class supports using aliases for item names, the default
     * implementation of {@link #query(FilterCriteria, String)} should be overriden as well.
     *
     * @param filter the filter to apply to the query
     * @return a time series of items
     */
    Iterable<HistoricItem> query(FilterCriteria filter);

    /**
     * Queries the {@link PersistenceService} for historic data with a given {@link FilterCriteria}.
     * If the persistence service implementing this interface supports aliases, the default implementation should be
     * overriden to query the database with the aliased name.
     *
     * @param filter the filter to apply to the query
     * @param alias for item name in database
     * @return a time series of items
     */
    default Iterable<HistoricItem> query(FilterCriteria filter, @Nullable String alias) {
        // Default implementation ignores alias
        return query(filter);
    }

    /**
     * Returns a set of {@link PersistenceItemInfo} about items that are stored in the persistence service. This allows
     * the persistence service to return information about items that are no long available as an
     * {@link org.openhab.core.items.Item} in openHAB. If it is not possible to retrieve the information an empty set
     * should be returned.
     *
     * Note that implementations of this method may return an alias for an existing item if the database does not store
     * the mapping between item name and alias.
     *
     * @return a set of information about the persisted items
     */
    Set<PersistenceItemInfo> getItemInfo();
}
