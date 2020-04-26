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
package org.openhab.core.io.transport.mqtt.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnectionConfig;
import org.openhab.core.io.transport.mqtt.MqttException;
import org.openhab.core.io.transport.mqtt.MqttService;
import org.openhab.core.io.transport.mqtt.MqttServiceObserver;
import org.openhab.core.io.transport.mqtt.MqttWillAndTestament;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link MqttService}.
 *
 * @author Davy Vanherbergen - Initial contribution
 * @author David Graeff - Added/Removed observer interface, Add/Remove/Enumerate broker connections.
 * @author Markus Rathgeb - Synchronize access to broker connections
 */
@Component(immediate = true, service = MqttService.class, configurationPid = "org.openhab.mqtt", property = {
        Constants.SERVICE_PID + "=org.openhab.mqtt" })
@NonNullByDefault
public class MqttServiceImpl implements MqttService {
    private final Logger logger = LoggerFactory.getLogger(MqttServiceImpl.class);
    private final Map<String, MqttBrokerConnection> brokerConnections = new ConcurrentHashMap<>();
    private final List<MqttServiceObserver> brokersObservers = new CopyOnWriteArrayList<>();

    @Override
    public void addBrokersListener(MqttServiceObserver observer) {
        brokersObservers.add(observer);
    }

    @Override
    public void removeBrokersListener(MqttServiceObserver observer) {
        brokersObservers.remove(observer);
    }

    @Override
    public boolean hasBrokerObservers() {
        return !brokersObservers.isEmpty();
    }

    @Override
    public @Nullable MqttBrokerConnection getBrokerConnection(String brokerName) {
        synchronized (brokerConnections) {
            return brokerConnections.get(brokerName);
        }
    }

    @Override
    public boolean addBrokerConnection(String brokerID, MqttBrokerConnection connection) {
        synchronized (brokerConnections) {
            if (brokerConnections.containsKey(brokerID)) {
                return false;
            }
            brokerConnections.put(brokerID, connection);
        }
        brokersObservers.forEach(o -> o.brokerAdded(brokerID, connection));
        return true;
    }

    protected @Nullable MqttBrokerConnection addBrokerConnection(String brokerID, MqttBrokerConnectionConfig config)
            throws ConfigurationException, MqttException {
        MqttBrokerConnection connection;
        synchronized (brokerConnections) {
            if (brokerConnections.containsKey(brokerID)) {
                return null;
            }
            String host = config.host;
            if (host != null && !host.isBlank()) {
                connection = new MqttBrokerConnection(host, config.port, config.secure, config.clientID);
                brokerConnections.put(brokerID, connection);
            } else {
                throw new ConfigurationException("host", "You need to provide a hostname/IP!");
            }
        }

        // Extract further configurations
        connection.setCredentials(config.username, config.password);
        if (config.keepAlive != null) {
            connection.setKeepAliveInterval(config.keepAlive.intValue());
        }

        connection.setQos(config.qos.intValue());
        connection.setRetain(config.retainMessages);
        if (config.lwtTopic != null) {
            String topic = config.lwtTopic;
            MqttWillAndTestament will = new MqttWillAndTestament(topic,
                    config.lwtMessage != null ? config.lwtMessage.getBytes() : null, config.lwtQos, config.lwtRetain);
            logger.debug("Setting last will: {}", will);
            connection.setLastWill(will);
        }

        brokersObservers.forEach(o -> o.brokerAdded(brokerID, connection));
        return connection;
    }

    @SuppressWarnings("null")
    @Override
    public @Nullable MqttBrokerConnection removeBrokerConnection(String brokerID) {
        synchronized (brokerConnections) {
            final @Nullable MqttBrokerConnection connection = brokerConnections.remove(brokerID);
            if (connection != null) {
                brokersObservers.forEach(o -> o.brokerRemoved(brokerID, connection));
            }
            return connection;
        }
    }

    @Override
    public Map<String, MqttBrokerConnection> getAllBrokerConnections() {
        synchronized (brokerConnections) {
            return Collections.unmodifiableMap(brokerConnections);
        }
    }
}
