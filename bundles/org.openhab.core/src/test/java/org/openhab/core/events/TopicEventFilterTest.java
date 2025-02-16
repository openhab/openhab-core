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
package org.openhab.core.events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * {@link TopicEventFilterTest} tests the {@link TopicEventFilter}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class TopicEventFilterTest {
    public Event createEvent(String topic) {
        Event event = mock(Event.class);
        when(event.getTopic()).thenReturn(topic);
        return event;
    }

    @Test
    public void testSingle() {
        EventFilter filter = new TopicEventFilter("openhab/items/.*/.*");
        assertTrue(filter.apply(createEvent("openhab/items/test/command")));
        assertFalse(filter.apply(createEvent("somewhereElse")));
        assertFalse(filter.apply(createEvent("preopenhab/items/test/state")));

        filter = new TopicEventFilter("openhab/items/test/command");
        assertTrue(filter.apply(createEvent("openhab/items/test/command")));
        assertFalse(filter.apply(createEvent("openhab/items/test/state")));
    }

    @Test
    public void testMultiple() {
        EventFilter filter = new TopicEventFilter(List.of("openhab/items/.*/.*", "openhab/things/.*/.*"));
        assertTrue(filter.apply(createEvent("openhab/items/test/command")));
        assertTrue(filter.apply(createEvent("openhab/things/test/added")));
        assertFalse(filter.apply(createEvent("somewhereElse")));
        assertFalse(filter.apply(createEvent("preopenhab/items/test/command")));

        filter = new TopicEventFilter(List.of("openhab/items/test/command", "openhab/things/test/added"));
        assertTrue(filter.apply(createEvent("openhab/items/test/command")));
        assertTrue(filter.apply(createEvent("openhab/things/test/added")));
        assertFalse(filter.apply(createEvent("openhab/items/test/state")));
        assertFalse(filter.apply(createEvent("openhab/things/test/removed")));
    }
}
