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
package org.openhab.core.items.events;

import org.openhab.core.types.State;

/**
 * {@link GroupItemStateChangedEvent}s can be used to deliver group item state changes through the openHAB event bus. In
 * contrast to the {@link GroupItemStateEvent} the {@link GroupItemStateChangedEvent} is only sent if the state changed.
 * State events must be created with the {@link ItemEventFactory}.
 *
 * @author Christoph Knauf - Initial contribution
 */
public class GroupItemStateChangedEvent extends ItemStateChangedEvent {

    /**
     * The group item state changed event type.
     */
    public static final String TYPE = GroupItemStateChangedEvent.class.getSimpleName();

    private final String memberName;

    protected GroupItemStateChangedEvent(String topic, String payload, String itemName, String memberName,
            State newItemState, State oldItemState) {
        super(topic, payload, itemName, newItemState, oldItemState);
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
        return String.format("%s through %s", super.toString(), memberName);
    }

}
