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
import org.eclipse.jdt.annotation.Nullable;

/**
 * Contains configuration for a MqttBrokerConnection. Necessary to add a new broker connection the {@link MqttService}.
 *
 * @author David Graeff - Initial contribution and API
 */
@NonNullByDefault
public class MqttBrokerConnectionConfig {
    // Optional connection name
    public @Nullable String name;
    // Connection parameters (host+port+secure)
    public @Nullable String host;
    public @Nullable Integer port;
    public boolean secure = true;
    // Authentication parameters
    public @Nullable String username;
    public @Nullable String password;
    public @Nullable String clientID;
    // MQTT parameters
    public Integer qos = MqttBrokerConnection.DEFAULT_QOS;
    public Boolean retainMessages = false;
    /** Keepalive in seconds */
    public @Nullable Integer keepAlive;
    // Last will parameters
    public @Nullable String lwtTopic;
    public @Nullable String lwtMessage;
    public Integer lwtQos = MqttBrokerConnection.DEFAULT_QOS;
    public Boolean lwtRetain = false;

    /**
     * Return the brokerID of this connection. This is either the name or host:port(:s), for instance "myhost:8080:s".
     * This method will return an empty string, if none of the parameters is set.
     */
    public String getBrokerID() {
        final String name = this.name;
        if (name != null && name.length() > 0) {
            return name;
        } else {
            StringBuffer b = new StringBuffer();
            if (host != null) {
                b.append(host);
            }
            final Integer port = this.port;
            if (port != null) {
                b.append(":");
                b.append(port.toString());
            }
            if (secure) {
                b.append(":s");
            }
            return b.toString();
        }
    }

    /**
     * Output the name, host, port and secure flag
     */
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        if (name != null) {
            b.append(name);
            b.append(", ");
        }
        if (host != null) {
            b.append(host);
        }
        final Integer port = this.port;
        if (port != null) {
            b.append(":");
            b.append(port.toString());
        }
        if (secure) {
            b.append(":s");
        }
        return b.toString();
    }
}
