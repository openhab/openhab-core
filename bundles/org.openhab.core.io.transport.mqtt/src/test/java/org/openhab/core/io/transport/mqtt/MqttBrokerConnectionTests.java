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
package org.openhab.core.io.transport.mqtt;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;
import org.openhab.core.io.transport.mqtt.internal.client.MqttAsyncClientWrapper;
import org.openhab.core.io.transport.mqtt.reconnect.AbstractReconnectStrategy;
import org.openhab.core.io.transport.mqtt.reconnect.PeriodicReconnectStrategy;
import org.openhab.core.test.java.JavaTest;
import org.osgi.service.cm.ConfigurationException;

import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

/**
 * Tests the MqttBrokerConnection class
 *
 * @author David Graeff - Initial contribution
 * @author Jan N. Klug - adjusted to HiveMQ client
 */
@NonNullByDefault
public class MqttBrokerConnectionTests extends JavaTest {
    @Test
    public void subscribeBeforeOnlineThenConnect()
            throws ConfigurationException, MqttException, InterruptedException, ExecutionException, TimeoutException {
        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("123.123.123.123", null, false,
                "MqttBrokerConnectionTests");

        // Add a subscriber while still offline
        MqttMessageSubscriber subscriber = mock(MqttMessageSubscriber.class);
        connection.subscribe("homie/device123/$name", subscriber);

        assertTrue(connection.start().get(200, TimeUnit.MILLISECONDS));
        assertTrue(connection.hasSubscribers());
        assertThat(connection.connectionState(), is(MqttConnectionState.CONNECTED));

        Mqtt3Publish publishMessage = Mqtt3Publish.builder().topic("homie/device123/$name").payload("hello".getBytes())
                .build();
        // Test if subscription is active
        connection.getSubscribers().get("homie/device123/$name").messageArrived(publishMessage);
        verify(subscriber).processMessage(eq("homie/device123/$name"), eq("hello".getBytes()));
    }

    @Test
    public void subscribeToWildcardTopic()
            throws ConfigurationException, MqttException, InterruptedException, ExecutionException, TimeoutException {
        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("123.123.123.123", null, false,
                "MqttBrokerConnectionTests");

        // Add a subscriber while still offline
        MqttMessageSubscriber subscriber = mock(MqttMessageSubscriber.class);
        connection.subscribe("homie/device123/+", subscriber);

        MqttMessageSubscriber subscriber2 = mock(MqttMessageSubscriber.class);
        connection.subscribe("#", subscriber2);

        MqttMessageSubscriber subscriber3 = mock(MqttMessageSubscriber.class);
        connection.subscribe("homie/#", subscriber3);

        assertTrue(connection.start().get(200, TimeUnit.MILLISECONDS));
        assertTrue(connection.hasSubscribers());
        assertThat(connection.connectionState(), is(MqttConnectionState.CONNECTED));

        Mqtt3Publish publishMessage = Mqtt3Publish.builder().topic("homie/device123/$name").payload("hello".getBytes())
                .build();
        connection.getSubscribers().get("homie/device123/+").messageArrived(publishMessage);
        connection.getSubscribers().get("#").messageArrived(publishMessage);
        connection.getSubscribers().get("homie/#").messageArrived(publishMessage);

        verify(subscriber).processMessage(eq("homie/device123/$name"), eq("hello".getBytes()));
        verify(subscriber2).processMessage(eq("homie/device123/$name"), eq("hello".getBytes()));
        verify(subscriber3).processMessage(eq("homie/device123/$name"), eq("hello".getBytes()));
    }

    @Test
    public void subscriber()
            throws ConfigurationException, MqttException, InterruptedException, ExecutionException, TimeoutException {
        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("123.123.123.123", null, false,
                "MqttBrokerConnectionTests");

        // Expect no subscribers
        assertFalse(connection.hasSubscribers());

        // Add subscribers (while not connected)
        MqttMessageSubscriber subscriber = mock(MqttMessageSubscriber.class);
        MqttMessageSubscriber subscriber2 = mock(MqttMessageSubscriber.class);
        connection.subscribe("utf8- topic äö:", subscriber);
        connection.subscribe("subscribe/to/multiple/$topics", subscriber);
        connection.subscribe("second/subscriber", subscriber);
        connection.subscribe("second/subscriber", subscriber2);
        assertTrue(connection.hasSubscribers());

        // Remove subscribers (while not connected)
        connection.unsubscribe("utf8- topic äö:", subscriber);
        connection.unsubscribe("subscribe/to/multiple/$topics", subscriber);
        connection.unsubscribe("second/subscriber", subscriber);
        connection.unsubscribe("second/subscriber", subscriber2);
        assertFalse(connection.hasSubscribers());

        assertTrue(connection.start().get(200, TimeUnit.MILLISECONDS));

        // Add subscriber (while connected)
        CompletableFuture<Boolean> future = connection.subscribe("topic", subscriber);
        verify(connection.client).subscribe(any(), anyInt(), any());
        assertTrue(future.get(200, TimeUnit.MILLISECONDS));

        // Remove subscriber (while connected)
        assertTrue(connection.unsubscribe("topic", subscriber).get(200, TimeUnit.MILLISECONDS));
    }

    @Test
    public void reconnectPolicyDefault() throws ConfigurationException, MqttException, InterruptedException {
        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("123.123.123.123", null, false,
                "MqttBrokerConnectionTests");

        // Check if the default policy is set and that the broker within the policy is set.
        assertTrue(connection.getReconnectStrategy() instanceof PeriodicReconnectStrategy);
        AbstractReconnectStrategy p = connection.getReconnectStrategy();
        assertThat(p.getBrokerConnection(), equalTo(connection));
    }

    @Test
    public void reconnectPolicy()
            throws ConfigurationException, MqttException, InterruptedException, ConfigurationException {
        MqttBrokerConnectionEx connection = spy(
                new MqttBrokerConnectionEx("123.123.123.123", null, false, "MqttBrokerConnectionTests"));
        connection.setConnectionCallback(connection);

        // Check setter
        connection.setReconnectStrategy(new PeriodicReconnectStrategy());
        assertThat(connection.getReconnectStrategy().getBrokerConnection(), equalTo(connection));

        // Prepare a Mock to test if lostConnect is called and
        // if the PeriodicReconnectPolicy indeed calls start()
        PeriodicReconnectStrategy mockPolicy = spy(new PeriodicReconnectStrategy(10000, 0));
        doReturn(CompletableFuture.completedFuture(true)).when(connection).start();
        mockPolicy.start();

        // Fake a disconnect
        connection.setReconnectStrategy(mockPolicy);
        doReturn(MqttConnectionState.DISCONNECTED).when(connection).connectionState();

        connection.isConnecting = true; /* Pretend that start did something */
        connection.connectionCallback.onDisconnected(new Throwable("disconnected"));

        // Check lostConnect
        verify(mockPolicy).lostConnection();
        waitForAssert(() -> verify(connection).start());
        assertTrue(mockPolicy.isReconnecting());

        // Fake connection established
        connection.connectionCallback.onConnected(null);
        assertFalse(mockPolicy.isReconnecting());
    }

    @Test
    public void timeoutWhenNotReachable() throws ConfigurationException, MqttException, InterruptedException {
        MqttBrokerConnectionEx connection = spy(
                new MqttBrokerConnectionEx("10.0.10.10", null, false, "MqttBrokerConnectionTests"));
        connection.setConnectionCallback(connection);

        ScheduledThreadPoolExecutor s = new ScheduledThreadPoolExecutor(1);
        connection.setTimeoutExecutor(s, 10);
        assertThat(connection.timeoutExecutor, is(s));

        // Adjust test conditions
        connection.connectTimeout = true;

        CountDownLatch latch = new CountDownLatch(2);

        MqttConnectionObserver o = new MqttConnectionObserver() {
            @Override
            public void connectionStateChanged(MqttConnectionState state, @Nullable Throwable error) {
                if (state == MqttConnectionState.DISCONNECTED) {
                    latch.countDown();
                }
                if (state == MqttConnectionState.CONNECTING) {
                    latch.countDown();
                }
            }
        };
        connection.addConnectionObserver(o);
        connection.start();
        assertNotNull(connection.timeoutFuture);

        assertThat(latch.await(500, TimeUnit.MILLISECONDS), is(true));
    }

    @Test
    public void timeoutWhenNotReachableFuture()
            throws ConfigurationException, MqttException, InterruptedException, ExecutionException, TimeoutException {
        MqttBrokerConnectionEx connection = spy(
                new MqttBrokerConnectionEx("10.0.10.10", null, false, "MqttBrokerConnectionTests"));
        connection.setConnectionCallback(connection);

        ScheduledThreadPoolExecutor s = new ScheduledThreadPoolExecutor(1);
        connection.setTimeoutExecutor(s, 10);
        assertThat(connection.timeoutExecutor, is(s));

        // Adjust test conditions
        connection.connectTimeout = true;

        CompletableFuture<Boolean> future = connection.start();
        verify(connection.connectionCallback).createFuture();
        verify(connection.connectionCallback, times(0)).onConnected(any());
        verify(connection.connectionCallback, times(0)).onDisconnected(any(MqttClientDisconnectedContext.class));
        assertNotNull(connection.timeoutFuture);

        assertThat(future.get(70, TimeUnit.MILLISECONDS), is(false));
    }

    @Test
    public void connectionObserver() throws ConfigurationException, MqttException {
        MqttBrokerConnectionEx connection = spy(
                new MqttBrokerConnectionEx("123.123.123.123", null, false, "connectionObserver"));
        connection.setConnectionCallback(connection);

        // Add an observer
        assertFalse(connection.hasConnectionObservers());
        MqttConnectionObserver connectionObserver = mock(MqttConnectionObserver.class);
        connection.addConnectionObserver(connectionObserver);
        assertTrue(connection.hasConnectionObservers());

        // Adding a connection observer should not immediately call its connectionStateChanged() method.
        verify(connectionObserver, times(0)).connectionStateChanged(eq(MqttConnectionState.DISCONNECTED), any());

        // Cause a success callback
        connection.connectionStateOverwrite = MqttConnectionState.CONNECTED;
        connection.connectionCallback.onConnected(null);
        verify(connectionObserver, times(1)).connectionStateChanged(eq(MqttConnectionState.CONNECTED), isNull());

        Exception testException = new Exception("test message");

        connection.connectionStateOverwrite = MqttConnectionState.DISCONNECTED;
        connection.connectionCallback.onDisconnected(testException);
        verify(connectionObserver, times(1)).connectionStateChanged(eq(MqttConnectionState.DISCONNECTED),
                eq(testException));

        // Remove observer
        connection.removeConnectionObserver(connectionObserver);
        assertFalse(connection.hasConnectionObservers());
    }

    @Test
    public void lastWillAndTestamentTests() throws ConfigurationException {
        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("123.123.123.123", null, false,
                "lastWillAndTestamentTests");

        assertNull(connection.getLastWill());
        assertNull(MqttWillAndTestament.fromString(""));
        connection.setLastWill(MqttWillAndTestament.fromString("topic:message:1:true"));
        assertEquals("topic", connection.getLastWill().getTopic());
        assertEquals(1, connection.getLastWill().getQos());
        assertTrue(connection.getLastWill().isRetain());
        byte[] b = { 'm', 'e', 's', 's', 'a', 'g', 'e' };
        assertArrayEquals(connection.getLastWill().getPayload(), b);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lastWillAndTestamentConstructorTests() {
        new MqttWillAndTestament("", new byte[0], 0, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void qosInvalid() throws ConfigurationException {
        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("123.123.123.123", null, false, "qosInvalid");
        connection.setQos(10);
    }

    @Test
    public void setterGetterTests() {
        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("123.123.123.123", null, false,
                "setterGetterTests");
        assertEquals("URL getter", connection.getHost(), "123.123.123.123");
        assertEquals("Name getter", connection.getPort(), 1883); // Check for non-secure port
        assertFalse("Secure getter", connection.isSecure());
        assertEquals("ClientID getter", "setterGetterTests", connection.getClientId());

        connection.setCredentials("user@!", "password123@^");
        assertEquals("User getter/setter", "user@!", connection.getUser());
        assertEquals("Password getter/setter", "password123@^", connection.getPassword());

        assertEquals(MqttBrokerConnection.DEFAULT_KEEPALIVE_INTERVAL, connection.getKeepAliveInterval());
        connection.setKeepAliveInterval(80);
        assertEquals(80, connection.getKeepAliveInterval());

        assertFalse(connection.isRetain());
        connection.setRetain(true);
        assertTrue(connection.isRetain());

        assertEquals(MqttBrokerConnection.DEFAULT_QOS, connection.getQos());
        connection.setQos(2);
        assertEquals(2, connection.getQos());
        connection.setQos(1);
        assertEquals(1, connection.getQos());

        // Check for default ssl context provider and reconnect policy
        assertNotNull(connection.getReconnectStrategy());

        assertThat(connection.connectionState(), equalTo(MqttConnectionState.DISCONNECTED));
    }

    @SuppressWarnings("null")
    @Test
    public void gracefulStop()
            throws ConfigurationException, MqttException, InterruptedException, ExecutionException, TimeoutException {
        MqttBrokerConnectionEx connection = spy(
                new MqttBrokerConnectionEx("123.123.123.123", null, false, "MqttBrokerConnectionTests"));

        assertTrue(connection.start().get(200, TimeUnit.MILLISECONDS));

        // Add test subscribers
        MqttMessageSubscriber subscriber = mock(MqttMessageSubscriber.class);
        connection.subscribe("abc", subscriber);
        connection.subscribe("def/subtopic", subscriber);
        assertThat(connection.hasSubscribers(), is(true));

        // Let's observe the internal connection client
        MqttAsyncClientWrapper client = connection.client;

        // Stop
        CompletableFuture<Boolean> future = connection.stop();

        // Restart strategy must be stopped
        PeriodicReconnectStrategy p = (PeriodicReconnectStrategy) connection.getReconnectStrategy();
        assertThat(p.isStarted(), is(false));

        // Wait to complete stop
        future.get(1000, TimeUnit.MILLISECONDS);

        verify(connection).unsubscribeAll();
        verify(client).disconnect();

        // Subscribers should be removed
        assertThat(connection.hasSubscribers(), is(false));
    }
}
