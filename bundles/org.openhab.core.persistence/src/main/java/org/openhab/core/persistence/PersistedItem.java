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

import java.time.Instant;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;

/**
 * This interface is used by persistence services to represent the full persisted state of an item, including the
 * previous state, and last update and change timestamps.
 * It can be used in restoring the full state of an item.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public interface PersistedItem extends HistoricItem {

    /**
     * returns the timestamp of the last state change of the persisted item
     *
     * @return the timestamp of the last state change of the item
     */
    @Nullable
    ZonedDateTime getLastStateChange();

    /**
     * returns the timestamp of the last state change of the persisted item
     *
     * @return the timestamp of the last state change of the item
     */
    @Nullable
    default Instant getLastStateChangeInstant() {
        ZonedDateTime lastStateChange = getLastStateChange();
        return lastStateChange != null ? lastStateChange.toInstant() : null;
    }

    /**
     * returns the last state of the item
     *
     * @return the last state
     */
    @Nullable
    State getLastState();
}
