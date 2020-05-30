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
package org.openhab.core.thing.events;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingFactory;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;

/**
 * Event Tests for {@link ThingRegistry}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ThingEventOSGiTest extends JavaOSGiTest {

    class ThingEventSubscriber implements EventSubscriber {

        private @Nullable Event lastReceivedEvent;

        @Override
        public Set<String> getSubscribedEventTypes() {
            return Set.of(ThingAddedEvent.TYPE, ThingUpdatedEvent.TYPE, ThingRemovedEvent.TYPE);
        }

        @Override
        public @Nullable EventFilter getEventFilter() {
            return null;
        }

        @Override
        public void receive(Event event) {
            lastReceivedEvent = event;
        }

        public @Nullable Event getLastReceivedEvent() {
            return lastReceivedEvent;
        }
    }

    @BeforeEach
    public void setup() {
        registerVolatileStorageService();
    }

    @Test
    public void assertThingEventsAreSent() {
        ThingRegistry thingRegistry = getService(ThingRegistry.class);

        ThingEventSubscriber eventSubscriber = new ThingEventSubscriber();
        registerService(eventSubscriber);

        ThingType thingType = ThingTypeBuilder.instance("bindingId", "thingTypeId", "label").build();
        ThingUID thingUID = new ThingUID(thingType.getUID(), "thingId");
        Configuration configuration = new Configuration();
        Thing thing = ThingFactory.createThing(thingType, thingUID, configuration);

        thingRegistry.add(thing);
        waitFor(() -> eventSubscriber.getLastReceivedEvent() != null);
        waitForAssert(() -> assertThat(eventSubscriber.getLastReceivedEvent().getType(), is(ThingAddedEvent.TYPE)));
        assertThat(eventSubscriber.getLastReceivedEvent().getTopic(),
                is(ThingEventFactory.THING_ADDED_EVENT_TOPIC.replace("{thingUID}", thingUID.getAsString())));

        thingRegistry.update(thing);
        waitForAssert(() -> assertThat(eventSubscriber.getLastReceivedEvent().getType(), is(ThingUpdatedEvent.TYPE)));
        assertThat(eventSubscriber.getLastReceivedEvent().getTopic(),
                is(ThingEventFactory.THING_UPDATED_EVENT_TOPIC.replace("{thingUID}", thingUID.getAsString())));

        thingRegistry.forceRemove(thing.getUID());
        waitForAssert(() -> assertThat(eventSubscriber.getLastReceivedEvent().getType(), is(ThingRemovedEvent.TYPE)));
        assertThat(eventSubscriber.getLastReceivedEvent().getTopic(),
                is(ThingEventFactory.THING_REMOVED_EVENT_TOPIC.replace("{thingUID}", thingUID.getAsString())));
    }
}
