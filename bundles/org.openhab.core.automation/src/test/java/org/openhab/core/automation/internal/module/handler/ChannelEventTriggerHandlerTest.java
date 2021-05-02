/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.events.ThingEventFactory;
import org.osgi.framework.BundleContext;

/**
 * Basic test cases for {@link ChannelEventTriggerHandler}
 *
 * @author Thomas Weißschuh - Initial contribution
 */
@NonNullByDefault
class ChannelEventTriggerHandlerTest {
    private @NonNullByDefault({}) ChannelEventTriggerHandler handler;
    private @NonNullByDefault({}) Trigger moduleMock;
    private @NonNullByDefault({}) BundleContext contextMock;

    @BeforeEach
    public void setUp() {
        moduleMock = mock(Trigger.class);
        contextMock = mock(BundleContext.class);
    }

    @Test
    public void testExactlyMatchingChannelIsApplied() {
        when(moduleMock.getConfiguration())
                .thenReturn(new Configuration(Map.of(ChannelEventTriggerHandler.CFG_CHANNEL, "foo:bar:baz:quux")));
        handler = new ChannelEventTriggerHandler(moduleMock, contextMock);

        assertTrue(handler.apply(ThingEventFactory.createTriggerEvent("PRESSED", new ChannelUID("foo:bar:baz:quux"))));
    }

    @Test
    public void testSubstringMatchingChannelIsNotApplied() {
        when(moduleMock.getConfiguration())
                .thenReturn(new Configuration(Map.of(ChannelEventTriggerHandler.CFG_CHANNEL, "foo:bar:baz:q")));
        handler = new ChannelEventTriggerHandler(moduleMock, contextMock);

        assertFalse(handler.apply(ThingEventFactory.createTriggerEvent("PRESSED", new ChannelUID("foo:bar:baz:quux"))));
    }
}
