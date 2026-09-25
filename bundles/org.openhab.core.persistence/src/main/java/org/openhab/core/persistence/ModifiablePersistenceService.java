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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class provides an interface to the a {@link PersistenceService} to allow data to be stored
 * at a specific time. This allows bindings that interface to devices that store data internally,
 * and then periodically provide it to the server to be accommodated.
 *
 * @author Chris Jackson - Initial contribution
 * @author Mark Herwege - Implement aliases
 */
@NonNullByDefault
public interface ModifiablePersistenceService extends QueryablePersistenceService {
    /**
     * Removes data associated with an item from a persistence service.
     * If all data is removed for the specified item, the persistence service should free any resources associated with
     * the item (e.g. remove any tables or delete files from the storage).
     * If the persistence service implementing this method supports aliases for item names, the default implementation
     * of {@link #remove(FilterCriteria, String)} should be overriden as well.
     *
     * @param filter the filter to apply to the data removal. ItemName can not be null.
     * @return true if the query executed successfully
     * @throws IllegalArgumentException if item name is null.
     */
    boolean remove(FilterCriteria filter) throws IllegalArgumentException;

    /**
     * Removes data associated with an item from a persistence service.
     * If all data is removed for the specified item, the persistence service should free any resources associated with
     * the item (e.g. remove any tables or delete files from the storage).
     * Persistence services supporting aliases, and relying on lookups in the item registry, should override the default
     * implementation from this interface.
     *
     * @param filter the filter to apply to the data removal. ItemName can not be null.
     * @param alias for item name in database
     * @return true if the query executed successfully
     * @throws IllegalArgumentException if item name is null.
     */
    default boolean remove(FilterCriteria filter, @Nullable String alias) throws IllegalArgumentException {
        // Default implementation changes the filter to have the alias as itemName.
        // This gives correct results as long as the persistence service does not rely on a lookup in the item registry
        // (in which case the item will not be found).
        if (alias != null) {
            FilterCriteria aliasFilter = new FilterCriteria(filter).setItemName(alias);
            return remove(aliasFilter);
        }
        return remove(filter);
    }
}
