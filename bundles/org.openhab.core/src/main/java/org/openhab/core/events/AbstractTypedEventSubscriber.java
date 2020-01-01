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
package org.openhab.core.events;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link AbstractTypedEventSubscriber} is an abstract implementation of the {@link EventSubscriber} interface which
 * helps to subscribe to a specific event type. To receive an event - casted to the specific event type - the
 * {@link #receiveTypedEvent(T)} method must be implemented. This implementation provides no event filter. To filter
 * events based on the topic or some content the {@link #getEventFilter()} method can be overridden.
 *
 * @author Stefan Bußweiler - Initial contribution
 *
 * @param <T> The specific event type this class subscribes to.
 */
@NonNullByDefault
public abstract class AbstractTypedEventSubscriber<T extends Event> implements EventSubscriber {

    private final Set<String> subscribedEventTypes;

    /**
     * Constructs a new typed event subscriber. Must be called in the subclass.
     *
     * @param eventType the event type
     */
    protected AbstractTypedEventSubscriber(String eventType) {
        this.subscribedEventTypes = Collections.singleton(eventType);
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return subscribedEventTypes;
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(Event event) {
        receiveTypedEvent((T) event);
    }

    /**
     * Callback method for receiving typed events of type T.
     *
     * @param event the received event
     */
    protected abstract void receiveTypedEvent(T event);

}
