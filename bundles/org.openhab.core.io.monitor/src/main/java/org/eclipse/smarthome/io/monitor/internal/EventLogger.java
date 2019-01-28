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
package org.eclipse.smarthome.io.monitor.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
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

    private final Set<String> subscribedEventTypes = Collections.singleton(EventSubscriber.ALL_EVENT_TYPES);

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
