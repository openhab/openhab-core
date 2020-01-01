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
package org.openhab.core.internal.events;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.events.EventFactory;
import org.openhab.core.events.EventSubscriber;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle openHAB events encapsulated by OSGi events in a separate thread.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class ThreadedEventHandler implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(ThreadedEventHandler.class);

    private final Thread thread;

    private final Event notifyEvent = new Event("notify", Collections.emptyMap());
    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Create a new threaded event handler.
     *
     * @param typedEventSubscribers the event subscribers
     * @param typedEventFactories the event factories indexed by the event type
     */
    ThreadedEventHandler(Map<String, Set<EventSubscriber>> typedEventSubscribers,
            final Map<String, EventFactory> typedEventFactories) {
        thread = new Thread(() -> {
            try (EventHandler worker = new EventHandler(typedEventSubscribers, typedEventFactories)) {
                while (running.get()) {
                    try {
                        logger.trace("wait for event");
                        final Event event = queue.poll(1, TimeUnit.HOURS);
                        logger.trace("inspect event: {}", event);
                        if (event == null) {
                            logger.debug("Hey, you have really very few events.");
                        } else if (event == notifyEvent) {
                            // received an internal notification
                        } else {
                            worker.handleEvent(event);
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } catch (RuntimeException ex) {
                        logger.error("Error on event handling.", ex);
                    }
                }
            }
        }, "OH-OSGiEventManager");

    }

    void open() {
        thread.start();
    }

    @Override
    public void close() {
        running.set(false);
        queue.add(notifyEvent);
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void handleEvent(Event event) {
        queue.add(event);
    }
}
