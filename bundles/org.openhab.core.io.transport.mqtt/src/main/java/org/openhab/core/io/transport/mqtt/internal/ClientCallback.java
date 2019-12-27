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
package org.openhab.core.io.transport.mqtt.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttConnectionObserver;
import org.openhab.core.io.transport.mqtt.MqttConnectionState;
import org.openhab.core.io.transport.mqtt.MqttException;
import org.openhab.core.io.transport.mqtt.MqttMessageSubscriber;
import org.openhab.core.io.transport.mqtt.reconnect.AbstractReconnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

/**
 * Processes the MqttCallbacks for the {@link MqttBrokerConnection}.
 *
 * @author David Graeff - Initial contribution
 * @author Jan N. Klug - adjusted to HiveMQ client
 */
@NonNullByDefault
public class ClientCallback {
    final Logger logger = LoggerFactory.getLogger(ClientCallback.class);
    private final MqttBrokerConnection connection;
    private final List<MqttConnectionObserver> connectionObservers;
    private final Map<String, TopicSubscribers> subscribers;

    public ClientCallback(MqttBrokerConnection mqttBrokerConnectionImpl,
            List<MqttConnectionObserver> connectionObservers, Map<String, TopicSubscribers> subscribers) {
        this.connection = mqttBrokerConnectionImpl;
        this.connectionObservers = connectionObservers;
        this.subscribers = subscribers;
    }

    public synchronized void connectionLost(@Nullable Throwable exception) {
        if (exception instanceof MqttException) {
            MqttException e = (MqttException) exception;
            logger.info("MQTT connection to '{}' was lost: {} : Cause : {}", connection.getHost(), e.getMessage(),
                    e.getCause().getMessage());
        } else if (exception != null) {
            logger.info("MQTT connection to '{}' was lost", connection.getHost(), exception);
        }

        connectionObservers.forEach(o -> o.connectionStateChanged(MqttConnectionState.DISCONNECTED, exception));
        AbstractReconnectStrategy reconnectStrategy = connection.getReconnectStrategy();
        if (reconnectStrategy != null) {
            reconnectStrategy.lostConnection();
        }
    }

    public void messageArrived(Mqtt3Publish message) {
        messageArrived(message.getTopic(), message.getPayloadAsBytes());
    }

    public void messageArrived(Mqtt5Publish message) {
        messageArrived(message.getTopic(), message.getPayloadAsBytes());
    }

    private void messageArrived(MqttTopic topic, byte[] payload) {
        String topicString = topic.toString();
        logger.trace("Received message on topic '{}' : {}", topic, new String(payload));

        List<MqttMessageSubscriber> matchingSubscribers = new ArrayList<>();
        synchronized (subscribers) {
            subscribers.values().forEach(subscriberList -> {
                if (subscriberList.topicMatch(topicString)) {
                    logger.trace("Topic match for '{}' using regex {}", topic, subscriberList.getTopicRegexPattern());
                    subscriberList.forEach(consumer -> matchingSubscribers.add(consumer));
                } else {
                    logger.trace("No topic match for '{}' using regex {}", topic,
                            subscriberList.getTopicRegexPattern());
                }
            });
        }

        try {
            matchingSubscribers.forEach(subscriber -> subscriber.processMessage(topicString, payload));
        } catch (Exception e) {
            logger.error("MQTT message received. MqttMessageSubscriber#processMessage() implementation failure", e);
        }
    }
}
