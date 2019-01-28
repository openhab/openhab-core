/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.items.events;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFactory;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.Metadata;
import org.eclipse.smarthome.core.items.MetadataKey;
import org.eclipse.smarthome.core.items.MetadataProvider;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * The {@link AbstractItemEventSubscriberOSGiTest} runs inside an OSGi container and tests the
 * {@link AbstractItemEventSubscriber}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public class AbstractItemEventSubscriberOSGiTest extends JavaOSGiTest {

    private static final String ITEM_NAME = "SomeItem";
    private EventPublisher eventPublisher;
    private @Mock ItemProvider itemProvider;
    private ItemCommandEvent commandEvent;
    private ItemStateEvent updateEvent;
    private @Mock MetadataProvider mockMetadataProvider;

    @Before
    public void setup() {
        initMocks(this);

        eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        when(itemProvider.getAll()).thenReturn(Collections.singletonList(new SwitchItem(ITEM_NAME)));
        registerService(itemProvider);

        EventSubscriber itemEventSubscriber = new AbstractItemEventSubscriber() {
            @Override
            protected void receiveCommand(ItemCommandEvent event) {
                commandEvent = event;
            }

            @Override
            protected void receiveUpdate(ItemStateEvent event) {
                updateEvent = event;
            }
        };
        registerService(itemEventSubscriber, EventSubscriber.class.getName());

        when(mockMetadataProvider.getAll()).thenReturn(Collections
                .singletonList(new Metadata(new MetadataKey("autoupdate", ITEM_NAME), Boolean.toString(false), null)));
        registerService(mockMetadataProvider);
    }

    @Test
    public void testReceive() {
        eventPublisher.post(ItemEventFactory.createCommandEvent(ITEM_NAME, OnOffType.ON));
        waitForAssert(() -> assertNotNull(commandEvent));
        waitForAssert(() -> assertNull(updateEvent));

        commandEvent = null;
        updateEvent = null;

        eventPublisher.post(ItemEventFactory.createStateEvent(ITEM_NAME, OnOffType.ON));
        waitForAssert(() -> assertNull(commandEvent));
        waitForAssert(() -> assertNotNull(updateEvent));
    }

    @Test
    public void testReceiveUnsupportedEvent() throws Exception {
        String someEventType = "SOME_EVENT_TYPE";
        EventFactory someEventFactory = new EventFactory() {
            @Override
            public Event createEvent(String eventType, String topic, String payload, String source) throws Exception {
                return new Event() {
                    @Override
                    public String getType() {
                        return eventType;
                    }

                    @Override
                    public String getTopic() {
                        return topic;
                    }

                    @Override
                    public String getPayload() {
                        return payload;
                    }

                    @Override
                    public String getSource() {
                        return source;
                    }
                };
            }

            @Override
            public Set<String> getSupportedEventTypes() {
                return Collections.singleton(someEventType);
            }
        };
        registerService(someEventFactory);

        Event event = new Event() {
            @Override
            public String getType() {
                return someEventType;
            }

            @Override
            public String getTopic() {
                return "smarthome/items";
            }

            @Override
            public String getPayload() {
                return "{a: 'A', b: 'B'}";
            }

            @Override
            public String getSource() {
                return null;
            }
        };
        eventPublisher.post(event);
        Thread.sleep(100);
        waitForAssert(() -> assertNull(commandEvent));
        waitForAssert(() -> assertNull(updateEvent));
    }
}
