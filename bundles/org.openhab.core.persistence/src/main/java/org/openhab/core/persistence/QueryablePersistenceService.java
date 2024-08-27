/*
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

import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

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
     * implementation of {@link #query(FilterCriteria, String)} should be overriden as well.
     *
     * @param filter the filter to apply to the query
     * @return a time series of items
     */
    Iterable<HistoricItem> query(FilterCriteria filter);

    /**
     * Queries the {@link PersistenceService} for historic data with a given {@link FilterCriteria}.
     * If the persistence service implementing this interface supports aliases and relies on item registry lookups, the
     * default implementation should be overriden to query the database with the aliased name.
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
     * the persistence service to return information about items that are no long available as an
     * {@link org.openhab.core.items.Item} in openHAB. If it is not possible to retrieve the information an empty set
     * should be returned.
     *
     * Note that implementations for method callers that this method may return an alias for an existing item if the
     * database does not store the mapping between item name and alias or the reverse mapping is not implemented in the
     * persistence service.
     *
     * @return a set of information about the persisted items
     */
    Set<PersistenceItemInfo> getItemInfo();

    /**
     * Returns a {@link PersistedItem} representing the persisted state, last update and change timestamps and previous
     * persisted state. This can be used to restore the full state of an item.
     * The default implementation queries the service and iterates backward to find the last change and previous
     * persisted state. Persistence services can override this default implementation with a more specific or efficient
     * algorithm.
     *
     * @return a {@link PersistedItem} or null if the item has not been persisted
     */
    default @Nullable PersistedItem persistedItem(String itemName) {
        State currentState = UnDefType.NULL;
        State previousState = null;
        ZonedDateTime lastUpdate = null;
        ZonedDateTime lastChange = null;

        int pageNumber = 0;
        FilterCriteria filter = new FilterCriteria().setItemName(itemName).setEndDate(ZonedDateTime.now())
                .setOrdering(Ordering.DESCENDING).setPageSize(1000).setPageNumber(pageNumber);
        Iterable<HistoricItem> items = query(filter);
        while (items != null) {
            Iterator<HistoricItem> it = items.iterator();
            int itemCount = 0;
            if (UnDefType.NULL.equals(currentState) && it.hasNext()) {
                HistoricItem historicItem = it.next();
                itemCount++;
                currentState = historicItem.getState();
                lastUpdate = historicItem.getTimestamp();
                lastChange = lastUpdate;
            }
            while (it.hasNext()) {
                HistoricItem historicItem = it.next();
                itemCount++;
                if (!historicItem.getState().equals(currentState)) {
                    previousState = historicItem.getState();
                    items = null;
                    break;
                }
                lastChange = historicItem.getTimestamp();
            }
            if (itemCount == filter.getPageSize()) {
                filter.setPageNumber(++pageNumber);
                items = query(filter);
            } else {
                items = null;
            }
        }

        if (UnDefType.NULL.equals(currentState) || lastUpdate == null) {
            return null;
        }

        final State state = currentState;
        final ZonedDateTime lastStateUpdate = lastUpdate;
        final State lastState = previousState;
        // if we don't find a previous state in persistence, we also don't know when it last changed
        final ZonedDateTime lastStateChange = previousState != null ? lastChange : null;

        return new PersistedItem() {

            @Override
            public ZonedDateTime getTimestamp() {
                return lastStateUpdate;
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
                return lastStateChange;
            }

            @Override
            public @Nullable State getLastState() {
                return lastState;
            }
        };
    }
}
