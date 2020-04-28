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
package org.openhab.core.io.rest.sse.internal;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The specific information we need to hold for a SSE sink which tracks item state updates.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class SseSinkItemInfo {

    private final String connectionId = UUID.randomUUID().toString();
    private final Set<String> trackedItems = new CopyOnWriteArraySet<>();

    /**
     * Gets the connection identifier of this {@link SseSinkItemInfo}
     *
     * @return the connection id
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Updates the list of tracked items for a connection
     *
     * @param itemNames the item names to track
     */
    public void updateTrackedItems(Set<String> itemNames) {
        trackedItems.clear();
        trackedItems.addAll(itemNames);
    }

    public static Predicate<SseSinkItemInfo> hasConnectionId(String connectionId) {
        return info -> info.connectionId.equals(connectionId);
    }

    public static Predicate<SseSinkItemInfo> tracksItem(String itemName) {
        return info -> info.trackedItems.contains(itemName);
    }
}
