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
package org.openhab.core.io.transport.mqtt.sslcontext;

import javax.net.ssl.SSLContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.osgi.service.cm.ConfigurationException;

/**
 * Implement this and provide a {@link SSLContext} instance to be used by the {@link MqttBrokerConnection} for secure
 * Mqtt broker connections where the URL starts with 'ssl://'. Register your implementation with
 * {@link MqttBrokerConnection.setSSLContextProvider}.
 *
 * @author David Graeff - Initial contribution
 */
@Deprecated
@NonNullByDefault
public interface SSLContextProvider {
    /**
     * Return an {@link SSLContext} to be used by secure Mqtt broker connections. Never return null here. If you are not
     * able to create an {@link SSLContext} instance, fail with a ConfigurationException instead.
     */
    SSLContext getContext() throws ConfigurationException;
}
