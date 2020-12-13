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

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyMarkerFilter;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.ReadyService.ReadyTracker;
import org.openhab.core.service.StartLevelService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component
public class EventLogger implements EventSubscriber, ReadyTracker {

    private final Map<String, Logger> eventLoggers = new HashMap<>();
    private final Set<String> subscribedEventTypes = Set.of(EventSubscriber.ALL_EVENT_TYPES);
    private final ReadyService readyService;

    private boolean loggingActive = false;

    @Activate
    public EventLogger(@Reference ReadyService readyService) {
        this.readyService = readyService;
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(StartLevelService.STARTLEVEL_MARKER_TYPE)
                .withIdentifier(Integer.toString(StartLevelService.STARTLEVEL_RULES)));
    }

    @Deactivate
    protected void deactivate() {
        readyService.unregisterTracker(this);
    }

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
        if (loggingActive) {
            logger.info("{}", event);
        }
    }

    private Logger getLogger(String eventType) {
        String loggerName = "openhab.event." + eventType;
        Logger logger = eventLoggers.get(loggerName);
        if (logger == null) {
            logger = LoggerFactory.getLogger(loggerName);
            eventLoggers.put(loggerName, logger);
        }
        return logger;
    }

    @Override
    public void onReadyMarkerAdded(@NonNull ReadyMarker readyMarker) {
        loggingActive = true;
    }

    @Override
    public void onReadyMarkerRemoved(@NonNull ReadyMarker readyMarker) {
        loggingActive = false;
    }
}
