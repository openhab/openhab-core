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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.internal.MqttBrokerConnectionServiceInstance;

/**
 * This service allows you to enumerate system-wide configured Mqtt broker connections. You do not need this service
 * if you want to manage own/local Mqtt broker connections. If you add a broker connection, it will be
 * available immediately for all MqttService users. A removed broker connection may still be in use by consuming
 * services.
 *
 * Added/removed connections are not permanent. Use {@link MqttBrokerConnectionServiceInstance} to configure permanent
 * connections.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public interface MqttService {

    /**
     * Add a listener to get notified of new/removed brokers.
     *
     * @param observer The observer
     */
    void addBrokersListener(MqttServiceObserver observer);

    /**
     * Remove a listener and don't get notified of new/removed brokers anymore.
     *
     * @param observer The observer
     */
    void removeBrokersListener(MqttServiceObserver observer);

    /**
     * Return true if a broker listener has been added via addBrokersListener().
     */
    boolean hasBrokerObservers();

    /**
     * Lookup an broker connection by name.
     *
     * @param brokerName to look for.
     * @return existing connection or null
     */
    @Nullable
    MqttBrokerConnection getBrokerConnection(String brokerName);

    /**
     * Adds a broker connection to the service.
     * The broker connection state will not be altered (started/stopped).
     *
     * It is your responsibility to remove the broker connection again by calling
     * removeBrokerConnection(brokerID).
     *
     * @param brokerID The broker connection will be identified by this ID. The ID must be unique within the service.
     * @param connection The broker connection object
     * @return Return true if the connection could be added successfully, return false if there is already
     *         an existing connection with the same name.
     */
    boolean addBrokerConnection(String brokerID, MqttBrokerConnection connection);

    /**
     * Remove a broker connection by name
     *
     * @param brokerName The broker ID
     * @return Returns the removed broker connection, or null if there was none with the given name.
     */
    @Nullable
    MqttBrokerConnection removeBrokerConnection(String brokerID);

    /**
     * Returns an unmodifiable map with all configured brokers of this service and the broker ID as keys.
     */
    Map<String, MqttBrokerConnection> getAllBrokerConnections();
}
