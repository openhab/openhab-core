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
package org.openhab.core.io.monitor.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component
public class EventLogger implements EventSubscriber {

    private final Map<String, Logger> eventLoggers = new HashMap<>();

    private final Set<String> subscribedEventTypes = Set.of(EventSubscriber.ALL_EVENT_TYPES);

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
        Logger logger = getLogger(event.getType());
        logger.trace("Received event of type '{}' under the topic '{}' with payload: '{}'", event.getType(),
                event.getTopic(), event.getPayload());
        logger.info("{}", event);
    }

    private Logger getLogger(String eventType) {
        String loggerName = "smarthome.event." + eventType;
        Logger logger = eventLoggers.get(loggerName);
        if (logger == null) {
            logger = LoggerFactory.getLogger(loggerName);
            eventLoggers.put(loggerName, logger);
        }
        return logger;
    }
}
