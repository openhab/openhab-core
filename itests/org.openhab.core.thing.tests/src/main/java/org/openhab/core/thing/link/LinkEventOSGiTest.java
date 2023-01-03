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
package org.openhab.core.thing.link;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.events.ItemChannelLinkAddedEvent;
import org.openhab.core.thing.link.events.ItemChannelLinkRemovedEvent;

/**
 * Event Tests for {@link ItemChannelLinkRegistry}.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public class LinkEventOSGiTest extends JavaOSGiTest {

    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) ItemChannelLinkEventSubscriber eventSubscriber;

    class ItemChannelLinkEventSubscriber implements EventSubscriber {

        private @Nullable Event lastReceivedEvent;

        @Override
        public Set<String> getSubscribedEventTypes() {
            return Stream.of(ItemChannelLinkAddedEvent.TYPE, ItemChannelLinkRemovedEvent.TYPE)
                    .collect(Collectors.toSet());
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
        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        eventSubscriber = new ItemChannelLinkEventSubscriber();
        registerService(eventSubscriber);
    }

    @Test
    public void assertItemChannelLinkEventsAreSent() {
        ItemChannelLink link = new ItemChannelLink("item", new ChannelUID("a:b:c:d"));

        itemChannelLinkRegistry.add(link);
        waitFor(() -> eventSubscriber.getLastReceivedEvent() != null);
        waitForAssert(
                () -> assertThat(eventSubscriber.getLastReceivedEvent().getType(), is(ItemChannelLinkAddedEvent.TYPE)));
        assertThat(eventSubscriber.getLastReceivedEvent().getTopic(), is("openhab/links/item-a:b:c:d/added"));

        itemChannelLinkRegistry.remove(link.getUID());
        waitForAssert(() -> assertThat(eventSubscriber.getLastReceivedEvent().getType(),
                is(ItemChannelLinkRemovedEvent.TYPE)));
        assertThat(eventSubscriber.getLastReceivedEvent().getTopic(), is("openhab/links/item-a:b:c:d/removed"));
    }
}
