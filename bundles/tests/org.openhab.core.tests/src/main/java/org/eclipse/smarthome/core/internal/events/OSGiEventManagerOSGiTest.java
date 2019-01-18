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
package org.eclipse.smarthome.core.internal.events;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFactory;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.events.TopicEventFilter;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.osgi.framework.ServiceRegistration;

/**
 * The {@link OSGiEventManagerOSGiTest} runs inside an OSGi container and tests the {@link OSGiEventManager}.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Simon Kaufmann - migrated from Groovy to Java
 */
public class OSGiEventManagerOSGiTest extends JavaOSGiTest {

    private static final String EVENT_TYPE_A = "EVENT_TYPE_A";
    private static final String EVENT_TYPE_B = "EVENT_TYPE_B";
    private static final String EVENT_TYPE_C = "EVENT_TYPE_C";

    private static final String EVENT_TYPE_FACTORY_A_B = "EVENT_TYPE_FACTORY_A_B";
    private static final String EVENT_TYPE_FACTORY_C = "EVENT_TYPE_FACTORY_C";

    private static final String TOPIC = "smarthome/some/topic";

    private static final String TYPE_BASED_SUBSCRIBER_1 = "TYPE_BASED_SUBSCRIBER_1";
    private static final String TYPE_BASED_SUBSCRIBER_2 = "TYPE_BASED_SUBSCRIBER_2";
    private static final String TOPIC_BASED_SUBSCRIBER_3 = "TOPIC_BASED_SUBSCRIBER_3";
    private static final String ALL_EVENT_TYPES_SUBSCRIBER_4 = "ALL_EVENT_TYPES_SUBSCRIBER_4";

    private final Map<String, ServiceRegistration<?>> serviceRegistrations = new HashMap<>();

    private EventPublisher eventPublisher;

    private @Mock EventSubscriber subscriber1;
    private @Mock EventSubscriber subscriber2;
    private @Mock EventSubscriber subscriber3;
    private @Mock EventSubscriber subscriber4;
    private @Mock EventFactory eventTypeFactoryAB;
    private @Mock EventFactory eventTypeFactoryC;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        when(eventTypeFactoryAB.getSupportedEventTypes())
                .thenReturn(new HashSet<>(Arrays.asList(EVENT_TYPE_A, EVENT_TYPE_B)));
        when(eventTypeFactoryAB.createEvent(any(), any(), any(), any()))
                .thenAnswer(answer -> createEvent(answer.getArgument(0), answer.getArgument(2), answer.getArgument(1)));
        internalRegisterService(EVENT_TYPE_FACTORY_A_B, EventFactory.class, eventTypeFactoryAB);

        when(eventTypeFactoryC.getSupportedEventTypes()).thenReturn(Collections.singleton(EVENT_TYPE_C));
        when(eventTypeFactoryC.createEvent(any(), any(), any(), any()))
                .thenAnswer(answer -> createEvent(answer.getArgument(0), answer.getArgument(2), answer.getArgument(1)));
        internalRegisterService(EVENT_TYPE_FACTORY_C, EventFactory.class, eventTypeFactoryC);

        when(subscriber1.getSubscribedEventTypes()).thenReturn(Collections.singleton(EVENT_TYPE_A));
        internalRegisterService(TYPE_BASED_SUBSCRIBER_1, EventSubscriber.class, subscriber1);

        when(subscriber2.getSubscribedEventTypes()).thenReturn(Collections.singleton(EVENT_TYPE_A));
        internalRegisterService(TYPE_BASED_SUBSCRIBER_2, EventSubscriber.class, subscriber2);

        when(subscriber3.getSubscribedEventTypes())
                .thenReturn(new HashSet<>(Arrays.asList(EVENT_TYPE_B, EVENT_TYPE_C)));
        when(subscriber3.getEventFilter()).thenReturn(new TopicEventFilter(TOPIC));
        internalRegisterService(TOPIC_BASED_SUBSCRIBER_3, EventSubscriber.class, subscriber3);

        when(subscriber4.getSubscribedEventTypes()).thenReturn(Collections.singleton(EventSubscriber.ALL_EVENT_TYPES));
        when(subscriber4.getEventFilter()).thenReturn(new TopicEventFilter(TOPIC));
        internalRegisterService(ALL_EVENT_TYPES_SUBSCRIBER_4, EventSubscriber.class, subscriber4);
    }

    @After
    public void cleanUp() {
        for (ServiceRegistration<?> service : serviceRegistrations.values()) {
            service.unregister();
        }
        serviceRegistrations.clear();
    }

    @Test
    public void testDispatchA() throws Exception {
        eventPublisher.post(createEvent(EVENT_TYPE_A));
        Thread.sleep(100);

        assertEvent(subscriber1, createEvent(EVENT_TYPE_A));
        assertEvent(subscriber2, createEvent(EVENT_TYPE_A));
        assertEventCount(subscriber3, 0);
        assertEvent(subscriber4, createEvent(EVENT_TYPE_A));
    }

    @Test
    public void testDispatchB() throws InterruptedException {
        eventPublisher.post(createEvent(EVENT_TYPE_B));
        Thread.sleep(100);

        assertEventCount(subscriber1, 0);
        assertEventCount(subscriber2, 0);
        assertEvent(subscriber3, createEvent(EVENT_TYPE_B));
        assertEvent(subscriber4, createEvent(EVENT_TYPE_B));
    }

    @Test
    public void testDispatchC() throws InterruptedException {
        eventPublisher.post(createEvent(EVENT_TYPE_C));
        Thread.sleep(100);

        assertEventCount(subscriber1, 0);
        assertEventCount(subscriber2, 0);
        assertEvent(subscriber3, createEvent(EVENT_TYPE_C));
        assertEvent(subscriber4, createEvent(EVENT_TYPE_C));
    }

    @Test
    public void testSubscriberUnregistration1() throws Exception {
        internalUnregisterService(TYPE_BASED_SUBSCRIBER_1);
        eventPublisher.post(createEvent(EVENT_TYPE_A));
        Thread.sleep(100);

        assertEventCount(subscriber1, 0);
        assertEventCount(subscriber2, 1);
        assertEventCount(subscriber3, 0);
        assertEventCount(subscriber4, 1);
    }

    @Test
    public void testSubscriberUnregistration1_2() throws Exception {
        internalUnregisterService(TYPE_BASED_SUBSCRIBER_1);
        internalUnregisterService(TYPE_BASED_SUBSCRIBER_2);
        eventPublisher.post(createEvent(EVENT_TYPE_A));
        Thread.sleep(100);

        assertEventCount(subscriber1, 0);
        assertEventCount(subscriber2, 0);
        assertEventCount(subscriber3, 0);
        assertEventCount(subscriber4, 1);
    }

    @Test
    public void testFactoryUnregistration() throws Exception {
        internalUnregisterService(EVENT_TYPE_FACTORY_A_B);
        eventPublisher.post(createEvent(EVENT_TYPE_A));
        Thread.sleep(100);

        assertEventCount(subscriber1, 0);
        assertEventCount(subscriber2, 0);
        assertEventCount(subscriber3, 0);
        assertEventCount(subscriber4, 0);
    }

    @Test
    public void testValidationEvent() {
        try {
            eventPublisher.post(null);
            fail("IllegalArgumentException expected!");
        } catch (IllegalArgumentException e) {
            assertEquals("Argument 'event' must not be null.", e.getMessage());
        }

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

    private Event createEvent(String eventType, String payload, String topic) {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(eventType);
        when(event.getPayload()).thenReturn(payload);
        when(event.getTopic()).thenReturn(topic);
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
