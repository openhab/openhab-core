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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.ConfigurableService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * This is a marker service and represents a service factory so multiple configuration instances of type
 * {@link MqttBrokerConnectionServiceInstance} can be created.
 *
 * @author David Graeff - Initial contribution
 */
@Component(immediate = true, service = MqttBrokerConnectionServiceInstanceMarker.class, //
        property = Constants.SERVICE_PID + "=org.openhab.mqttbroker")
@ConfigurableService(factory = true, category = "MQTT", label = "MQTT system broker connection", description_uri = MqttBrokerConnectionServiceInstanceMarker.CONFIG_URI)
@NonNullByDefault
public class MqttBrokerConnectionServiceInstanceMarker {

    protected static final String CONFIG_URI = "mqtt:systemBrokerConnectionInstance";
}
