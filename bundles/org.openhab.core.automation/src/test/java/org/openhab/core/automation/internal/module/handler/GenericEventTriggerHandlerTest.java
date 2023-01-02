/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Trigger;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.osgi.framework.BundleContext;

/**
 * Basic test cases for {@link GenericEventTriggerHandler}
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
class GenericEventTriggerHandlerTest {
    private @NonNullByDefault({}) GenericEventTriggerHandler handler;
    private @NonNullByDefault({}) Trigger moduleMock;
    private @NonNullByDefault({}) BundleContext contextMock;

    public Event createEvent(String topic, String source) {
        Event event = mock(Event.class);
        when(event.getTopic()).thenReturn(topic);
        when(event.getSource()).thenReturn(source);
        return event;
    }

    @BeforeEach
    public void setUp() {
        moduleMock = mock(Trigger.class);
        contextMock = mock(BundleContext.class);
    }

    @Test
    public void testTopicFilterIsGlobbed() {
        when(moduleMock.getConfiguration()).thenReturn(new Configuration(Map.of(GenericEventTriggerHandler.CFG_TOPIC,
                "openhab/items/*/command", GenericEventTriggerHandler.CFG_SOURCE, "",
                GenericEventTriggerHandler.CFG_TYPES, "", GenericEventTriggerHandler.CFG_PAYLOAD, "")));
        handler = new GenericEventTriggerHandler(moduleMock, contextMock);

        assertTrue(handler.apply(createEvent("openhab/items/myMotion1/command", "Source")));
    }

    @Test
    public void testsSourceFilterIsExactMatch() {
        when(moduleMock.getConfiguration()).thenReturn(new Configuration(
                Map.of(GenericEventTriggerHandler.CFG_TOPIC, "", GenericEventTriggerHandler.CFG_SOURCE, "ExactSource",
                        GenericEventTriggerHandler.CFG_TYPES, "", GenericEventTriggerHandler.CFG_PAYLOAD, "")));
        handler = new GenericEventTriggerHandler(moduleMock, contextMock);

        assertTrue(handler.apply(createEvent("openhab/items/myMotion1/command", "ExactSource")));
    }

    @Test
    public void testsSourceFilterDoesntMatchSubstring() {
        when(moduleMock.getConfiguration()).thenReturn(new Configuration(
                Map.of(GenericEventTriggerHandler.CFG_TOPIC, "", GenericEventTriggerHandler.CFG_SOURCE, "Source",
                        GenericEventTriggerHandler.CFG_TYPES, "", GenericEventTriggerHandler.CFG_PAYLOAD, "")));
        handler = new GenericEventTriggerHandler(moduleMock, contextMock);

        assertFalse(handler.apply(createEvent("openhab/items/myMotion1/command", "ExactSource")));
    }
}
