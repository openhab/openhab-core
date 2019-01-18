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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Implement this interface to get notified of new and removed MQTT broker.
 * Register this observer at {@see MqttService}.
 *
 * @author David Graeff - Initial contribution and API
 */
@NonNullByDefault
public interface MqttServiceObserver {
    /**
     * Called, if a new broker has been added to the {@see MqttService}.
     * If a broker connection is replaced, you will be notified by a brokerRemoved call,
     * followed by a brokerAdded call.
     *
     * @param The unique ID within {@link MqttService}
     * @param broker The new broker connection
     */
    void brokerAdded(String brokerID, MqttBrokerConnection broker);

    /**
     * Called, if a broker has been removed from the {@see MqttService}.
     *
     * @param The unique ID within {@link MqttService}
     * @param broker The removed broker connection.
     *            Please unsubscribe from all topics and unregister
     *            all your listeners.
     */
    void brokerRemoved(String brokerID, MqttBrokerConnection broker);
}
