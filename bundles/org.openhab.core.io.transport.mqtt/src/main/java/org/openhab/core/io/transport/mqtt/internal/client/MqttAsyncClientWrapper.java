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
package org.openhab.core.io.transport.mqtt.internal.client;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.MqttWillAndTestament;
import org.openhab.core.io.transport.mqtt.internal.Subscription;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;

/**
 * The {@link MqttAsyncClientWrapper} is the base class for async client wrappers
 *
 * @author Jan N. Klug - Initial contribution
 */

@NonNullByDefault
public abstract class MqttAsyncClientWrapper {
    /**
     * connect this client
     *
     * @param lwt last-will and testament (optional)
     * @param keepAliveInterval keep-alive interval in ms
     * @param username username (optional)
     * @param password password (optional)
     * @return a CompletableFuture (exceptionally on fail)
     */
    public abstract CompletableFuture<?> connect(@Nullable MqttWillAndTestament lwt, int keepAliveInterval,
            @Nullable String username, @Nullable String password);

    /**
     * disconnect this client
     *
     * @return a CompletableFuture (exceptionally on fail)
     */
    public abstract CompletableFuture<Void> disconnect();

    /**
     * get the connection state of this client
     *
     * @return the client state
     */
    public abstract MqttClientState getState();

    /**
     * publish a message
     *
     * @param topic the topic
     * @param payload the message as byte array
     * @param retain whether this message should be retained
     * @param qos the QoS level of this message
     * @return a CompletableFuture (exceptionally on fail)
     */
    public abstract CompletableFuture<?> publish(String topic, byte[] payload, boolean retain, int qos);

    /**
     * subscribe a client callback to a topic
     *
     * @param topic the topic
     * @param qos QoS for this subscription
     * @param subscription the subscription which keeps track of subscribers and retained messages
     * @return a CompletableFuture (exceptionally on fail)
     */
    public abstract CompletableFuture<?> subscribe(String topic, int qos, Subscription subscription);

    /**
     * unsubscribes from a topic
     *
     * @param topic the topic
     * @return a CompletableFuture (exceptionally on fail)
     */
    public abstract CompletableFuture<?> unsubscribe(String topic);

    protected MqttQos getMqttQosFromInt(int qos) {
        switch (qos) {
            case 0:
                return MqttQos.AT_LEAST_ONCE;
            case 1:
                return MqttQos.AT_MOST_ONCE;
            case 2:
                return MqttQos.EXACTLY_ONCE;
            default:
                throw new IllegalArgumentException("QoS needs to be 0, 1 or 2.");
        }
    }
}
