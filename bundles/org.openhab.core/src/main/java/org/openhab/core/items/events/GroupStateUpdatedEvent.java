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
package org.openhab.core.items.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;

/**
 * {@link GroupStateUpdatedEvent}s can be used to deliver group item state updates through the openHAB event bus.
 * In contrast to the {@link GroupItemStateChangedEvent} it is always sent.
 * State events must be created with the {@link ItemEventFactory}.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class GroupStateUpdatedEvent extends ItemStateUpdatedEvent {

    /**
     * The group item state changed event type.
     */
    public static final String TYPE = GroupStateUpdatedEvent.class.getSimpleName();

    private final String memberName;

    protected GroupStateUpdatedEvent(String topic, String payload, String itemName, String memberName,
            State newItemState, @Nullable String source) {
        super(topic, payload, itemName, newItemState, source);
        this.memberName = memberName;
    }

    /**
     * @return the name of the changed group member
     */
    public String getMemberName() {
        return this.memberName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return String.format("Group '%s' updated to %s through %s", itemName, itemState, memberName);
    }
}
