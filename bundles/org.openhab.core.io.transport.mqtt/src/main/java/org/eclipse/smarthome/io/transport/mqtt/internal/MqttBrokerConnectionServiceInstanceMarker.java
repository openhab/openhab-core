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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.core.ConfigurableService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * This is a marker service and represents a service factory so multiple configuration instances of type
 * {@link MqttBrokerConnectionServiceInstance} can be created.
 *
 * @author David Graeff - Initial contribution
 */
@Component(immediate = true, service = MqttBrokerConnectionServiceInstanceMarker.class, property = {
        Constants.SERVICE_PID + "=org.eclipse.smarthome.mqttbroker",
        ConfigurableService.SERVICE_PROPERTY_FACTORY_SERVICE + "=true",
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=MQTT system broker connection",
        ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=MQTT",
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=mqtt:systemBrokerConnectionInstance" })
@NonNullByDefault
public class MqttBrokerConnectionServiceInstanceMarker {

}
