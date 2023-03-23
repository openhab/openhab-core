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
package org.openhab.core.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * {@link TopicGlobEventFilterTest} tests the {@link org.openhab.core.events.TopicGlobEventFilter}.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class TopicGlobEventFilterTest {
    public Event createEvent(String topic) {
        Event event = mock(Event.class);
        when(event.getTopic()).thenReturn(topic);
        return event;
    }

    @Test
    public void testBasic() throws Exception {
        var filter = new TopicGlobEventFilter("openhab/**");
        assertTrue(filter.apply(createEvent("openhab/items/a")));
        assertFalse(filter.apply(createEvent("somewhereElse")));
        assertFalse(filter.apply(createEvent("preopenhab/items/apost")));

        // * does not match multiple sub-directories
        filter = new TopicGlobEventFilter("openhab/*");
        assertFalse(filter.apply(createEvent("openhab/items/a")));

        // * can be used in the middle of a path component
        filter = new TopicGlobEventFilter("openhab/it*s/*");
        assertTrue(filter.apply(createEvent("openhab/items/a")));
        assertFalse(filter.apply(createEvent("openhab/things/a")));
    }
}
