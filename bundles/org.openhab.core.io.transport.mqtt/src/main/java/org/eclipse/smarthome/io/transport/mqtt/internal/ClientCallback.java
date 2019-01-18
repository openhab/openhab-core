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
package org.eclipse.smarthome.io.transport.mqtt.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnection;
import org.eclipse.smarthome.io.transport.mqtt.MqttConnectionObserver;
import org.eclipse.smarthome.io.transport.mqtt.MqttConnectionState;
import org.eclipse.smarthome.io.transport.mqtt.MqttException;
import org.eclipse.smarthome.io.transport.mqtt.MqttMessageSubscriber;
import org.eclipse.smarthome.io.transport.mqtt.reconnect.AbstractReconnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes the paho MqttCallbacks for the {@link MqttBrokerConnection}.
 *
 * @author David Graeff - Initial contribution
 */
public class ClientCallback implements MqttCallback {
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

    @Override
    public synchronized void connectionLost(@Nullable Throwable exception) {
        if (exception instanceof MqttException) {
            MqttException e = (MqttException) exception;
            logger.info("MQTT connection to '{}' was lost: {} : ReasonCode {} : Cause : {}", connection.getHost(),
                    e.getMessage(), e.getReasonCode(), (e.getCause() == null ? "Unknown" : e.getCause().getMessage()));
        } else if (exception != null) {
            logger.info("MQTT connection to '{}' was lost", connection.getHost(), exception);
        }

        connectionObservers.forEach(o -> o.connectionStateChanged(MqttConnectionState.DISCONNECTED, exception));
        AbstractReconnectStrategy reconnectStrategy = connection.getReconnectStrategy();
        if (reconnectStrategy != null) {
            reconnectStrategy.lostConnection();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.trace("Message with id {} delivered.", token.getMessageId());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        byte[] payload = message.getPayload();
        logger.trace("Received message on topic '{}' : {}", topic, new String(payload));
        List<MqttMessageSubscriber> matches = new ArrayList<>();
        synchronized (subscribers) {
            subscribers.values().forEach(subscriberList -> {
                if (topic.matches(subscriberList.regexMatchTopic)) {
                    logger.trace("Topic match for '{}' using regex {}", topic, subscriberList.regexMatchTopic);
                    subscriberList.forEach(consumer -> matches.add(consumer));
                } else {
                    logger.trace("No topic match for '{}' using regex {}", topic, subscriberList.regexMatchTopic);

                }
            });

        }
        try {
            matches.forEach(subscriber -> subscriber.processMessage(topic, payload));
        } catch (Exception e) {
            logger.error("MQTT message received. MqttMessageSubscriber#processMessage() implementation failure", e);
        }
    }
}
