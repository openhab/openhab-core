/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;

/**
 * The {@link OSGiEventPublisher} provides an OSGi based default implementation of the openHAB event
 * publisher.
 *
 * Events are send in an asynchronous way via OSGi Event Admin mechanism.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Simon Kaufmann - separated from OSGiEventManager
 */
@Component
@NonNullByDefault
public class OSGiEventPublisher implements EventPublisher {
    protected static final String SOURCE = "source";
    protected static final String TOPIC = "topic";
    protected static final String PAYLOAD = "payload";
    protected static final String TYPE = "type";

    private final EventAdmin osgiEventAdmin;

    @Activate
    public OSGiEventPublisher(final @Reference @Nullable EventAdmin eventAdmin) {
        if (eventAdmin == null) {
            throw new IllegalStateException("The event bus module is not available!");
        }
        this.osgiEventAdmin = eventAdmin;
    }

    @Override
    public void post(final Event event) throws IllegalArgumentException, IllegalStateException {
        assertValidArgument(event);
        postAsOSGiEvent(osgiEventAdmin, event);
    }

    private void postAsOSGiEvent(final EventAdmin eventAdmin, final Event event) throws IllegalStateException {
        try {
            Dictionary<String, Object> properties = new Hashtable<>(3);
            properties.put(TYPE, event.getType());
            properties.put(PAYLOAD, event.getPayload());
            properties.put(TOPIC, event.getTopic());
            String source = event.getSource();
            if (source != null) {
                properties.put(SOURCE, source);
            }
            eventAdmin.postEvent(new org.osgi.service.event.Event("openhab", properties));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot post the event via the event bus. Error message: " + e.getMessage(),
                    e);
        }
    }

    private void assertValidArgument(Event event) throws IllegalArgumentException {
        String errorMsg = "The %s of the 'event' argument must not be null or empty.";

        if (event.getType().isEmpty()) {
            throw new IllegalArgumentException(errorMsg.formatted(TYPE));
        } else if (event.getPayload().isEmpty()) {
            throw new IllegalArgumentException(errorMsg.formatted(PAYLOAD));
        } else if (event.getTopic().isEmpty()) {
            throw new IllegalArgumentException(errorMsg.formatted(TOPIC));
        }
    }
}
