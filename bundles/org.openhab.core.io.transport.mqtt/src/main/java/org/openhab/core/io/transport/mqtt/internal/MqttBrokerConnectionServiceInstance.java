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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnectionConfig;
import org.openhab.core.io.transport.mqtt.MqttException;
import org.openhab.core.io.transport.mqtt.MqttService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The user can configure multiple system Mqtt broker connections. This is realized via the OSGI service factory
 * pattern.
 * ESH requires a factory marker service, implemented in {@link MqttBrokerConnectionServiceInstanceMarker}.
 * This service represents an instance of that factory and will initialize one MQTT broker connection with the given
 * configuration and register it to the {@link MqttService}.
 *
 * @author David Graeff - Initial contribution
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, service = MqttBrokerConnectionServiceInstance.class, configurationPid = "org.openhab.mqttbroker")
@NonNullByDefault
public class MqttBrokerConnectionServiceInstance {
    private final Logger logger = LoggerFactory.getLogger(MqttBrokerConnectionServiceInstance.class);

    private @Nullable MqttBrokerConnection connection;
    private final MqttService mqttService;

    @Activate
    public MqttBrokerConnectionServiceInstance(final @Reference MqttService mqttService) {
        this.mqttService = mqttService;
    }

    /**
     * Create broker connections based on the service configuration. This will disconnect and
     * discard all existing textual configured brokers.
     */
    @Modified
    public void modified(@Nullable Map<String, Object> configMap) {
        if (connection != null) {
            connection.stop();
        }

        final MqttServiceImpl service = (MqttServiceImpl) mqttService;
        if (configMap == null || configMap.isEmpty()) {
            return;
        }

        // Parse configuration
        MqttBrokerConnectionConfig config = new Configuration(configMap).as(MqttBrokerConnectionConfig.class);

        try {
            // Compute brokerID and make sure it is not empty
            String brokerID = config.getBrokerID();
            if (brokerID == null || brokerID.isBlank()) {
                logger.warn("Ignore invalid broker connection configuration: {}", config);
                return;
            }

            // Add connection and make sure it succeeded
            MqttBrokerConnection c = service.addBrokerConnection(brokerID, config);
            connection = c;
            if (c == null) {
                logger.warn("Ignore existing broker connection configuration for: {}", brokerID);
                return;
            }
            c.start(); // Start connection
        } catch (ConfigurationException | IllegalArgumentException e) {
            logger.warn("MqttBroker connection configuration faulty: {}", e.getMessage());
        } catch (MqttException e) {
            logger.warn("MqttBroker start failed: {}", e.getMessage(), e);
        }
    }

    @Activate
    public void activate(@Nullable Map<String, Object> config) {
        logger.debug("MQTT Broker connection service started...");
        modified(config);
    }

    @Deactivate
    public void deactivate() {
        if (connection != null) {
            connection.stop();
        }
        connection = null;
    }
}
