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
package org.eclipse.smarthome.core.events;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link EventSubscriber} defines the callback interface for receiving events from
 * the Eclipse SmartHome event bus.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public interface EventSubscriber {

    /**
     * The constant {@link #ALL_EVENT_TYPES} must be returned by the {@link #getSubscribedEventTypes()} method, if the
     * event subscriber should subscribe to all event types.
     */
    public static String ALL_EVENT_TYPES = "ALL";

    /**
     * Gets the event types to which the event subscriber is subscribed to.
     *
     * @return subscribed event types (not null)
     */
    Set<String> getSubscribedEventTypes();

    /**
     * Gets an {@link EventFilter} in order to receive specific events if the filter applies. If there is no
     * filter all subscribed event types are received.
     *
     * @return the event filter, or null
     */
    @Nullable
    EventFilter getEventFilter();

    /**
     * Callback method for receiving {@link Event}s from the Eclipse SmartHome event bus. This method is called for
     * every event where the event subscriber is subscribed to and the event filter applies.
     *
     * @param event the received event (not null)
     */
    void receive(Event event);
}
