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

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;
import java.util.Hashtable;

import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
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
public class OSGiEventPublisher implements EventPublisher {

    private EventAdmin osgiEventAdmin;

    @Reference
    protected void setEventAdmin(EventAdmin eventAdmin) {
        this.osgiEventAdmin = eventAdmin;
    }

    protected void unsetEventAdmin(EventAdmin eventAdmin) {
        this.osgiEventAdmin = null;
    }

    @Override
    public void post(final Event event) throws IllegalArgumentException, IllegalStateException {
        EventAdmin eventAdmin = this.osgiEventAdmin;
        assertValidArgument(event);
        assertValidState(eventAdmin);
        postAsOSGiEvent(eventAdmin, event);
    }

    private void postAsOSGiEvent(final EventAdmin eventAdmin, final Event event) throws IllegalStateException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    Dictionary<String, Object> properties = new Hashtable<>(3);
                    properties.put("type", event.getType());
                    properties.put("payload", event.getPayload());
                    properties.put("topic", event.getTopic());
                    if (event.getSource() != null) {
                        properties.put("source", event.getSource());
                    }
                    eventAdmin.postEvent(new org.osgi.service.event.Event("smarthome", properties));
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            throw new IllegalStateException("Cannot post the event via the event bus. Error message: " + e.getMessage(),
                    e);
        }
    }

    private void assertValidArgument(Event event) throws IllegalArgumentException {
        String errorMsg = "The %s of the 'event' argument must not be null or empty.";
        String value;

        if (event == null) {
            throw new IllegalArgumentException("Argument 'event' must not be null.");
        }
        if ((value = event.getType()) == null || value.isEmpty()) {
            throw new IllegalArgumentException(String.format(errorMsg, "type"));
        }
        if ((value = event.getPayload()) == null || value.isEmpty()) {
            throw new IllegalArgumentException(String.format(errorMsg, "payload"));
        }
        if ((value = event.getTopic()) == null || value.isEmpty()) {
            throw new IllegalArgumentException(String.format(errorMsg, "topic"));
        }
    }

    private void assertValidState(EventAdmin eventAdmin) throws IllegalStateException {
        if (eventAdmin == null) {
            throw new IllegalStateException("The event bus module is not available!");
        }
    }
}
