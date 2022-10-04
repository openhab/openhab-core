/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemCommandEvent;
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

    @BeforeEach
    public void setUp() {
        registerVolatileStorageService();
        eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        var groupItem = new GroupItem("group");
        groupItem.setEventPublisher(eventPublisher);
        var switchItem = new SwitchItem("switch");
        groupItem.addMember(switchItem);
        itemRegistry.add(switchItem);
        itemRegistry.add(groupItem);

        EventSubscriber eventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                // veto OFF commands, for the veto test
                if (event instanceof ItemCommandEvent && ((ItemCommandEvent) event).getItemCommand() == OnOffType.OFF) {
                    ((ItemCommandEvent) event).veto();
                }
                receivedEvents.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(ItemStateChangedEvent.TYPE, ItemCommandEvent.TYPE);
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

    @Test
    public void testItemCommandEventForwardsToGroupMembers() throws Exception {
        eventPublisher.post(ItemEventFactory.createCommandEvent("group", OnOffType.ON));

        // first the group item gets a command event
        waitForAssert(() -> {
            assertFalse(receivedEvents.isEmpty());
            ItemCommandEvent commandEvent = (ItemCommandEvent) receivedEvents.poll();
            assertNotNull(commandEvent);
            assertFalse(commandEvent.isVetoed());
            assertEquals("group", commandEvent.getItemName());
            assertEquals(OnOffType.ON, commandEvent.getItemCommand());
        });
        // then the member of the group gets the event
        waitForAssert(() -> {
            assertFalse(receivedEvents.isEmpty());
            ItemCommandEvent commandEvent = (ItemCommandEvent) receivedEvents.poll();
            assertNotNull(commandEvent);
            assertEquals("switch", commandEvent.getItemName());
            assertEquals(OnOffType.ON, commandEvent.getItemCommand());
        });
    }

    @Test
    public void testItemCommandEventVetoedDoesNothing() throws Exception {
        eventPublisher.post(ItemEventFactory.createCommandEvent("group", OnOffType.OFF));

        waitForAssert(() -> {
            assertFalse(receivedEvents.isEmpty());
            ItemCommandEvent commandEvent = (ItemCommandEvent) receivedEvents.poll();
            assertNotNull(commandEvent);
            assertTrue(commandEvent.isVetoed());
            assertEquals("group", commandEvent.getItemName());
            assertEquals(OnOffType.OFF, commandEvent.getItemCommand());
        });
        // wait for all events to be processed
        Thread.sleep(100);

        // make sure no more command event has been sent
        assertTrue(receivedEvents.isEmpty());
    }
}
