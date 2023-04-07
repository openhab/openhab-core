/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.persistence.extensions;

import java.time.ZonedDateTime;

import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.types.State;

/**
 * This class wraps a HistoricItem to reset the timestamp.
 *
 * @author Florian Binder - Initial contribution
 */
public class RetimedHistoricItem implements HistoricItem {

    private final HistoricItem originItem;
    private final ZonedDateTime timestamp;

    public RetimedHistoricItem(HistoricItem originItem, ZonedDateTime timestamp) {
        this.originItem = originItem;
        this.timestamp = timestamp;
    }

    @Override
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public State getState() {
        return originItem.getState();
    }

    @Override
    public String getName() {
        return originItem.getName();
    }

    @Override
    public String toString() {
        return "RetimedHistoricItem [originItem=" + originItem + ", timestamp=" + timestamp + "]";
    }
}
