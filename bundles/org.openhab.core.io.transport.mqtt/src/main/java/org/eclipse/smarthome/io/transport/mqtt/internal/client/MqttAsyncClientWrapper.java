/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.transport.mqtt.internal.client;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.transport.mqtt.MqttWillAndTestament;
import org.eclipse.smarthome.io.transport.mqtt.internal.ClientCallback;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;

/**
 * The {@link AbstractMqttAsyncClient} is the base class for async client wrappers
 *
 * @author Jan N. Klug - Initial contribution
 */

@NonNullByDefault
public abstract class MqttAsyncClientWrapper {
    public abstract CompletableFuture<?> connect(@Nullable MqttWillAndTestament lwt, int keepAliveInterval,
            @Nullable String username, @Nullable String password);

    public abstract CompletableFuture<Void> disconnect();

    public abstract MqttClientState getState();

    public abstract CompletableFuture<?> subscribe(String topic, int qos, ClientCallback clientCallback);

    public abstract CompletableFuture<?> unsubscribe(String topic);

    public abstract CompletableFuture<?> publish(String topic, byte[] payload, boolean retain, int qos);

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
