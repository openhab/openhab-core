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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Implement this interface and register on the {@see MqttBrokerConnection} to get notified
 * of incoming Mqtt messages on the given topic.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public interface MqttMessageSubscriber {
    /**
     * Process a received MQTT message.
     *
     * @param topic The mqtt topic on which the message was received.
     * @param payload content of the message.
     */
    public void processMessage(String topic, byte[] payload);
}
