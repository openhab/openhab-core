/**
 * Copyright (c) 2015-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.events;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.openhab.core.compat1x.internal.TypeMapper;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class EventPublisherDelegate implements org.openhab.core.events.EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisherDelegate.class);

    private EventPublisher eventPublisher;

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    @Override
    public void sendCommand(String itemName, Command command) {
        // we do not offer synchronous sending of commands anymore
        postCommand(itemName, command);
    }

    @Override
    public void postCommand(String itemName, Command command) {
        org.eclipse.smarthome.core.types.Command eshCommand = (org.eclipse.smarthome.core.types.Command) TypeMapper
                .mapToESHType(command);
        if (eshCommand != null) {
            ItemCommandEvent event = ItemEventFactory.createCommandEvent(itemName, eshCommand);
            eventPublisher.post(event);
        } else if (command != null) {
            logger.warn("Compatibility layer could not convert {} of type {}.", command,
                    command.getClass().getSimpleName());
        } else {
            logger.warn("given command is NULL, couldn't post command for '{}'", itemName);
        }
    }

    @Override
    public void postUpdate(String itemName, State newState) {
        org.eclipse.smarthome.core.types.State eshState = (org.eclipse.smarthome.core.types.State) TypeMapper
                .mapToESHType(newState);
        if (eshState != null) {
            ItemStateEvent event = ItemEventFactory.createStateEvent(itemName, eshState);
            eventPublisher.post(event);
        } else if (newState != null) {
            logger.warn("Compatibility layer could not convert {} of type {}.", newState,
                    newState.getClass().getSimpleName());
        } else {
            logger.warn("given new state is NULL, couldn't post update for '{}'", itemName);
        }
    }
}
