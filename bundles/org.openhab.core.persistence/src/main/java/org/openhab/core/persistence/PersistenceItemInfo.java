/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class provides information about an item that is stored in a persistence service.
 * It is used to return information about the item to the system
 *
 * @author Chris Jackson - Initial contribution
 */
@NonNullByDefault
public interface PersistenceItemInfo {
    /**
     * Returns the item name.
     * It should be noted that the item name is as stored in the persistence service and as such may not be linked to an
     * item. This may be the case if the item was removed from the system, but data still exists in the persistence
     * service.
     *
     * @return Item name
     */
    String getName();

    /**
     * Returns a counter with the number of rows of data associated with the item
     * Note that this should be used as a guide to the amount of data and may note be 100% accurate. If accurate
     * information is required, the {@link QueryablePersistenceService#query} method should be used.
     *
     * @return count of the number of rows of data. May return null if the persistence service doesn't support this.
     */
    @Nullable
    Integer getCount();

    /**
     * Returns the earliest timestamp from data in the persistence database
     *
     * @return the earliest {@link Date} stored in the database. May return null if the persistence service doesn't
     *         support this.
     */
    @Nullable
    Date getEarliest();

    /**
     * Returns the latest timestamp from data in the persistence database
     *
     * @return the latest {@link Date} stored in the database. May return null if the persistence service doesn't
     *         support this.
     */
    @Nullable
    Date getLatest();
}
