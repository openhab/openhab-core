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
package org.eclipse.smarthome.io.transport.mqtt;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.smarthome.io.transport.mqtt.reconnect.AbstractReconnectStrategy;
import org.eclipse.smarthome.io.transport.mqtt.reconnect.PeriodicReconnectStrategy;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

/**
 * Tests the MqttBrokerConnection class
 *
 * @author David Graeff - Initial contribution
 */
public class MqttBrokerConnectionTests {
    @Test
    public void subscribeBeforeOnlineThenConnect() throws ConfigurationException, MqttException, InterruptedException,
            ExecutionException, TimeoutException, org.eclipse.paho.client.mqttv3.MqttException {
        MqttBrokerConnectionEx connection = new MqttBrokerConnectionEx("123.123.123.123", null, false,
                "MqttBrokerConnectionTests");

        // Add a subscriber while still offline
        MqttMessageSubscriber subscriber = mock(MqttMessageSubscriber.class);
        connection.subscribe("homie/device123/$name", subscriber);

        assertTrue(connection.start().get(200, TimeUnit.MILLISECONDS));
        assertTrue(connection.hasSubscribers());
        assertThat(connection.connectionState(), is(MqttConnectionState.CONNECTED));

        // Test if subscription is active
        connection.clientCallback.messageArrived("homie/device123/$name", new MqttMessage("hello".getBytes()));
        verify(subscriber).processMessage(eq("homie/device123/$name"), eq("hello".getBytes()));
    }

    @Test
    public void subscribeToWildcardTopic() throws ConfigurationException, MqttException, InterruptedException,
            ExecutionException, TimeoutException, org.eclipse.paho.client.mqttv3.MqttException {
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

        connection.clientCallback.messageArrived("homie/device123/$name", new MqttMessage("hello".getBytes()));

        verify(subscriber).processMessage(eq("homie/device123/$name"), eq("hello".getBytes()));
        verify(subscriber2).processMessage(eq("homie/device123/$name"), eq("hello".getBytes()));
        verify(subscriber3).processMessage(eq("homie/device123/$name"), eq("hello".getBytes()));
    }

    @Test
    public void subscriber() throws ConfigurationException, MqttException, InterruptedException, ExecutionException,
            TimeoutException, org.eclipse.paho.client.mqttv3.MqttException {
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
        verify(connection.client).subscribe(any(), anyInt(), any(), any());
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
        IMqttToken token = mock(IMqttToken.class);
        when(token.getException()).thenReturn(new org.eclipse.paho.client.mqttv3.MqttException(1));

        connection.isConnecting = true; /* Pretend that start did something */
        connection.connectionCallback.onFailure(token, null);

        // Check lostConnect
        verify(mockPolicy).lostConnection();
        Thread.sleep(10);
        verify(connection).start();
        assertTrue(mockPolicy.isReconnecting());

        // Fake connection established
        connection.connectionCallback.onSuccess(token);
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
            public void connectionStateChanged(@NonNull MqttConnectionState state, @Nullable Throwable error) {
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
        verify(connection.connectionCallback, times(0)).onSuccess(any());
        verify(connection.connectionCallback, times(0)).onFailure(any(), any());
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
        connection.connectionCallback.onSuccess(null);
        verify(connectionObserver, times(1)).connectionStateChanged(eq(MqttConnectionState.CONNECTED), isNull());

        // Cause a failure callback with a mocked token
        IMqttToken token = mock(IMqttToken.class);
        org.eclipse.paho.client.mqttv3.MqttException testException = new org.eclipse.paho.client.mqttv3.MqttException(
                1);
        when(token.getException()).thenReturn(testException);

        connection.connectionStateOverwrite = MqttConnectionState.DISCONNECTED;
        connection.connectionCallback.onFailure(token, null);
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
        assertTrue(connection.getLastWill().getTopic().equals("topic"));
        assertEquals(1, connection.getLastWill().getQos());
        assertEquals(true, connection.getLastWill().isRetain());
        byte b[] = { 'm', 'e', 's', 's', 'a', 'g', 'e' };
        assertTrue(Arrays.equals(connection.getLastWill().getPayload(), b));
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
        assertEquals("Secure getter", connection.isSecure(), false);
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
        assertNotNull(connection.getSSLContextProvider());
        assertNotNull(connection.getReconnectStrategy());

        assertThat(connection.connectionState(), equalTo(MqttConnectionState.DISCONNECTED));
    }

    @SuppressWarnings("null")
    @Test
    public void gracefulStop() throws ConfigurationException, MqttException, InterruptedException, ExecutionException,
            TimeoutException, org.eclipse.paho.client.mqttv3.MqttException {
        MqttBrokerConnectionEx connection = spy(
                new MqttBrokerConnectionEx("123.123.123.123", null, false, "MqttBrokerConnectionTests"));

        assertTrue(connection.start().get(200, TimeUnit.MILLISECONDS));

        // Add test subscribers
        MqttMessageSubscriber subscriber = mock(MqttMessageSubscriber.class);
        connection.subscribe("abc", subscriber);
        connection.subscribe("def/subtopic", subscriber);
        assertThat(connection.hasSubscribers(), is(true));

        // Let's observe the internal connection client
        MqttAsyncClientEx client = (MqttAsyncClientEx) connection.client;

        // Stop
        CompletableFuture<Boolean> future = connection.stop();

        // Restart strategy must be stopped
        PeriodicReconnectStrategy p = (PeriodicReconnectStrategy) connection.getReconnectStrategy();
        assertThat(p.isStarted(), is(false));

        // Wait to complete stop
        future.get(200, TimeUnit.MILLISECONDS);

        verify(connection).unsubscribeAll();
        verify(client).disconnect(anyLong(), any(), any());

        // Subscribers should be removed
        assertThat(connection.hasSubscribers(), is(false));
    }

}
