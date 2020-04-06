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
package org.openhab.core.internal.items;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateChangedEvent;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * The {@link ItemUpdaterOSGiTest} runs inside an OSGi container and tests the {@link ItemRegistry}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Andre Fuechsel - extended with tag tests
 * @author Kai Kreuzer - added tests for all items changed cases
 */
@NonNullByDefault
public class ItemUpdaterOSGiTest extends JavaOSGiTest {

    private @NonNullByDefault({}) EventPublisher eventPublisher;
    private @NonNullByDefault({}) ItemRegistry itemRegistry;
    private final ConcurrentLinkedQueue<Event> receivedEvents = new ConcurrentLinkedQueue<>();

    @Before
    public void setUp() {
        registerVolatileStorageService();
        eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        itemRegistry.add(new SwitchItem("switch"));

        EventSubscriber eventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvents.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Collections.singleton(ItemStateChangedEvent.TYPE);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        };
        registerService(eventSubscriber);
    }

    @Test
    public void testItemUpdaterSetsItemState() {
        eventPublisher.post(ItemEventFactory.createStateEvent("switch", OnOffType.ON));
        Item switchItem = itemRegistry.get("switch");
        waitForAssert(() -> assertEquals(OnOffType.ON, switchItem.getState()));
    }

    @Test
    public void testItemUpdaterSendsStateChangedEvent() throws Exception {
        eventPublisher.post(ItemEventFactory.createStateEvent("switch", OnOffType.ON));

        Item switchItem = itemRegistry.get("switch");
        waitForAssert(() -> assertEquals(OnOffType.ON, switchItem.getState()));

        // change state
        eventPublisher.post(ItemEventFactory.createStateEvent("switch", OnOffType.OFF));

        // wait for an event that change the state from OFF to ON
        // there could be one remaining event from the 'ItemUpdater sets item state' test
        waitForAssert(() -> {
            assertFalse(receivedEvents.isEmpty());
            ItemStateChangedEvent changedEvent = (ItemStateChangedEvent) receivedEvents.poll();
            assertNotNull(changedEvent);
            assertEquals(OnOffType.ON, changedEvent.getOldItemState());
            assertEquals(OnOffType.OFF, changedEvent.getItemState());
        });

        // send update for same state
        eventPublisher.post(ItemEventFactory.createStateEvent("switch", OnOffType.OFF));

        // wait a few milliseconds
        Thread.sleep(100);

        // make sure no state changed event has been sent
        assertTrue(receivedEvents.isEmpty());
    }
}
