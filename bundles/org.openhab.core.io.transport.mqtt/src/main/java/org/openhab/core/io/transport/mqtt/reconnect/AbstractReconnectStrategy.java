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
package org.openhab.core.io.transport.mqtt.reconnect;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttConnectionObserver;

/**
 * Implement this class to provide a strategy for (re)establishing a lost
 * broker connection.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractReconnectStrategy {
    protected @Nullable MqttBrokerConnection brokerConnection;

    /**
     * Will be called by {@see MqttBrokerConnection.setReconnectPolicy()}.
     *
     * @param mqttBrokerConnectionImpl The broker connection
     */
    public void setBrokerConnection(MqttBrokerConnection mqttBrokerConnectionImpl) {
        this.brokerConnection = mqttBrokerConnectionImpl;
    }

    /**
     * Return the brokerConnection object that this reconnect policy is assigned to.
     */
    public @Nullable MqttBrokerConnection getBrokerConnection() {
        return brokerConnection;
    }

    /**
     * Return true if your implementation is trying to establish a connection, false otherwise.
     */
    public abstract boolean isReconnecting();

    /**
     * The {@link MqttConnectionObserver} will call this method if a broker connection has been lost
     * or couldn't be established. Your implementation should start trying to reestablish a connection.
     */
    public abstract void lostConnection();

    /**
     * The {@link MqttConnectionObserver} will call this method if a broker connection has been
     * successfully established. Your implementation should stop reconnection attempts and release
     * resources.
     */
    public abstract void connectionEstablished();

    /**
     * Start the reconnect strategy handling.
     */
    public abstract void start();

    /**
     * Stop the reconnect strategy handling.
     *
     * <p>
     * It must be possible to restart a reconnect strategy again after it has been stopped.
     */
    public abstract void stop();

}
