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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.items.Item;

/**
 * A queryable persistence service which can be used to store and retrieve
 * data from Eclipse SmartHome. This is most likely some kind of database system.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Chris Jackson - Added getItems method
 */
@NonNullByDefault
public interface QueryablePersistenceService extends PersistenceService {

    /**
     * Queries the {@link PersistenceService} for data with a given filter criteria
     *
     * @param filter the filter to apply to the query
     * @return a time series of items
     */
    Iterable<HistoricItem> query(FilterCriteria filter);

    /**
     * Returns a list of items that are stored in the persistence service
     *
     * This is returned as a string to allow the persistence service to return items that are no long available as an
     * ESH {@link Item}.
     *
     * @return list of strings of item names contained in the store. Not null.
     */
    Set<PersistenceItemInfo> getItemInfo();
}
