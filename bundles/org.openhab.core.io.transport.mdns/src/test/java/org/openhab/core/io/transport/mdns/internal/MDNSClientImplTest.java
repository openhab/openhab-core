/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.transport.mdns.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.test.Matchers.*;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.io.transport.mdns.ServiceDescription;
import org.openhab.core.io.transport.mdns.internal.MDNSClientImpl.QueueTaskHandler;
import org.openhab.core.io.transport.mdns.internal.MDNSClientImpl.TaskAction;
import org.openhab.core.net.NetworkAddressService;

/**
 * Tests for {@link MDNSClientImpl}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
class MDNSClientImplTest {

    private @Mock @NonNullByDefault({}) NetworkAddressService networkAddressService;

    @Test
    public void serviceLogicTest() throws Exception {
        MDNSClientImpl client = new MDNSClientImpl(networkAddressService);

        Queue<QueueTaskHandler<@Nullable Void, ServiceDescription>> queue = client.serviceQueue;
        assertNotNull(queue);

        final SequenceRecorder<String> recorder = new SequenceRecorder<>();
        ServiceDescription mockService = new ServiceDescription("_oh-test-server._tcp.local.", "oh-test", 16372, null);
        CountDownLatch task1Blocker = new CountDownLatch(1);
        CountDownLatch task1Started = new CountDownLatch(1);

        client.submitToQueue(queue, mockService, TaskAction.REGISTER, () -> {
            recorder.recordEvent("Task1 waiting");
            task1Started.countDown();
            task1Blocker.await();
            recorder.recordEvent("Task1 finished");
            return null;
        });
        task1Started.await(10, TimeUnit.SECONDS);

        client.submitToQueue(queue, mockService, TaskAction.UNREGISTER, () -> {
            recorder.recordEvent("Task2 finished");
            return null;
        });

        synchronized (queue) {
            assertThat(queue, hasSize(2));
            Iterator<QueueTaskHandler<@Nullable Void, ServiceDescription>> it = queue.iterator();

            QueueTaskHandler<@Nullable Void, ServiceDescription> first = it.next();
            assertThat(first.getAction(), is(TaskAction.REGISTER));
            assertThat(first.getIdentifier(), is(mockService));
            assertTrue(first.isActive());

            QueueTaskHandler<@Nullable Void, ServiceDescription> second = it.next();
            assertThat(second.getAction(), is(TaskAction.UNREGISTER));
            assertThat(second.getIdentifier(), is(mockService));
            assertFalse(second.isActive());
        }

        client.submitToQueue(queue, mockService, TaskAction.REGISTER, () -> {
            recorder.recordEvent("Task3 finished");
            return null;
        });

        synchronized (queue) {
            assertThat(queue, hasSize(1));
        }

        client.submitToQueue(queue, mockService, TaskAction.UNREGISTER, () -> {
            recorder.recordEvent("Task4 finished");
            return null;
        });

        synchronized (queue) {
            assertThat(queue, hasSize(2));
        }

        assertThat(recorder.getEvents(), hasSize(1));
        assertThat(recorder.getEvents().get(0), is("Task1 waiting"));

        task1Blocker.countDown();

        assertThat(queue, waitUntil(synchronizedMatcher(queue, is(empty())), 5000L));

        assertThat(recorder.getEvents(), hasSize(3));
        assertThat(recorder.getEvents().get(1), is("Task1 finished"));
        assertThat(recorder.getEvents().get(2), is("Task4 finished"));
    }

    @Test
    public void addressLogicTest() throws Exception {
        MDNSClientImpl client = new MDNSClientImpl(networkAddressService);

        Queue<QueueTaskHandler<@Nullable Void, InetAddress>> queue = client.addressQueue;
        assertNotNull(queue);

        final SequenceRecorder<String> recorder = new SequenceRecorder<>();

        InetAddress mockAddr1 = InetAddress.getByName("127.0.0.5");
        InetAddress mockAddr2 = InetAddress.getByName("127.0.0.19");

        CountDownLatch task1Blocker = new CountDownLatch(1);
        CountDownLatch task1Started = new CountDownLatch(1);
        client.submitToQueue(queue, mockAddr1, TaskAction.REGISTER, () -> {
            recorder.recordEvent("Task1 waiting");
            task1Started.countDown();
            task1Blocker.await();
            recorder.recordEvent("Task1 finished");
            return null;
        });
        task1Started.await(10, TimeUnit.SECONDS);

        CountDownLatch task2Blocker = new CountDownLatch(1);
        CountDownLatch task2Started = new CountDownLatch(1);
        client.submitToQueue(queue, mockAddr2, TaskAction.REGISTER, () -> {
            recorder.recordEvent("Task2 waiting");
            task2Started.countDown();
            task2Blocker.await();
            recorder.recordEvent("Task2 finished");
            return null;
        });

        assertThat(recorder.getEvents(), waitUntil(hasSize(2), 5000L));
        assertThat(recorder.getEvents().get(0), is("Task1 waiting"));
        assertThat(recorder.getEvents().get(1), is("Task2 waiting"));

        assertThat(queue, waitUntil(synchronizedMatcher(queue, hasSize(2)), 5000L));
        synchronized (queue) {
            Iterator<QueueTaskHandler<@Nullable Void, InetAddress>> it = queue.iterator();

            QueueTaskHandler<@Nullable Void, InetAddress> first = it.next();
            assertThat(first.getAction(), is(TaskAction.REGISTER));
            assertThat(first.getIdentifier(), is(mockAddr1));
            assertTrue(first.isActive());

            QueueTaskHandler<@Nullable Void, InetAddress> second = it.next();
            assertThat(second.getAction(), is(TaskAction.REGISTER));
            assertThat(second.getIdentifier(), is(mockAddr2));
            assertTrue(second.isActive());
        }

        client.submitToQueue(queue, mockAddr1, TaskAction.UNREGISTER, () -> {
            recorder.recordEvent("Task3 finished");
            return null;
        });

        client.submitToQueue(queue, mockAddr2, TaskAction.REGISTER, () -> {
            recorder.recordEvent("Task4 finished");
            return null;
        });

        assertThat(queue, waitUntil(synchronizedMatcher(queue, hasSize(4)), 5000L));
        synchronized (queue) {
            Iterator<QueueTaskHandler<@Nullable Void, InetAddress>> it = queue.iterator();

            QueueTaskHandler<@Nullable Void, InetAddress> element = it.next();
            assertThat(element.getAction(), is(TaskAction.REGISTER));
            assertThat(element.getIdentifier(), is(mockAddr1));
            assertTrue(element.isActive());

            element = it.next();
            assertThat(element.getAction(), is(TaskAction.REGISTER));
            assertThat(element.getIdentifier(), is(mockAddr2));
            assertTrue(element.isActive());

            element = it.next();
            assertThat(element.getAction(), is(TaskAction.UNREGISTER));
            assertThat(element.getIdentifier(), is(mockAddr1));
            assertFalse(element.isActive());

            element = it.next();
            assertThat(element.getAction(), is(TaskAction.REGISTER));
            assertThat(element.getIdentifier(), is(mockAddr2));
            assertFalse(element.isActive());
        }

        assertThat(recorder.getEvents(), waitUntil(hasSize(2), 5000L));
        assertThat(recorder.getEvents().get(0), is("Task1 waiting"));
        assertThat(recorder.getEvents().get(1), is("Task2 waiting"));

        client.submitToQueue(queue, mockAddr1, TaskAction.UNREGISTER, () -> {
            recorder.recordEvent("Task5 finished");
            return null;
        });

        client.submitToQueue(queue, mockAddr2, TaskAction.UNREGISTER, () -> {
            recorder.recordEvent("Task6 finished");
            return null;
        });

        assertThat(queue, waitUntil(synchronizedMatcher(queue, hasSize(4)), 5000L));
        synchronized (queue) {
            Iterator<QueueTaskHandler<@Nullable Void, InetAddress>> it = queue.iterator();

            QueueTaskHandler<@Nullable Void, InetAddress> element = it.next();
            assertThat(element.getAction(), is(TaskAction.REGISTER));
            assertThat(element.getIdentifier(), is(mockAddr1));
            assertTrue(element.isActive());

            element = it.next();
            assertThat(element.getAction(), is(TaskAction.REGISTER));
            assertThat(element.getIdentifier(), is(mockAddr2));
            assertTrue(element.isActive());

            element = it.next();
            assertThat(element.getAction(), is(TaskAction.UNREGISTER));
            assertThat(element.getIdentifier(), is(mockAddr1));
            assertFalse(element.isActive());

            element = it.next();
            assertThat(element.getAction(), is(TaskAction.UNREGISTER));
            assertThat(element.getIdentifier(), is(mockAddr1));
            assertFalse(element.isActive());
        }

        assertThat(recorder.getEvents(), waitUntil(hasSize(2), 5000L));

        task1Blocker.countDown();

        assertThat(queue, waitUntil(synchronizedMatcher(queue, hasSize(1)), 5000L));
        synchronized (queue) {
            Iterator<QueueTaskHandler<@Nullable Void, InetAddress>> it = queue.iterator();

            QueueTaskHandler<@Nullable Void, InetAddress> element = it.next();
            assertThat(element.getAction(), is(TaskAction.REGISTER));
            assertThat(element.getIdentifier(), is(mockAddr2));
            assertTrue(element.isActive());
        }

        assertThat(recorder.getEvents(), waitUntil(hasSize(5), 5000L));
        assertThat(recorder.getEvents().get(2), is("Task1 finished"));
        assertThat(recorder.getEvents().get(3), is("Task3 finished"));
        assertThat(recorder.getEvents().get(4), is("Task5 finished"));

        task2Blocker.countDown();
        assertThat(queue, waitUntil(synchronizedMatcher(queue, is(empty())), 5000L));

        assertThat(recorder.getEvents(), waitUntil(hasSize(6), 5000L));
        assertThat(recorder.getEvents().get(5), is("Task2 finished"));
    }

    private static class SequenceRecorder<T> {

        private final List<T> events = new CopyOnWriteArrayList<>();

        public void recordEvent(T event) {
            events.add(event);
        }

        public List<T> getEvents() {
            return events;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("SequenceRecorder [events=").append(events).append("]");
            return builder.toString();
        }
    }
}
