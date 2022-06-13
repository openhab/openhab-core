/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.items.events;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;

@NonNullByDefault
public class ItemHistoricStateEvent extends ItemEvent {

    private static DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME; // ofLocalizedDateTime(FormatStyle.SHORT);

    /**
     * The item state changed event type.
     */
    public static final String TYPE = ItemHistoricStateEvent.class.getSimpleName();

    protected final State itemState;

    protected final ZonedDateTime dateTime;

    /**
     * Constructs a new item state event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param itemName the item name
     * @param itemState the item state
     * @param source the source, can be null
     */
    protected ItemHistoricStateEvent(String topic, String payload, String itemName, State itemState,
            ZonedDateTime dateTime, @Nullable String source) {
        super(topic, payload, itemName, source);
        this.itemState = itemState;
        this.dateTime = dateTime;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the item's historic state.
     *
     * @return the item's historic state
     */
    public State getItemState() {
        return itemState;
    }

    /**
     * Gets the date time.
     *
     * @return the date time
     */
    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    @Override
    public String toString() {
        return String.format("Item '%s' state at %s set to %s", itemName,
                dateTime == null ? "null" : dateTime.format(DATETIME_FORMAT), itemState);
    }
}
