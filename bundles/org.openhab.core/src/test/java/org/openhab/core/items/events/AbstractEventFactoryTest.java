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
package org.openhab.core.items.events;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.openhab.core.events.AbstractEventFactory;

/**
 * {@link AbstractEventFactoryTests} tests the {@link AbstractEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public class AbstractEventFactoryTest {
    private final ItemEventFactory factory = new ItemEventFactory();

    private static final String SOURCE = "binding:type:id:channel";
    private static final String EVENT_TYPE = "SOME_EVENT_TYPE";
    private static final String EVENT_TOPIC = "openhab/some/topic";
    private static final String EVENT_PAYLOAD = "{\"some\":\"payload\"}";

    @Test
    public void testExceptionForNotSupportedEventTypes() throws Exception {
        try {
            factory.createEvent("SOME_NOT_SUPPORTED_TYPE", EVENT_TOPIC, EVENT_PAYLOAD, SOURCE);
            fail("IllegalArgumentException expected!");
        } catch (IllegalArgumentException e) {
            assertEquals("The event type 'SOME_NOT_SUPPORTED_TYPE' is not supported by this factory.", e.getMessage());
        }
    }

    @Test
    public void testArgumentValidation() throws Exception {
        try {
            factory.createEvent("", EVENT_TOPIC, EVENT_PAYLOAD, null);
            fail("IllegalArgumentException expected!");
        } catch (IllegalArgumentException e) {
            assertEquals("The argument 'eventType' must not be null or empty.", e.getMessage());
        }
        try {
            factory.createEvent(EVENT_TYPE, "", EVENT_PAYLOAD, null);
            fail("IllegalArgumentException expected!");
        } catch (IllegalArgumentException e) {
            assertEquals("The argument 'topic' must not be null or empty.", e.getMessage());
        }
        try {
            factory.createEvent(EVENT_TYPE, EVENT_TOPIC, "", null);
            fail("IllegalArgumentException expected!");
        } catch (IllegalArgumentException e) {
            assertEquals("The argument 'payload' must not be null or empty.", e.getMessage());
        }
    }
}
