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
package org.openhab.core.internal.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.events.TopicEventFilter;
import org.openhab.core.test.java.JavaOSGiTest;
import org.osgi.framework.ServiceRegistration;

/**
 * The {@link OSGiEventManagerOSGiTest} runs inside an OSGi container and tests the {@link OSGiEventManager}.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Simon Kaufmann - migrated from Groovy to Java
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class OSGiEventManagerOSGiTest extends JavaOSGiTest {

    private static final String EVENT_TYPE_A = "EVENT_TYPE_A";
    private static final String EVENT_TYPE_B = "EVENT_TYPE_B";
    private static final String EVENT_TYPE_C = "EVENT_TYPE_C";

    private static final String EVENT_TYPE_FACTORY_A_B = "EVENT_TYPE_FACTORY_A_B";
    private static final String EVENT_TYPE_FACTORY_C = "EVENT_TYPE_FACTORY_C";

    private static final String TOPIC = "openhab/some/topic";

    private static final String TYPE_BASED_SUBSCRIBER_1 = "TYPE_BASED_SUBSCRIBER_1";
    private static final String TYPE_BASED_SUBSCRIBER_2 = "TYPE_BASED_SUBSCRIBER_2";
    private static final String TOPIC_BASED_SUBSCRIBER_3 = "TOPIC_BASED_SUBSCRIBER_3";
    private static final String ALL_EVENT_TYPES_SUBSCRIBER_4 = "ALL_EVENT_TYPES_SUBSCRIBER_4";

    private final Map<String, ServiceRegistration<?>> serviceRegistrations = new HashMap<>();

    private @NonNullByDefault({}) EventPublisher eventPublisher;

    private @Mock @NonNullByDefault({}) EventSubscriber subscriber1Mock;
    private @Mock @NonNullByDefault({}) EventSubscriber subscriber2Mock;
    private @Mock @NonNullByDefault({}) EventSubscriber subscriber3Mock;
    private @Mock @NonNullByDefault({}) EventSubscriber subscriber4Mock;
    private @Mock @NonNullByDefault({}) EventFactory eventTypeFactoryABMock;
    private @Mock @NonNullByDefault({}) EventFactory eventTypeFactoryCMock;

    @BeforeEach
    public void beforeEach() throws Exception {
        eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        when(eventTypeFactoryABMock.getSupportedEventTypes()).thenReturn(Set.of(EVENT_TYPE_A, EVENT_TYPE_B));
        when(eventTypeFactoryABMock.createEvent(any(), any(), any(), any()))
                .thenAnswer(answer -> createEvent(answer.getArgument(0), answer.getArgument(2), answer.getArgument(1)));
        internalRegisterService(EVENT_TYPE_FACTORY_A_B, EventFactory.class, eventTypeFactoryABMock);

        when(eventTypeFactoryCMock.getSupportedEventTypes()).thenReturn(Set.of(EVENT_TYPE_C));
        when(eventTypeFactoryCMock.createEvent(any(), any(), any(), any()))
                .thenAnswer(answer -> createEvent(answer.getArgument(0), answer.getArgument(2), answer.getArgument(1)));
        internalRegisterService(EVENT_TYPE_FACTORY_C, EventFactory.class, eventTypeFactoryCMock);

        when(subscriber1Mock.getSubscribedEventTypes()).thenReturn(Set.of(EVENT_TYPE_A));
        internalRegisterService(TYPE_BASED_SUBSCRIBER_1, EventSubscriber.class, subscriber1Mock);

        when(subscriber2Mock.getSubscribedEventTypes()).thenReturn(Set.of(EVENT_TYPE_A));
        internalRegisterService(TYPE_BASED_SUBSCRIBER_2, EventSubscriber.class, subscriber2Mock);

        when(subscriber3Mock.getSubscribedEventTypes()).thenReturn(Set.of(EVENT_TYPE_B, EVENT_TYPE_C));
        when(subscriber3Mock.getEventFilter()).thenReturn(new TopicEventFilter(TOPIC));
        internalRegisterService(TOPIC_BASED_SUBSCRIBER_3, EventSubscriber.class, subscriber3Mock);

        when(subscriber4Mock.getSubscribedEventTypes()).thenReturn(Set.of(EventSubscriber.ALL_EVENT_TYPES));
        when(subscriber4Mock.getEventFilter()).thenReturn(new TopicEventFilter(TOPIC));
        internalRegisterService(ALL_EVENT_TYPES_SUBSCRIBER_4, EventSubscriber.class, subscriber4Mock);
    }

    @AfterEach
    public void afterEach() throws Exception {
        for (ServiceRegistration<?> service : serviceRegistrations.values()) {
            service.unregister();
        }
        serviceRegistrations.clear();
    }

    @Test
    public void testDispatchA() throws Exception {
        eventPublisher.post(createEvent(EVENT_TYPE_A));
        Thread.sleep(100);

        assertEvent(subscriber1Mock, createEvent(EVENT_TYPE_A));
        assertEvent(subscriber2Mock, createEvent(EVENT_TYPE_A));
        assertEventCount(subscriber3Mock, 0);
        assertEvent(subscriber4Mock, createEvent(EVENT_TYPE_A));
    }

    @Test
    public void testDispatchB() throws InterruptedException {
        eventPublisher.post(createEvent(EVENT_TYPE_B));
        Thread.sleep(100);

        assertEventCount(subscriber1Mock, 0);
        assertEventCount(subscriber2Mock, 0);
        assertEvent(subscriber3Mock, createEvent(EVENT_TYPE_B));
        assertEvent(subscriber4Mock, createEvent(EVENT_TYPE_B));
    }

    @Test
    public void testDispatchC() throws InterruptedException {
        eventPublisher.post(createEvent(EVENT_TYPE_C));
        Thread.sleep(100);

        assertEventCount(subscriber1Mock, 0);
        assertEventCount(subscriber2Mock, 0);
        assertEvent(subscriber3Mock, createEvent(EVENT_TYPE_C));
        assertEvent(subscriber4Mock, createEvent(EVENT_TYPE_C));
    }

    @Test
    public void testSubscriberUnregistration1() throws Exception {
        internalUnregisterService(TYPE_BASED_SUBSCRIBER_1);
        eventPublisher.post(createEvent(EVENT_TYPE_A));
        Thread.sleep(100);

        assertEventCount(subscriber1Mock, 0);
        assertEventCount(subscriber2Mock, 1);
        assertEventCount(subscriber3Mock, 0);
        assertEventCount(subscriber4Mock, 1);
    }

    @Test
    public void testSubscriberUnregistration1And2() throws Exception {
        internalUnregisterService(TYPE_BASED_SUBSCRIBER_1);
        internalUnregisterService(TYPE_BASED_SUBSCRIBER_2);
        eventPublisher.post(createEvent(EVENT_TYPE_A));
        Thread.sleep(100);

        assertEventCount(subscriber1Mock, 0);
        assertEventCount(subscriber2Mock, 0);
        assertEventCount(subscriber3Mock, 0);
        assertEventCount(subscriber4Mock, 1);
    }

    @Test
    public void testFactoryUnregistration() throws Exception {
        internalUnregisterService(EVENT_TYPE_FACTORY_A_B);
        eventPublisher.post(createEvent(EVENT_TYPE_A));
        Thread.sleep(100);

        assertEventCount(subscriber1Mock, 0);
        assertEventCount(subscriber2Mock, 0);
        assertEventCount(subscriber3Mock, 0);
        assertEventCount(subscriber4Mock, 0);
    }

    @Test
    public void testValidationType() {
        Event event = createEvent(null, "{a: 'A', b: 'B'}", TOPIC);
        try {
            eventPublisher.post(event);
            fail("IllegalArgumentException expected!");
        } catch (IllegalArgumentException e) {
            assertEquals("The type of the 'event' argument must not be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testValidationPayload() {
        Event event = createEvent(EVENT_TYPE_A, null, TOPIC);
        try {
            eventPublisher.post(event);
            fail("IllegalArgumentException expected!");
        } catch (IllegalArgumentException e) {
            assertEquals("The payload of the 'event' argument must not be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testValidationTopic() {
        Event event = createEvent(EVENT_TYPE_A, "{a: 'A', b: 'B'}", null);
        try {
            eventPublisher.post(event);
            fail("IllegalArgumentException expected!");
        } catch (IllegalArgumentException e) {
            assertEquals("The topic of the 'event' argument must not be null or empty.", e.getMessage());
        }
    }

    private Event createEvent(String eventType) {
        return createEvent(eventType, "{a: 'A', b: 'B'}", TOPIC);
    }

    private Event createEvent(@Nullable String eventType, @Nullable String payload, @Nullable String topic) {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(eventType == null ? giveNull() : eventType);
        when(event.getPayload()).thenReturn(payload == null ? giveNull() : payload);
        when(event.getTopic()).thenReturn(topic == null ? giveNull() : topic);
        return event;
    }

    private void internalUnregisterService(String key) {
        ServiceRegistration<?> reg = serviceRegistrations.remove(key);
        if (reg != null) {
            reg.unregister();
        }
    }

    private <S> void internalRegisterService(String key, Class<S> clazz, S serviceObject) {
        serviceRegistrations.put(key, bundleContext.registerService(clazz, serviceObject, null));
    }

    private void assertEvent(EventSubscriber subscriber, Event expectedEvent) {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        waitForAssert(() -> verify(subscriber).receive(captor.capture()));
        Event event = captor.getValue();
        assertNotNull(event);
        assertEquals(expectedEvent.getType(), event.getType());
        assertEquals(expectedEvent.getPayload(), event.getPayload());
        assertEquals(expectedEvent.getTopic(), event.getTopic());
    }

    private void assertEventCount(EventSubscriber subscriber, int count) {
        waitForAssert(() -> verify(subscriber, times(count)).receive(any()));
    }
}
