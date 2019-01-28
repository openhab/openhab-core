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
package org.eclipse.smarthome.core.items.events;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;

/**
 * <p>
 * The {@link AbstractItemEventSubscriber} defines an abstract implementation of the {@link EventSubscriber} interface
 * for receiving {@link ItemStateEvent}s and {@link ItemCommandEvent}s from the Eclipse SmartHome event bus.
 *
 * A subclass can implement the methods {@link #receiveUpdate(ItemStateEvent)} and
 * {@link #receiveCommand(ItemCommandEvent)} in order to receive and handle such events.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public abstract class AbstractItemEventSubscriber implements EventSubscriber {

    private final Set<String> subscribedEventTypes = Collections
            .unmodifiableSet(Stream.of(ItemStateEvent.TYPE, ItemCommandEvent.TYPE).collect(Collectors.toSet()));

    @Override
    public Set<String> getSubscribedEventTypes() {
        return subscribedEventTypes;
    }

    @Override
    public EventFilter getEventFilter() {
        return null;
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ItemStateEvent) {
            receiveUpdate((ItemStateEvent) event);
        } else if (event instanceof ItemCommandEvent) {
            receiveCommand((ItemCommandEvent) event);
        }
    }

    /**
     * Callback method for receiving item command events from the Eclipse SmartHome event bus.
     *
     * @param commandEvent the item command event
     */
    protected void receiveCommand(ItemCommandEvent commandEvent) {
        // Default implementation: do nothing.
        // Can be implemented by subclass in order to handle item commands.
    }

    /**
     * Callback method for receiving item update events from the Eclipse SmartHome event bus.
     *
     * @param updateEvent the item update event
     */
    protected void receiveUpdate(ItemStateEvent updateEvent) {
        // Default implementation: do nothing.
        // Can be implemented by subclass in order to handle item updates.
    }

}
