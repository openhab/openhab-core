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
package org.openhab.core.internal.items;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.items.events.ItemStateChangedEvent;
import org.openhab.core.items.events.ItemStateUpdatedEvent;
import org.openhab.core.items.events.ItemTimeSeriesUpdatedEvent;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.UnDefType;

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
    private @NonNullByDefault({}) SwitchItem switchItem;

    private final Queue<Event> receivedEvents = new ConcurrentLinkedQueue<>();

    @BeforeEach
    public void setUp() {
        registerVolatileStorageService();
        eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        switchItem = new SwitchItem("switch");
        itemRegistry.add(switchItem);

        EventSubscriber eventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvents.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(ItemStateChangedEvent.TYPE, ItemStateUpdatedEvent.TYPE, ItemTimeSeriesUpdatedEvent.TYPE);
            }
        };
        registerService(eventSubscriber);
    }

    @AfterEach
    public void tearDown() {
        receivedEvents.clear();
        itemRegistry.remove(switchItem.getName());
    }

    @Test
    public void testItemUpdaterSetsItemState() {
        eventPublisher.post(ItemEventFactory.createStateEvent("switch", OnOffType.ON));
        Item switchItem = itemRegistry.get("switch");
        waitForAssert(() -> assertEquals(OnOffType.ON, switchItem.getState()));
    }

    @Test
    public void testItemUpdaterSendsStateUpdatedEvent() throws Exception {
        eventPublisher.post(ItemEventFactory.createStateEvent("switch", OnOffType.ON));

        Item switchItem = itemRegistry.get("switch");
        waitForAssert(() -> assertEquals(OnOffType.ON, switchItem.getState()));

        // wait for the initial events (updated and changed, because it was NULL before)
        waitForAssert(() -> {
            assertEquals(2, receivedEvents.size());
            ItemStateUpdatedEvent updatedEvent = (ItemStateUpdatedEvent) receivedEvents.poll();
            assertNotNull(updatedEvent);
            assertEquals(OnOffType.ON, updatedEvent.getItemState());
            ItemStateChangedEvent changedEvent = (ItemStateChangedEvent) receivedEvents.poll();
            assertNotNull(changedEvent);
            assertEquals(UnDefType.NULL, changedEvent.getOldItemState());
            assertEquals(OnOffType.ON, changedEvent.getItemState());
        });

        // update with same value
        eventPublisher.post(ItemEventFactory.createStateEvent("switch", OnOffType.ON));

        // wait for the updated event
        waitForAssert(() -> {
            assertEquals(1, receivedEvents.size());
            ItemStateUpdatedEvent updatedEvent = (ItemStateUpdatedEvent) receivedEvents.poll();
            assertNotNull(updatedEvent);
            assertEquals(OnOffType.ON, updatedEvent.getItemState());
        });

        // ensure no other events send
        Thread.sleep(1000);
        assertTrue(receivedEvents.isEmpty());
    }

    @Test
    public void testItemUpdaterSendsStateChangedEvent() throws Exception {
        eventPublisher.post(ItemEventFactory.createStateEvent("switch", OnOffType.ON));

        // wait for the initial events (updated and changed, because it was NULL before)
        waitForAssert(() -> {
            assertEquals(2, receivedEvents.size());
            ItemStateUpdatedEvent updatedEvent = (ItemStateUpdatedEvent) receivedEvents.poll();
            assertNotNull(updatedEvent);
            assertEquals(OnOffType.ON, updatedEvent.getItemState());
            ItemStateChangedEvent changedEvent = (ItemStateChangedEvent) receivedEvents.poll();
            assertNotNull(changedEvent);
            assertEquals(UnDefType.NULL, changedEvent.getOldItemState());
            assertEquals(OnOffType.ON, changedEvent.getItemState());
        });

        // change state
        eventPublisher.post(ItemEventFactory.createStateEvent("switch", OnOffType.OFF));

        // wait for two events: the updated event and the changed event
        waitForAssert(() -> {
            assertEquals(2, receivedEvents.size());
            ItemStateUpdatedEvent updatedEvent = (ItemStateUpdatedEvent) receivedEvents.poll();
            assertNotNull(updatedEvent);
            assertEquals(OnOffType.OFF, updatedEvent.getItemState());
            ItemStateChangedEvent changedEvent = (ItemStateChangedEvent) receivedEvents.poll();
            assertNotNull(changedEvent);
            assertEquals(OnOffType.ON, changedEvent.getOldItemState());
            assertEquals(OnOffType.OFF, changedEvent.getItemState());
        });

        // wait a second and make sure no other events have been sent
        Thread.sleep(1000);
        assertTrue(receivedEvents.isEmpty());
    }

    @Test
    public void testItemUpdaterSetsTimeSeries() throws InterruptedException {
        TimeSeries timeSeries = new TimeSeries(TimeSeries.Policy.ADD);
        timeSeries.add(Instant.now(), OnOffType.ON);
        eventPublisher.post(ItemEventFactory.createTimeSeriesEvent("switch", timeSeries, null));

        // wait for the event
        waitForAssert(() -> {
            assertEquals(1, receivedEvents.size());
            ItemTimeSeriesUpdatedEvent updatedEvent = (ItemTimeSeriesUpdatedEvent) receivedEvents.poll();
            assertNotNull(updatedEvent);
            assertEquals(timeSeries, updatedEvent.getTimeSeries());
        });

        Thread.sleep(1000);
        assertTrue(receivedEvents.isEmpty());
    }
}
