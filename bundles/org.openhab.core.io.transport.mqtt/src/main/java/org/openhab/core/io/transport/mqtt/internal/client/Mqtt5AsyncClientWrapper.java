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

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection.ConnectionCallback;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection.Protocol;
import org.openhab.core.io.transport.mqtt.MqttWillAndTestament;
import org.openhab.core.io.transport.mqtt.internal.Subscription;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.client.mqtt.mqtt5.message.unsubscribe.Mqtt5Unsubscribe;

/**
 * The {@link Mqtt5AsyncClientWrapper} provides the wrapper for Mqttv3 async clients
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Mqtt5AsyncClientWrapper extends MqttAsyncClientWrapper {
    private final Mqtt5AsyncClient client;

    public Mqtt5AsyncClientWrapper(String host, int port, String clientId, Protocol protocol, boolean secure,
            ConnectionCallback connectionCallback, @Nullable TrustManagerFactory trustManagerFactory) {
        Mqtt5ClientBuilder clientBuilder = Mqtt5Client.builder().serverHost(host).serverPort(port).identifier(clientId)
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
        Mqtt5Subscribe subscribeMessage = Mqtt5Subscribe.builder().topicFilter(topic).qos(getMqttQosFromInt(qos))
                .build();
        return client.subscribe(subscribeMessage, subscription::messageArrived);
    }

    @Override
    public CompletableFuture<?> unsubscribe(String topic) {
        Mqtt5Unsubscribe unsubscribeMessage = Mqtt5Unsubscribe.builder().topicFilter(topic).build();
        return client.unsubscribe(unsubscribeMessage);
    }

    @Override
    public CompletableFuture<Mqtt5PublishResult> publish(String topic, byte[] payload, boolean retain, int qos) {
        Mqtt5Publish publishMessage = Mqtt5Publish.builder().topic(topic).qos(getMqttQosFromInt(qos)).payload(payload)
                .retain(retain).build();
        return client.publish(publishMessage);
    }

    @Override
    public CompletableFuture<?> connect(@Nullable MqttWillAndTestament lwt, int keepAliveInterval,
            @Nullable String username, @Nullable String password) {
        Mqtt5ConnectBuilder connectMessageBuilder = Mqtt5Connect.builder().keepAlive(keepAliveInterval);
        if (lwt != null) {
            Mqtt5Publish willPublish = Mqtt5Publish.builder().topic(lwt.getTopic()).payload(lwt.getPayload())
                    .retain(lwt.isRetain()).qos(getMqttQosFromInt(lwt.getQos())).build();
            connectMessageBuilder.willPublish(willPublish);
        }

        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password) && password != null) {
            connectMessageBuilder.simpleAuth().username(username).password(password.getBytes()).applySimpleAuth();
        }

        return client.connect(connectMessageBuilder.build());
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return client.disconnect();
    }

}
