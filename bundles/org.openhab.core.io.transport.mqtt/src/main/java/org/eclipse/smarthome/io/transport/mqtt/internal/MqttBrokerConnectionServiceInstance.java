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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnection;
import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnectionConfig;
import org.eclipse.smarthome.io.transport.mqtt.MqttException;
import org.eclipse.smarthome.io.transport.mqtt.MqttService;
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
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, service = MqttBrokerConnectionServiceInstance.class, configurationPid = "org.eclipse.smarthome.mqttbroker")
@NonNullByDefault
public class MqttBrokerConnectionServiceInstance {
    private final Logger logger = LoggerFactory.getLogger(MqttBrokerConnectionServiceInstance.class);
    private @Nullable MqttBrokerConnection connection;
    private @Nullable MqttService mqttService;

    @Reference
    public void setMqttService(MqttService service) {
        mqttService = service;
    }

    public void unsetMqttService(MqttService service) {
        mqttService = null;
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

        if (configMap == null || configMap.isEmpty() || mqttService == null) {
            return;
        }
        final @NonNull MqttServiceImpl service = (@NonNull MqttServiceImpl) mqttService;

        // Parse configuration
        MqttBrokerConnectionConfig config = new Configuration(configMap).as(MqttBrokerConnectionConfig.class);

        try {
            // Compute brokerID and make sure it is not empty
            String brokerID = config.getBrokerID();
            if (StringUtils.isBlank(brokerID) || brokerID == null) {
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
