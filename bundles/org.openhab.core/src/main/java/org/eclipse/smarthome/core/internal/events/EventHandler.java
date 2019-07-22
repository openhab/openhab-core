/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.internal.events;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.caller.Caller;
import org.eclipse.smarthome.core.caller.CallerFactory;
import org.eclipse.smarthome.core.caller.ExecutionConstraints;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFactory;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle openHAB events encapsulated by OSGi events.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class EventHandler implements AutoCloseable {

    private static final long EVENTSUBSCRIBER_EVENTHANDLING_MAX_MS = TimeUnit.SECONDS.toMillis(5);

    private final Logger logger = LoggerFactory.getLogger(EventHandler.class);

    private final Map<String, Set<EventSubscriber>> typedEventSubscribers;
    private final Map<String, EventFactory> typedEventFactories;

    private final Caller caller;

    /**
     * Create a new event handler.
     *
     * @param callerFactory the callerFactory
     * @param numOfThreads the number of threads to use to inform subscribers
     * @param typedEventSubscribers the event subscribers indexed by the event type
     * @param typedEventFactories the event factories indexed by the event type
     */
    public EventHandler(final CallerFactory callerFactory, final int numOfThreads,
            final Map<String, Set<EventSubscriber>> typedEventSubscribers,
            final Map<String, EventFactory> typedEventFactories) {
        this.caller = callerFactory.create("EventHandler", numOfThreads);
        this.typedEventSubscribers = typedEventSubscribers;
        this.typedEventFactories = typedEventFactories;
    }

    @Override
    public void close() {
        caller.close();
    }

    public void handleEvent(org.osgi.service.event.Event osgiEvent) {
        Object typeObj = osgiEvent.getProperty("type");
        Object payloadObj = osgiEvent.getProperty("payload");
        Object topicObj = osgiEvent.getProperty("topic");
        Object sourceObj = osgiEvent.getProperty("source");

        if (typeObj instanceof String && payloadObj instanceof String && topicObj instanceof String) {
            String typeStr = (String) typeObj;
            String payloadStr = (String) payloadObj;
            String topicStr = (String) topicObj;
            String sourceStr = (sourceObj instanceof String) ? (String) sourceObj : null;
            if (!typeStr.isEmpty() && !payloadStr.isEmpty() && !topicStr.isEmpty()) {
                handleEvent(typeStr, payloadStr, topicStr, sourceStr);
            }
        } else {
            logger.error(
                    "The handled OSGi event is invalid. Expect properties as string named 'type', 'payload' and 'topic'. "
                            + "Received event properties are: {}",
                    Arrays.toString(osgiEvent.getPropertyNames()));
        }
    }

    private void handleEvent(final String type, final String payload, final String topic,
            final @Nullable String source) {
        final EventFactory eventFactory = typedEventFactories.get(type);
        if (eventFactory == null) {
            logger.debug("Could not find an Event Factory for the event type '{}'.", type);
            return;
        }

        final Set<EventSubscriber> eventSubscribers = getEventSubscribers(type);
        if (eventSubscribers.isEmpty()) {
            return;
        }

        final Event eshEvent = createESHEvent(eventFactory, type, payload, topic, source);
        if (eshEvent == null) {
            return;
        }

        dispatchESHEvent(eventSubscribers, eshEvent);
    }

    private Set<EventSubscriber> getEventSubscribers(String eventType) {
        Set<EventSubscriber> eventTypeSubscribers = typedEventSubscribers.get(eventType);
        Set<EventSubscriber> allEventTypeSubscribers = typedEventSubscribers.get(EventSubscriber.ALL_EVENT_TYPES);

        Set<EventSubscriber> subscribers = new HashSet<>();
        if (eventTypeSubscribers != null) {
            subscribers.addAll(eventTypeSubscribers);
        }
        if (allEventTypeSubscribers != null) {
            subscribers.addAll(allEventTypeSubscribers);
        }
        return subscribers;
    }

    private @Nullable Event createESHEvent(final EventFactory eventFactory, final String type, final String payload,
            final String topic, final @Nullable String source) {
        Event eshEvent = null;
        try {
            eshEvent = eventFactory.createEvent(type, topic, payload, source);
        } catch (Exception e) {
            logger.error(
                    "Creation of ESH-Event failed, "
                            + "because one of the registered event factories has thrown an exception: {}",
                    e.getMessage(), e);
        }
        return eshEvent;
    }

    private synchronized void dispatchESHEvent(final Set<EventSubscriber> eventSubscribers, final Event event) {
        for (final EventSubscriber eventSubscriber : eventSubscribers) {
            EventFilter filter = eventSubscriber.getEventFilter();
            if (filter == null || filter.apply(event)) {
                logger.trace("Delegate event to subscriber ({}).", eventSubscriber.getClass());
                caller.execAsync(() -> eventSubscriber.receive(event),
                        new ExecutionConstraints(EVENTSUBSCRIBER_EVENTHANDLING_MAX_MS, () -> {
                            logger.warn("Dispatching event to subscriber '{}' takes more than {}ms.", eventSubscriber,
                                    EVENTSUBSCRIBER_EVENTHANDLING_MAX_MS);
                        })).exceptionally(ex -> {
                            logger.error("Dispatching/filtering event for subscriber '{}' failed: {}",
                                    EventSubscriber.class.getName(), ex.getMessage(), ex);
                            return Caller.VOID;
                        });
            } else {
                logger.trace("Skip event subscriber ({}) because of its filter.", eventSubscriber.getClass());
            }
        }
    }
}
