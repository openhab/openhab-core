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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.types.State;

/**
 * A queryable persistence service which can be used to store and retrieve
 * data from openHAB. This is most likely some kind of database system.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Chris Jackson - Added getItems method
 * @author Mark Herwege - Added methods to retrieve lastUpdate, lastChange and lastState from persistence
 * @author Mark Herwege - Implement aliases
 */
@NonNullByDefault
public interface QueryablePersistenceService extends PersistenceService {

    /**
     * Queries the {@link PersistenceService} for historic data with a given {@link FilterCriteria}.
     * If the persistence service implementing this class supports using aliases for item names, the default
     * implementation of {@link #query(FilterCriteria, String)} should be overridden as well.
     *
     * @param filter the filter to apply to the query
     * @return a time series of items
     */
    Iterable<HistoricItem> query(FilterCriteria filter);

    /**
     * Queries the {@link PersistenceService} for historic data with a given {@link FilterCriteria}.
     * If the persistence service implementing this interface supports aliases and relies on item registry lookups, the
     * default implementation should be overridden to query the database with the aliased name.
     *
     * @param filter the filter to apply to the query
     * @param alias for item name in database
     * @return a time series of items
     */
    default Iterable<HistoricItem> query(FilterCriteria filter, @Nullable String alias) {
        // Default implementation changes the filter to have the alias as itemName and sets it back in the returned
        // result.
        // This gives correct results as long as the persistence service does not rely on a lookup in the item registry
        // (in which case the item will not be found).
        String itemName = filter.getItemName();
        if (itemName != null && alias != null) {
            FilterCriteria aliasFilter = new FilterCriteria(filter).setItemName(alias);
            return StreamSupport.stream(query(aliasFilter).spliterator(), false).map(hi -> new HistoricItem() {

                @Override
                public Instant getInstant() {
                    return hi.getInstant();
                }

                @Override
                public ZonedDateTime getTimestamp() {
                    return hi.getTimestamp();
                }

                @Override
                public State getState() {
                    return hi.getState();
                }

                @Override
                public String getName() {
                    return itemName;
                }
            }).collect(Collectors.toList());
        }
        return query(filter);
    }

    /**
     * Returns a set of {@link PersistenceItemInfo} about items that are stored in the persistence service. This allows
     * the persistence service to return information about items that are no longer available as an
     * {@link org.openhab.core.items.Item} in openHAB. If it is not possible to retrieve the information or it would be
     * too expensive to do so an {@link UnsupportedOperationException} should be thrown.
     *
     * Note that this method will return the names for items as stored in persistence. If aliases are used, the calling
     * method is responsible for mapping back to the real item name.
     *
     * @return a set of information about the persisted items
     * @throws UnsupportedOperationException if the operation is not supported or would be too expensive to perform
     */
    default Set<PersistenceItemInfo> getItemInfo() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("getItemInfo not supported for persistence service");
    }

    /**
     * Returns {@link PersistenceItemInfo} for an item stored in the persistence service. Null can be returned when the
     * item is not found in the persistence service. If it is not possible to retrieve the information or it would be
     * too expensive to do so an {@link UnsupportedOperationException} should be thrown.
     * The default implementation will query the persistence service for the last value in the persistence store and
     * set the latest timestamp, leaving the {@link PersistenceItemInfo} earliest timestamp and count equal to null.
     *
     * Note that the method should return the alias for the item in its response if an alias is being used.
     *
     * @param itemName the real openHAB name as it appears in the item registry
     * @param alias for item name in database or null if no alias defined
     * @return information about the persisted item
     * @throws UnsupportedOperationException if the operation is not supported or would be too expensive to perform
     */
    default @Nullable PersistenceItemInfo getItemInfo(String itemName, @Nullable String alias)
            throws UnsupportedOperationException {
        PersistedItem item = persistedItem(itemName, alias);
        if (item == null) {
            return null;
        }
        Date latest = Date.from(item.getInstant());
        Integer count = null; // If we found the item, we do not know how many are in the store
        return new PersistenceItemInfo() {

            @Override
            public String getName() {
                return alias != null ? alias : itemName;
            }

            @Override
            public @Nullable Integer getCount() {
                return count;
            }

            @Override
            public @Nullable Date getEarliest() {
                return null;
            }

            @Override
            public @Nullable Date getLatest() {
                return latest;
            }
        };
    }

    /**
     * Returns a {@link PersistedItem} representing the persisted state, last update and change timestamps and previous
     * persisted state. This can be used to restore the full state of an item.
     * The default implementation only queries the service for the last persisted state and does not attempt to look
     * further back to find the last change timestamp and previous persisted state. This avoids potential performance
     * issues if this method is not overriden in the specific persistence service. Persistence services should override
     * this default implementation with a more complete and efficient algorithm.
     *
     * @param itemName name of item
     * @param alias alias of item
     * @return a {@link PersistedItem} or null if the item has not been persisted
     */
    default @Nullable PersistedItem persistedItem(String itemName, @Nullable String alias) {
        State currentState;
        Instant lastUpdate;

        FilterCriteria filter = new FilterCriteria().setItemName(itemName).setEndDate(ZonedDateTime.now())
                .setOrdering(Ordering.DESCENDING).setPageSize(1).setPageNumber(0);
        Iterator<HistoricItem> it = query(filter, alias).iterator();
        if (it.hasNext()) {
            HistoricItem historicItem = it.next();
            currentState = historicItem.getState();
            lastUpdate = historicItem.getInstant();
        } else {
            return null;
        }

        final State state = currentState;
        final Instant lastStateUpdate = lastUpdate;

        return new PersistedItem() {
            @Override
            public Instant getInstant() {
                return lastStateUpdate;
            }

            @Override
            public ZonedDateTime getTimestamp() {
                return lastStateUpdate.atZone(ZoneId.systemDefault());
            }

            @Override
            public State getState() {
                return state;
            }

            @Override
            public String getName() {
                return itemName;
            }

            @Override
            public @Nullable ZonedDateTime getLastStateChange() {
                return null;
            }

            @Override
            public @Nullable State getLastState() {
                return null;
            }
        };
    }
}
