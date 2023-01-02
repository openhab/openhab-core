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
package org.openhab.core.items.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * The {@link AbstractItemEventSubscriberOSGiTest} runs inside an OSGi container and tests the
 * {@link AbstractItemEventSubscriber}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class AbstractItemEventSubscriberOSGiTest extends JavaOSGiTest {

    private static final String ITEM_NAME = "SomeItem";

    private @NonNullByDefault({}) EventPublisher eventPublisher;
    private @NonNullByDefault({}) ItemCommandEvent commandEvent;
    private @NonNullByDefault({}) ItemStateEvent updateEvent;

    private @Mock @NonNullByDefault({}) ItemProvider itemProviderMock;
    private @Mock @NonNullByDefault({}) MetadataProvider mockMetadataProviderMock;

    @BeforeEach
    public void beforeEach() {
        eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        when(itemProviderMock.getAll()).thenReturn(List.of(new SwitchItem(ITEM_NAME)));
        registerService(itemProviderMock);

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

        when(mockMetadataProviderMock.getAll()).thenReturn(
                List.of(new Metadata(new MetadataKey("autoupdate", ITEM_NAME), Boolean.toString(false), null)));
        registerService(mockMetadataProviderMock);
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
            public Event createEvent(String eventType, String topic, String payload, @Nullable String source)
                    throws Exception {
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
                    public @Nullable String getSource() {
                        return source;
                    }
                };
            }

            @Override
            public Set<String> getSupportedEventTypes() {
                return Set.of(someEventType);
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
                return "openhab/items";
            }

            @Override
            public String getPayload() {
                return "{a: 'A', b: 'B'}";
            }

            @Override
            public @Nullable String getSource() {
                return null;
            }
        };
        eventPublisher.post(event);
        Thread.sleep(100);
        waitForAssert(() -> assertNull(commandEvent));
        waitForAssert(() -> assertNull(updateEvent));
    }
}
