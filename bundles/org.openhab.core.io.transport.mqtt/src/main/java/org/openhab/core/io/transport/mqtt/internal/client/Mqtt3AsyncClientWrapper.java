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

import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection.ConnectionCallback;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection.Protocol;
import org.openhab.core.io.transport.mqtt.MqttWillAndTestament;
import org.openhab.core.io.transport.mqtt.internal.Subscription;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3ConnectBuilder;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import com.hivemq.client.mqtt.mqtt3.message.unsubscribe.Mqtt3Unsubscribe;

/**
 * The {@link Mqtt3AsyncClientWrapper} provides the wrapper for Mqttv3 async clients
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Mqtt3AsyncClientWrapper extends MqttAsyncClientWrapper {
    private final Mqtt3AsyncClient client;

    public Mqtt3AsyncClientWrapper(String host, int port, String clientId, Protocol protocol, boolean secure,
            ConnectionCallback connectionCallback, @Nullable TrustManagerFactory trustManagerFactory) {
        Mqtt3ClientBuilder clientBuilder = Mqtt3Client.builder().serverHost(host).serverPort(port).identifier(clientId)
                .addConnectedListener(connectionCallback).addDisconnectedListener(connectionCallback);

        if (protocol == Protocol.WEBSOCKETS) {
            clientBuilder.webSocketWithDefaultConfig();
        }
        if (secure) {
            clientBuilder.sslWithDefaultConfig().sslConfig().trustManagerFactory(trustManagerFactory).applySslConfig();
        }

        client = clientBuilder.buildAsync();
    }

    @Override
    public MqttClientState getState() {
        return client.getState();
    }

    @Override
    public CompletableFuture<?> subscribe(String topic, int qos, Subscription subscription) {
        Mqtt3Subscribe subscribeMessage = Mqtt3Subscribe.builder().topicFilter(topic).qos(getMqttQosFromInt(qos))
                .build();
        return client.subscribe(subscribeMessage, subscription::messageArrived);
    }

    @Override
    public CompletableFuture<?> unsubscribe(String topic) {
        Mqtt3Unsubscribe unsubscribeMessage = Mqtt3Unsubscribe.builder().topicFilter(topic).build();
        return client.unsubscribe(unsubscribeMessage);
    }

    @Override
    public CompletableFuture<Mqtt3Publish> publish(String topic, byte[] payload, boolean retain, int qos) {
        Mqtt3Publish publishMessage = Mqtt3Publish.builder().topic(topic).qos(getMqttQosFromInt(qos)).payload(payload)
                .retain(retain).build();
        return client.publish(publishMessage);
    }

    @Override
    public CompletableFuture<?> connect(@Nullable MqttWillAndTestament lwt, int keepAliveInterval,
            @Nullable String username, @Nullable String password) {
        Mqtt3ConnectBuilder connectMessageBuilder = Mqtt3Connect.builder().keepAlive(keepAliveInterval);
        if (lwt != null) {
            Mqtt3Publish willPublish = Mqtt3Publish.builder().topic(lwt.getTopic()).payload(lwt.getPayload())
                    .retain(lwt.isRetain()).qos(getMqttQosFromInt(lwt.getQos())).build();
            connectMessageBuilder.willPublish(willPublish);
        }

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            connectMessageBuilder.simpleAuth().username(username).password(password.getBytes()).applySimpleAuth();
        }

        return client.connect(connectMessageBuilder.build());
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return client.disconnect();
    }
}
